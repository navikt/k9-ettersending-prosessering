package no.nav.helse.prosessering.v1.ettersending

import no.nav.helse.prosessering.v1.felles.Søker
import no.nav.k9.ettersendelse.Ettersendelse
import java.time.ZonedDateTime

data class PreprosessertEttersendingV1(
    val sprak: String?,
    val soknadId: String,
    val vedleggId: List<List<String>>,
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
        vedleggId: List<List<String>>
    ) : this(
        sprak = melding.språk,
        soknadId = melding.søknadId,
        vedleggId = vedleggId,
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

