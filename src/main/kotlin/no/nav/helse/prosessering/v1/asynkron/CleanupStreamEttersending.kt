package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.erEtter
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.felles.AktørId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class CleanupStreamEttersending(
    kafkaConfig: KafkaConfig,
    dokumentService: DokumentService,
    datoMottattEtter: ZonedDateTime
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            dokumentService,
            datoMottattEtter
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "CleanupV1Ettersending"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(dokumentService: DokumentService, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraCleanup: Topic<TopicEntry<CleanupEttersending>> = Topics.CLEANUP_ETTERSENDING

            builder
                .stream<String, TopicEntry<CleanupEttersending>>(
                    fraCleanup.name, Consumed.with(fraCleanup.keySerde, fraCleanup.valueSerde)
                )
                .filter { _, entry -> entry.data.melding.mottatt.erEtter(gittDato) }
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter ettersending dokumenter.")
                        dokumentService.slettDokumeter(
                            urlBolks = entry.data.melding.dokumentUrls,
                            aktørId = AktørId(entry.data.melding.søker.aktørId),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )
                        logger.info("Dokumenter slettet.")
                        entry.data.journalførtMelding
                    }
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
