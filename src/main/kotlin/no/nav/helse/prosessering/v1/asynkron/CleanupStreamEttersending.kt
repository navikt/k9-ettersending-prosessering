package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.erEtter
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class CleanupStreamEttersending(
    kafkaConfig: KafkaConfig,
    k9MellomlagringService: K9MellomlagringService,
    datoMottattEtter: ZonedDateTime
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            k9MellomlagringService,
            datoMottattEtter
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "CleanupV1Ettersending"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(k9MellomlagringService: K9MellomlagringService, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraCleanup = Topics.CLEANUP_ETTERSENDING

            builder
                .stream(fraCleanup.name, fraCleanup.consumed)
                .filter { _, entry -> entry.deserialiserTilCleanup().melding.mottatt.erEtter(gittDato) }
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter ettersending dokumenter.")
                        val cleanupEttersending = entry.deserialiserTilCleanup()
                        k9MellomlagringService.slettDokumeter(
                            urlBolks = cleanupEttersending.melding.dokumentUrls,
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumentEier = DokumentEier(cleanupEttersending.melding.søker.fødselsnummer)
                        )
                        logger.info("Dokumenter slettet.")
                        cleanupEttersending.serialiserTilData()
                    }
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
