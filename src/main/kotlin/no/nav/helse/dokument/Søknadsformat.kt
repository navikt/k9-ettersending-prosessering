package no.nav.helse.dokument

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.k9.ettersendelse.Ettersendelse

class Søknadsformat {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured()
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

        internal fun somJsonEttersending(
            k9Format: Ettersendelse
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(k9Format)
            node.remove("vedlegg_urls")
            return objectMapper.writeValueAsBytes(node)
        }
    }
}
