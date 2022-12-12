package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class K9EttersendingProsesseringWithMocks {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9EttersendingProsesseringWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer: WireMockServer = WireMockBuilder()
                .withPort(8091)
                .withAzureSupport()
                .build()
                .stubK9MellomlagringHealth()
                .stubK9JoarkHealth()
                .stubJournalfor()
                .stubLagreDokument()
                .stubSlettDokument()

            val kafkaEnvironment = KafkaWrapper.bootstrap()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                kafkaEnvironment = kafkaEnvironment,
                port = 8092
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    kafkaEnvironment.stop()
                    logger.info("Tear down complete")
                }
            })

            testApplication { no.nav.helse.main(testArgs) }
        }
    }
}
