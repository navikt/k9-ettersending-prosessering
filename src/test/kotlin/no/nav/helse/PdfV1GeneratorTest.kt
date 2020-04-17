package no.nav.helse

import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.felles.Søker
import org.junit.Ignore
import java.io.File
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()
    }

    private fun gyldigEttersending() = EttersendingV1(
        språk = "nb",
        mottatt = ZonedDateTime.now(),
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        søknadId = "Ettersending",
        søker = Søker(
            aktørId = "123456",
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fødselsnummer = "29099012345",
            fødselsdato = fødselsdato
        ),
        beskrivelse = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Sed accumsan erat cursus enim aliquet, ac auctor orci consequat. " +
                    "Etiam nec tellus sapien. Nam gravida massa id sagittis ultrices.",
        søknadstype = "Omsorgspenger",
        vedleggUrls = listOf(URI("http://localhost:8081/vedlegg1"),
                                URI("http://localhost:8081/vedlegg2"),
                                URI("http://localhost:8081/vedlegg3")),
        titler = listOf("vedlegg1", "vedlegg2")

    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-ettersending"
        var pdf = generator.generateSoknadOppsummeringPdfEttersending(
            melding = gyldigEttersending()
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
