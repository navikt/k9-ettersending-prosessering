package no.nav.helse.k9

import no.nav.helse.prosessering.v1.ettersending.Søknadstype
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.ettersendelse.Ytelse
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal fun String.assertCleanupEttersendeFormat(søknadstype: Søknadstype) {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val melding = rawJson.getJSONObject("data").getJSONObject("melding")
    assertEquals(søknadstype.name, melding.getString("søknadstype"))

    val journalførtMelding = assertNotNull(rawJson.getJSONObject("data").getJSONObject("journalførtMelding"))
    assertNotNull(journalførtMelding.getString("journalpostId"))

    val søknad = assertNotNull(journalførtMelding.getJSONObject("søknad"))
    val rekonstruertSøknad = Ettersendelse
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), Ettersendelse.SerDes.serialize(rekonstruertSøknad), true)
}
