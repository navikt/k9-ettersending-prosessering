package no.nav.helse.prosessering.v1.ettersending

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import java.net.URI
import java.time.ZonedDateTime

data class EttersendingV1(
    val søker : Søker,
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String? = "nb",
    val vedleggUrls: List<URI>,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String,
    val søknadstype: Søknadstype,
    val titler: List<String>,
    val k9Format: Ettersendelse
)

enum class Søknadstype(val pdfNavn: String){
    @JsonAlias("pleiepenger") //TODO 23.03.2021 - Alias for å støtte gammel versjon fra frontend
    PLEIEPENGER_SYKT_BARN("pleiepenger sykt barn"),
    @JsonAlias("omsorgspenger") //TODO 23.03.2021 - Alias for å støtte gammel versjon fra frontend
    OMP_UTV_KS("omsorgspenger utvidet rett"), // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    OMP_UT_SNF("omsorgspenger utbetaling snf"), // Omsorgspenger utbetaling SNF ytelse.
    OMP_UT_ARBEIDSTAKER("omsorgspenger utbetaling arbeidstaker"), // Omsorgspenger utbetaling arbeidstaker ytelse.
    OMP_UTV_MA("omsorgspenger midlertidig alene") // Omsorgspenger utvidet rett - midlertidig alene
}