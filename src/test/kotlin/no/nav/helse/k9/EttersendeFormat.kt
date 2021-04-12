package no.nav.helse.k9

import no.nav.k9.ettersendelse.Ettersendelse
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertNotNull

internal fun String.assertCleanupEttersendeFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val journalførtMelding = assertNotNull(rawJson.getJSONObject("data").getJSONObject("journalførtMelding"))
    assertNotNull(journalførtMelding.getString("journalpostId"))

    val søknad = assertNotNull(journalførtMelding.getJSONObject("søknad"))

    val rekonstruertSøknad = Ettersendelse
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), Ettersendelse.SerDes.serialize(rekonstruertSøknad), true)
}
