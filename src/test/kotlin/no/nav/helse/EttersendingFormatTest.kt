package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class EttersendingFormatTest{

    @Test
    fun `Ettersending journalføres som JSON`(){
        val søknadId = UUID.randomUUID().toString()
        val mottatt = ZonedDateTime.of(2018, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"))
        val json = Søknadsformat.somJsonEttersending(EttersendingUtils.k9Format(søknadId = søknadId, mottatt = mottatt))
        println(String(json))
        JSONAssert.assertEquals(
            """
            {
              "søknadId": "$søknadId",
              "versjon": "0.0.1",
              "mottattDato": "2018-01-02T03:04:05.000Z",
              "søker": {
                "norskIdentitetsnummer": "29099012345"
              },
              "ytelse": "OMP_UTV_KS"
            }
            """.trimIndent(), String(json), true
        )
    }
}
