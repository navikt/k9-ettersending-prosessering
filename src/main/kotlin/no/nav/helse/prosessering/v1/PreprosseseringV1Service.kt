package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.v1.felles.Metadata
import no.nav.helse.prosessering.v1.felles.SoknadId
import no.nav.helse.prosessering.v1.ettersending.PreprosessertEttersendingV1
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import no.nav.helse.prosessering.v1.ettersending.reportMetrics
import no.nav.helse.prosessering.v1.felles.AktørId
import no.nav.k9.ettersendelse.Ettersendelse
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseserEttersending(
        melding: EttersendingV1,
        metadata: Metadata,
        søknadstype: Søknadstype
    ): PreprosessertEttersendingV1 {
        val søknadId = SoknadId(melding.søknadId)
        logger.info("Preprosseserer ettersending med søknadId: $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val søkerAktørId = AktørId(melding.søker.aktørId)

        logger.info("Genererer Oppsummerings-PDF av ettersending.")
        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdfEttersending(melding)
        logger.info("Generering av Oppsummerings-PDF OK.")

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktørId = søkerAktørId,
            dokumentbeskrivelse = "Ettersendelse ${søknadstype.pdfNavn}"
        )
        logger.info("Mellomlagring av Oppsummerings-PDF OK")

        logger.info("Mellomlagrer Oppsummerings-JSON")

        val soknadJsonUrl = if(melding.k9Format != null) {
            logger.info("K9Format som lagres: ${Ettersendelse.SerDes.serialize(melding.k9Format)}") //TODO 24.03.2021 - Fjernes før prodsetting

            dokumentService.lagreSoknadsMeldingEttersending(
                k9Format = melding.k9Format,
                aktørId = søkerAktørId,
                correlationId = correlationId,
                søknadstype = melding.søknadstype.pdfNavn
            )
        } else {
            dokumentService.lagreSoknadsMeldingEttersending(
                ettersending = melding,
                aktørId = søkerAktørId,
                correlationId = correlationId,
                søknadstype = melding.søknadstype.pdfNavn
            )
        }

        logger.info("Mellomlagrer Oppsummerings-JSON OK.")

        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it)) }
        }

        logger.info("Totalt ${komplettDokumentUrls.size} dokumentbolker.")


        val preprossesertMeldingV1 = PreprosessertEttersendingV1(
            melding = melding,
            dokumentUrls = komplettDokumentUrls.toList(),
            sokerAktoerId = søkerAktørId
        )
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }
}
