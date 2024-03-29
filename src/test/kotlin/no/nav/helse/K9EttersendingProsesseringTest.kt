package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.k9.assertCleanupEttersendeFormat
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import org.junit.jupiter.api.AfterAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals


class K9EttersendingProsesseringTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(K9EttersendingProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9MellomlagringHealth()
            .stubK9JoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestProducerEttersending = kafkaEnvironment.meldingEttersendingProducer()
        private val cleanupKonsumerEttersending = kafkaEnvironment.cleanupKonsumerEttersending()

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaContainer: KafkaContainer): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaContainer
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaContainer: KafkaContainer) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaContainer)
        })

        private fun stopEngine() = engine.stop(5, 60, TimeUnit.SECONDS)

        internal fun restartEngine() {
            stopEngine()
            CollectorRegistry.defaultRegistry.clear()
            engine = newEngine(kafkaEnvironment)
            engine.start(wait = true)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            stopEngine()
            kafkaEnvironment.stop()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun`Gyldig ettersending for omsorgspenger ekstra dager blir prosessert av journalføringkonsumer`(){
        val søknadstype = Søknadstype.OMP_UTV_KS
        val søknad = EttersendingUtils.defaultEttersending().copy(
            søknadstype = søknadstype
        )

        kafkaTestProducerEttersending.leggTilMottak(søknad)
        cleanupKonsumerEttersending
            .hentCleanupMeldingEttersending(søknad.søknadId)
            .assertCleanupEttersendeFormat(søknadstype)
    }

    @Test
    fun`Gyldig ettersending for pleiepenger sykt barn blir prosessert av journalføringkonsumer`(){
        val søknadstype = Søknadstype.PLEIEPENGER_SYKT_BARN
        val søknad = EttersendingUtils.defaultEttersending().copy(
            søknadstype = søknadstype
        )

        kafkaTestProducerEttersending.leggTilMottak(søknad)
        cleanupKonsumerEttersending
            .hentCleanupMeldingEttersending(søknad.søknadId)
            .assertCleanupEttersendeFormat(søknadstype)
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val søknadstype = Søknadstype.PLEIEPENGER_SYKT_BARN
        val søknad = EttersendingUtils.defaultEttersending().copy(
            søknadstype = søknadstype
        )

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaTestProducerEttersending.leggTilMottak(søknad)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        cleanupKonsumerEttersending
            .hentCleanupMeldingEttersending(søknad.søknadId)
            .assertCleanupEttersendeFormat(søknadstype)
    }

    private fun readyGir200HealthGir503() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/health") {}.apply {
                    assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                }
            }
        }
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
