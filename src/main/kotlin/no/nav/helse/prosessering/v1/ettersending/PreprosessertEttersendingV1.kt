package no.nav.helse.prosessering.v1.ettersending

import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import java.net.URI
import java.time.ZonedDateTime

data class PreprosessertEttersendingV1(
    val sprak: String?,
    val soknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String?,
    val søknadstype: Søknadstype,
    val titler: List<String>,
    val k9Format: Ettersendelse
) {
    internal constructor(
        melding: EttersendingV1,
        dokumentUrls: List<List<URI>>
    ) : this(
        sprak = melding.språk,
        soknadId = melding.søknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        søker = melding.søker,
        beskrivelse = melding.beskrivelse,
        søknadstype = melding.søknadstype,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        titler = melding.titler,
        k9Format = melding.k9Format
    )
}

