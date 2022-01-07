package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.ettersendelse.Ytelse
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

internal object EttersendingUtils {
    internal val objectMapper = jacksonObjectMapper().k9EttersendingKonfigurert()
    private val start = LocalDate.parse("2020-01-01")

    internal fun defaultEttersending(søknadId: String = UUID.randomUUID().toString()) = EttersendingV1(
        språk = "nb",
        mottatt = ZonedDateTime.now(),
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        søknadId = søknadId,
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
        vedleggId = listOf("vedlegg1", "vedlegg2", "vedlegg3"),
        vedleggUrls = null,
        titler = listOf("Vedlegg 1", "Vedlegg 2", "Vedlegg 3"),
        k9Format = Ettersendelse.builder()
            .søknadId(SøknadId(søknadId))
            .søker(no.nav.k9.søknad.felles.personopplysninger.Søker(NorskIdentitetsnummer.of("29099012345")))
            .mottattDato(ZonedDateTime.now())
            .ytelse(Ytelse.OMP_UTV_KS)
            .build()
    )
}

internal fun EttersendingV1.somJson() = EttersendingUtils.objectMapper.writeValueAsString(this)