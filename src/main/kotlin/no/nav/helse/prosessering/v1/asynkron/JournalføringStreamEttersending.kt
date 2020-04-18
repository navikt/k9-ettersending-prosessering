package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.joark.Navn
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.ettersending.PreprosessertEttersendingV1
import no.nav.helse.prosessering.v1.ettersending.SøknadsType
import no.nav.helse.prosessering.v1.felles.AktørId
import no.nav.helse.tilK9Søker
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.ettersendelse.Ytelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class JournalføringStreamEttersending(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            joarkGateway
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "JournalforingV1Ettersending"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesertV1: Topic<TopicEntry<PreprosessertEttersendingV1>> = Topics.PREPROSSESERT_ETTERSENDING
            val tilCleanup: Topic<TopicEntry<CleanupEttersending>> = Topics.CLEANUP_ETTERSENDING

            val mapValues = builder
                .stream<String, TopicEntry<PreprosessertEttersendingV1>>(
                    fraPreprossesertV1.name,
                    Consumed.with(fraPreprossesertV1.keySerde, fraPreprossesertV1.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {

                        val dokumenter = entry.data.dokumentUrls
                        logger.info("Journalfører dokumenter: {}", dokumenter)
                        val journaPostId = joarkGateway.journalførEttersending(
                            mottatt = entry.data.mottatt,
                            aktørId = AktørId(entry.data.søker.aktørId),
                            norskIdent = entry.data.søker.fødselsnummer,
                            søkerNavn = Navn(
                                fornavn = entry.data.søker.fornavn,
                                mellomnavn = entry.data.søker.mellomnavn,
                                etternavn = entry.data.søker.etternavn
                            ),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = dokumenter,
                            søknadstype = entry.data.søknadstype
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")
                        val journalfort = JournalfortEttersending(
                            journalpostId = journaPostId.journalpostId,
                            søknad = entry.data.tilK9Ettersendelse()
                        )
                        CleanupEttersending(
                            metadata = entry.metadata,
                            melding = entry.data,
                            journalførtMelding = journalfort
                        )
                    }
                }
            mapValues
                .to(tilCleanup.name, Produced.with(tilCleanup.keySerde, tilCleanup.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}

private fun PreprosessertEttersendingV1.tilK9Ettersendelse(): Ettersendelse = Ettersendelse.builder()
    .mottattDato(mottatt)
    .søker(søker.tilK9Søker())
    .ytelse(tilK9Ytelse())
    .build()

private fun PreprosessertEttersendingV1.tilK9Ytelse() = when(søknadstype) {
    SøknadsType.PLEIEPENGER -> Ytelse.PLEIEPENGER_SYKT_BARN
    SøknadsType.OMSORGSPENGER -> Ytelse.OMSORGSPENGER
}
