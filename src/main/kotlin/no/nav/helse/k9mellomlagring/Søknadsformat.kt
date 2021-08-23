package no.nav.helse.k9mellomlagring

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.ettersendelse.Ettersendelse

class Søknadsformat {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

        internal fun somJsonEttersending(
            ettersending: Ettersendelse
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(ettersending)
            node.remove("vedlegg_urls")
            return objectMapper.writeValueAsBytes(node)
        }
    }
}
