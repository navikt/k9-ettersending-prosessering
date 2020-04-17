package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentService
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV1Service: PreprosseseringV1Service,
    joarkGateway: JoarkGateway,
    dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val preprosseseringStreamEttersending =
        PreprosseseringStreamEttersending(
            kafkaConfig = kafkaConfig,
            preprosseseringV1Service = preprosseseringV1Service
        )

    private val journalforingsStreamEttersending =
        JournalføringStreamEttersending(
            kafkaConfig = kafkaConfig,
            joarkGateway = joarkGateway
        )

    private val cleanupStreamEttersending =
        CleanupStreamEttersending(
            kafkaConfig = kafkaConfig,
            dokumentService = dokumentService
        )

    private val healthChecks = setOf(

        preprosseseringStreamEttersending.healthy,
        journalforingsStreamEttersending.healthy,
        cleanupStreamEttersending.healthy
    )

    private val isReadyChecks = setOf(
        preprosseseringStreamEttersending.ready,
        journalforingsStreamEttersending.ready,
        cleanupStreamEttersending.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        preprosseseringStreamEttersending.stop()
        journalforingsStreamEttersending.stop()
        cleanupStreamEttersending.stop()
        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}
