package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.ettersendelse.Ytelse
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker

internal object EttersendingUtils {
    internal val objectMapper = jacksonObjectMapper().k9EttersendingKonfigurert()
    private val start = LocalDate.parse("2020-01-01")

    internal val defaultEttersending = EttersendingV1(
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
            fødselsdato = LocalDate.now().minusYears(20)
        ),
        beskrivelse = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed accumsan erat cursus enim aliquet, ac auctor orci consequat. " +
                "Etiam nec tellus sapien. Nam gravida massa id sagittis ultrices.",
        søknadstype = Søknadstype.OMP_UTV_KS,
        vedleggUrls = listOf(
            URI("http://localhost:8081/vedlegg1"),
            URI("http://localhost:8081/vedlegg2"),
            URI("http://localhost:8081/vedlegg3")
        ),
        titler = listOf("Vedlegg 1", "Vedlegg 2", "Vedlegg 3"),
        k9Format = k9Format()
    )

    fun k9Format(søknadId: String = UUID.randomUUID().toString(), mottatt: ZonedDateTime? = ZonedDateTime.now()): Ettersendelse = Ettersendelse
        .builder()
        .mottattDato(mottatt)
        .søker(K9Søker(NorskIdentitetsnummer.of("29099012345")))
        .ytelse(Ytelse.OMP_UTV_KS)
        .søknadId(SøknadId(søknadId))
        .build()
}

internal fun EttersendingV1.somJson() = EttersendingUtils.objectMapper.writeValueAsString(this)
