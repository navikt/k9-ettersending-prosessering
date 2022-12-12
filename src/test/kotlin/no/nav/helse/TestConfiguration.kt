package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import org.json.JSONObject
import org.testcontainers.containers.KafkaContainer

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaContainer? = null,
        port : Int = 8080,
        tpsProxyBaseUrl : String? = wireMockServer?.getTpsProxyBaseUrl(),
        k9JoarkBaseUrl : String? = wireMockServer?.getk9JoarkBaseUrl(),
        k9DokumentBaseUrl : String? = wireMockServer?.getK9MellomlagringBaseUrl()
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.tps_proxy_v1_base_url","$tpsProxyBaseUrl"),
            Pair("nav.K9_JOARK_BASE_URL","$k9JoarkBaseUrl"),
            Pair("nav.k9_mellomlagring_base_url","$k9DokumentBaseUrl")
        )

        // Clients
        if (wireMockServer != null) {
            map["nav.auth.clients.1.alias"] = "azure-v2"
            map["nav.auth.clients.1.client_id"] = "k9-ettersending-prosessering"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.scopes.k9_mellomlagring"] = "k9-mellomlagring/.default"
            map["nav.auth.scopes.journalfore"] = "k9-joark/.default"
        }

        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.bootstrapServers
            map["nav.kafka.auto_offset_reset"] = "earliest"
        }

        return map.toMap()
    }
    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
