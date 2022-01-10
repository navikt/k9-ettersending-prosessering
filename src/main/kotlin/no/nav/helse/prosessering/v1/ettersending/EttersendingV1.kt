package no.nav.helse.prosessering.v1.ettersending

import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import java.time.ZonedDateTime

data class EttersendingV1(
    val søker : Søker,
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String? = "nb",
    val vedleggId: List<String>,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String?,
    val søknadstype: Søknadstype,
    val titler: List<String>,
    val k9Format: Ettersendelse
)

enum class Søknadstype(val pdfNavn: String){
    PLEIEPENGER_SYKT_BARN("Pleiepenger sykt barn"),
    OMP_UTV_KS("Ekstra omsorgsdager"), // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    OMP_UT_SNF("Omsorgspenger utbetaling selvstendig/frilanser"), // Omsorgspenger utbetaling SNF ytelse.
    OMP_UT_ARBEIDSTAKER("Omsorgspenger utbetaling arbeidstaker"), // Omsorgspenger utbetaling arbeidstaker ytelse.
    OMP_UTV_MA("Omsorgspenger regnet som alene"), // Omsorgspenger utvidet rett - midlertidig alene
    OMP_DELE_DAGER("Melding om deling av omsorgsdager")
}