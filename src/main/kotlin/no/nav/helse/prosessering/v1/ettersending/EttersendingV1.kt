package no.nav.helse.prosessering.v1.ettersending

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.prosessering.v1.felles.Søker
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
    val søknadstype: SøknadsType,
    val titler: List<String>
)
 enum class SøknadsType(@JsonValue val type: String) {
     PLEIEPENGER("pleiepenger"),
     OMSORGSPENGER("omsorgspenger")
 }
