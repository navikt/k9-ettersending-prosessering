package no.nav.helse

import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import java.io.File
import no.nav.helse.prosessering.v1.felles.Metadata
import java.util.*
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val metadata = Metadata(
            soknadDialogCommitSha = "abc-123",
            version = 1,
            correlationId = UUID.randomUUID().toString()
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-ettersending-omsorgspenger"
        var pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.OMP_UTV_KS,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-full-ettersending-pleiepenger"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.PLEIEPENGER_SYKT_BARN
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-full-ettersending-omsorgspenger-utbetaling-snf"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.OMP_UT_SNF,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-full-ettersending-omsorgspenger-utbetaling-arbeidstaker"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.OMP_UT_ARBEIDSTAKER,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-full-ettersending-omsorgspenger-midlertidig-alene"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.OMP_UTV_MA,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-full-ettersending-melding-dele-dager"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.OMP_DELE_DAGER,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-full-ettersending-pp-livets-sluttfase"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending().copy(
                søknadstype = Søknadstype.PLEIEPENGER_LIVETS_SLUTTFASE,
                beskrivelse = null
            ),
            metadata = metadata
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    //@Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
