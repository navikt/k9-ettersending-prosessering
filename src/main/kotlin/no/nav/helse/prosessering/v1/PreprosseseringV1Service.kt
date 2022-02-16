package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.Dokument
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.k9mellomlagring.Søknadsformat
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.ettersending.PreprosessertEttersendingV1
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import no.nav.helse.prosessering.v1.ettersending.reportMetrics
import no.nav.helse.prosessering.v1.felles.Metadata
import no.nav.helse.prosessering.v1.felles.SoknadId
import org.slf4j.LoggerFactory
import java.net.URI

internal class PreprosseseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val k9MellomlagringService: K9MellomlagringService
) {
     private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)

    internal suspend fun preprosesserEttersending(
        ettersending: EttersendingV1,
        metadata: Metadata
    ): PreprosessertEttersendingV1 {
        val søknadId = SoknadId(ettersending.søknadId)
        logger.info("Preprosesserer ettersending med søknadId: $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentEier(ettersending.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av ettersending.")
        val oppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdfEttersending(ettersending)

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val oppsummeringPdfVedleggId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = oppsummeringPdf,
                contentType = "application/pdf",
                title = ettersending.søknadstype.somDokumentbeskrivelse()
            ),
            correlationId = correlationId,
        ).vedleggId()

        logger.info("Mellomlagrer Oppsummerings-JSON")
        val ettersendingJsonVedleggId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = Søknadsformat.somJsonEttersending(ettersending.k9Format),
                contentType = "application/json",
                title = "Ettersendelse ${ettersending.søknadstype} som JSON"
            ),
            correlationId = correlationId
        ).vedleggId()

        val komplettVedleggId = mutableListOf(
            listOf(
                oppsummeringPdfVedleggId,
                ettersendingJsonVedleggId
            )
        )

        logger.info("Legger til ${ettersending.vedleggId.size} vedlegg id's fra meldingen som dokument.")
        ettersending.vedleggId.forEach { komplettVedleggId.add(listOf(it)) }

        logger.info("Totalt ${komplettVedleggId.size} dokumentbolker med totalt ${komplettVedleggId.flatten().size} dokumenter.")

        val preprossesertMeldingV1 = PreprosessertEttersendingV1(
            melding = ettersending,
            vedleggId = komplettVedleggId
        )
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }
}

fun URI.vedleggId(): String = this.toString().substringAfterLast("/")

private fun Søknadstype.somDokumentbeskrivelse(): String {
    return when (this) {
        Søknadstype.PLEIEPENGER_SYKT_BARN -> "Ettersendelse pleiepenger sykt barn"
        Søknadstype.OMP_UTV_KS -> "Ettersendelse ekstra omsorgsdager"
        Søknadstype.OMP_UT_SNF -> "Ettersendelse omsorgspenger utbetaling selvstendig/frilanser"
        Søknadstype.OMP_UT_ARBEIDSTAKER -> "Ettersendelse omsorgspenger utbetaling arbeidstaker"
        Søknadstype.OMP_UTV_MA -> "Ettersendelse omsorgspenger regnet som alene"
        Søknadstype.OMP_DELE_DAGER -> "Ettersendelse melding om deling av omsorgsdager"
        Søknadstype.PLEIEPENGER_LIVETS_SLUTTFASE -> "Ettersendelse pleiepenger i livets sluttfase"
    }
}