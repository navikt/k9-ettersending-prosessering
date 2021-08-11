package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.erEtter
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.joark.Navn
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.felles.AktørId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class JournalføringStreamEttersending(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig,
    datoMottattEtter: ZonedDateTime
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            joarkGateway,
            datoMottattEtter
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "JournalforingV1Ettersending"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")
        private val erJournalført = mutableListOf<String>()

        private fun topology(joarkGateway: JoarkGateway, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesertV1 = Topics.PREPROSESSERT_ETTERSENDING
            val tilCleanup = Topics.CLEANUP_ETTERSENDING

            val mapValues = builder
                .stream(fraPreprossesertV1.name, fraPreprossesertV1.consumed)
                .filter { _, entry -> entry.deserialiserTilPreprosessertMelding().mottatt.erEtter(gittDato) }
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        val preprosessertEttersending = entry.deserialiserTilPreprosessertMelding()
                        val dokumenter = preprosessertEttersending.dokumentUrls
                        logger.info("Journalfører dokumenter: {}", dokumenter)

                        val journaPostId = joarkGateway.journalførEttersending(
                            mottatt = preprosessertEttersending.mottatt,
                            aktørId = AktørId(preprosessertEttersending.søker.aktørId),
                            norskIdent = preprosessertEttersending.søker.fødselsnummer,
                            søkerNavn = Navn(
                                fornavn = preprosessertEttersending.søker.fornavn,
                                mellomnavn = preprosessertEttersending.søker.mellomnavn,
                                etternavn = preprosessertEttersending.søker.etternavn
                            ),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = dokumenter,
                            søknadstype = preprosessertEttersending.søknadstype
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")
                        val journalfort = JournalfortEttersending(
                            journalpostId = journaPostId.journalpostId,
                            søknad = preprosessertEttersending.k9Format
                        )

                        erJournalført.add(entry.metadata.correlationId)

                        CleanupEttersending(
                            metadata = entry.metadata,
                            melding = preprosessertEttersending,
                            journalførtMelding = journalfort
                        ).serialiserTilData()
                    }
                }
            mapValues
                .to(tilCleanup.name, tilCleanup.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}