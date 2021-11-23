package no.nav.helse.prosessering.v1.felles

import no.nav.helse.prosessering.v1.asynkron.CleanupEttersending
import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import java.util.*

data class K9Beskjed(
    val metadata: Metadata,
    val grupperingsId: String,
    val tekst: String,
    val link: String?,
    val dagerSynlig: Long,
    val søkerFødselsnummer: String,
    val eventId: String,
    val ytelse: Ytelse
)

const val DAGER_SYNLIG_K9BESKJED: Long = 7

enum class Ytelse(val tekst: String) {
    ETTERSENDING_PLEIEPENGER_SYKT_BARN("Vi har mottatt din ettersendelse til pleiepenger."),
    ETTERSENDING_OMP_UTV_KS("Vi har mottatt din ettersendelse til omsorgspenger."), // Ettersending - Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    ETTERSENDING_OMP_UT_SNF("Vi har mottatt din ettersendelse til omsorgspenger."), // Ettersending - Omsorgspenger utbetaling SNF ytelse.
    ETTERSENDING_OMP_UT_ARBEIDSTAKER("Vi har mottatt din ettersendelse til omsorgspenger."), // Ettersending - Omsorgspenger utbetaling arbeidstaker ytelse.
    ETTERSENDING_OMP_UTV_MA("Vi har mottatt din ettersendelse til omsorgspenger."), // Ettersending - Omsorgspenger utvidet rett - midlertidig alene
    ETTERSENDING_OMP_DELE_DAGER("Vi har mottatt din ettersendelse til omsorgspenger.") // Ettersending - Melding om deling av omsorgsdager
}

fun Søknadstype.somK9BeskjedYtelse(): Ytelse =
    when (this) {
        Søknadstype.PLEIEPENGER_SYKT_BARN -> Ytelse.ETTERSENDING_PLEIEPENGER_SYKT_BARN
        Søknadstype.OMP_UTV_KS -> Ytelse.ETTERSENDING_OMP_UTV_KS
        Søknadstype.OMP_UT_SNF -> Ytelse.ETTERSENDING_OMP_UT_SNF
        Søknadstype.OMP_UT_ARBEIDSTAKER -> Ytelse.ETTERSENDING_OMP_UT_ARBEIDSTAKER
        Søknadstype.OMP_UTV_MA -> Ytelse.ETTERSENDING_OMP_UTV_MA
        Søknadstype.OMP_DELE_DAGER -> Ytelse.ETTERSENDING_OMP_DELE_DAGER
    }
