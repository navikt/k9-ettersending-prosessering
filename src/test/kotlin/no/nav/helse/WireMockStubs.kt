package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import io.ktor.http.HttpHeaders
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import java.util.*

private const val aktoerRegisterBasePath = "/aktoerregister-mock"
private const val tpsProxyBasePath = "/tps-proxy-mock"
private const val k9JoarkBaseUrl = "/k9-joark-mock"
private const val k9DokumentBasePath = "/k9-dokument-mock"

internal fun WireMockServer.stubAktørRegister(
    identNummer: String,
    aktoerId: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*"))
            .withQueryParam("gjeldende", EqualToPattern("true"))
            .withQueryParam("identgruppe", EqualToPattern("AktoerId"))
            .withHeader("Nav-Personidenter", EqualToPattern(identNummer))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                    {
                      "$identNummer": {
                        "identer": [
                          {
                            "ident": "$aktoerId",
                            "identgruppe": "AktoerId",
                            "gjeldende": true
                          }
                        ],
                        "feilmelding": null
                      }
                    }
                    """.trimIndent()
                    )
                    .withStatus(200)
            )
    )

    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*"))
            .withQueryParam("gjeldende", EqualToPattern("true"))
            .withQueryParam("identgruppe", EqualToPattern("NorskIdent"))
            .withHeader("Nav-Personidenter", EqualToPattern(aktoerId))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                        {
                          "$aktoerId": {
                            "identer": [
                              {
                                "ident": "$identNummer",
                                "identgruppe": "NorskIdent",
                                "gjeldende": true
                              }
                            ],
                            "feilmelding": null
                          }
                        }
                        """.trimIndent()
                    )
            )
    )
    return this
}



internal fun WireMockServer.stubLagreDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9DokumentBasePath.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "${getK9DokumentBaseUrl()}/v1/dokument/${UUID.randomUUID()}")
                .withStatus(201)
        )
    )
    return this
}

internal fun WireMockServer.stubSlettDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.delete(WireMock.urlPathMatching(".*$k9DokumentBasePath.*")).willReturn(
            WireMock.aResponse()
                .withStatus(204)
        )
    )
    return this
}

internal fun WireMockServer.stubJournalfor(responseCode: Int = 201): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9JoarkBaseUrl.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                    {
                        "journal_post_id" : "9101112"
                    }
                    """.trimIndent()
                )
                .withStatus(responseCode)
        )
    )
    return this
}

private fun WireMockServer.stubHealthEndpoint(
    path: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
    return this
}

internal fun WireMockServer.stubK9DokumentHealth() = stubHealthEndpoint("$k9DokumentBasePath/health")
internal fun WireMockServer.stubK9JoarkHealth() = stubHealthEndpoint("$k9JoarkBaseUrl/health")

internal fun WireMockServer.getAktoerRegisterBaseUrl() = baseUrl() + aktoerRegisterBasePath
internal fun WireMockServer.getTpsProxyBaseUrl() = baseUrl() + tpsProxyBasePath
internal fun WireMockServer.getk9JoarkBaseUrl() = baseUrl() + k9JoarkBaseUrl
internal fun WireMockServer.getK9DokumentBaseUrl() = baseUrl() + k9DokumentBasePath
