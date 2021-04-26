package no.nav.helse

import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import java.io.File
import java.time.LocalDate
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-ettersending-omsorgspenger"
        var pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.OMP_UTV_KS,
                beskrivelse = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-full-ettersending-pleiepenger"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.PLEIEPENGER_SYKT_BARN
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-full-ettersending-omsorgspenger-utbetaling-snf"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.OMP_UT_SNF,
                beskrivelse = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-full-ettersending-omsorgspenger-utbetaling-arbeidstaker"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.OMP_UT_ARBEIDSTAKER,
                beskrivelse = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-full-ettersending-omsorgspenger-midlertidig-alene"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.OMP_UTV_MA,
                beskrivelse = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-full-ettersending-melding-dele-dager"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = EttersendingUtils.defaultEttersending.copy(
                søknadstype = Søknadstype.OMP_DELE_DAGER,
                beskrivelse = null
            )
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
