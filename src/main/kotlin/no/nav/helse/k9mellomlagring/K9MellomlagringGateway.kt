package no.nav.helse.k9mellomlagring

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Duration

class K9MellomlagringGateway(
    private val accessTokenClient: AccessTokenClient,
    private val k9MellomlagringScopes: Set<String>,
    baseUrl: URI
) : HealthCheck {

    private companion object {
        private const val LAGRE_DOKUMENT_OPERATION = "lagre-dokument"
        private const val SLETTE_DOKUMENT_OPERATION = "slette-dokument"
        private val logger: Logger = LoggerFactory.getLogger(K9MellomlagringGateway::class.java)
    }

    private val completeUrl = Url.buildURL(baseUrl, listOf("v1", "dokument"))
    private val objectMapper = configuredObjectMapper()
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private fun configuredObjectMapper(): ObjectMapper {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return objectMapper
    }

    override suspend fun check(): Result {
        val checkGetLagreDokumentAccessToken = checkGetAccessToken(LAGRE_DOKUMENT_OPERATION, k9MellomlagringScopes)
        val checkGetSletteDokumentAccessToken = checkGetAccessToken(SLETTE_DOKUMENT_OPERATION, k9MellomlagringScopes)
        val combined = checkGetLagreDokumentAccessToken.result().toMutableMap()
        combined.putAll(checkGetSletteDokumentAccessToken.result())
        combined["name"] = "K9MellomlagringGateway"
        return if (checkGetLagreDokumentAccessToken is UnHealthy || checkGetSletteDokumentAccessToken is UnHealthy) UnHealthy(combined)
        else Healthy(combined)
    }

    private fun checkGetAccessToken(operation: String, scopes: Set<String>) : Result {
        return try {
            accessTokenClient.getAccessToken(scopes)
            Healthy(mapOf(operation to "Henting av access token for K9-mellomlagring OK"))
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token for K9-mellomlagring", cause)
            UnHealthy(mapOf(operation to "Henting av access token feilet"))
        }
    }

    internal suspend fun lagreDokmenter(
        dokumenter: Set<Dokument>,
        correlationId: CorrelationId
    ) : List<URI> {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(k9MellomlagringScopes).asAuthoriationHeader()

        return coroutineScope {
            val deferred = mutableListOf<Deferred<URI>>()
            dokumenter.forEach { dokument: Dokument ->
                deferred.add(async {
                    requestLagreDokument(
                        dokument = dokument,
                        correlationId = correlationId,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    private suspend fun requestLagreDokument(
        dokument: Dokument,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) : URI {

        val body = objectMapper.writeValueAsBytes(dokument)
        val contentStream = { ByteArrayInputStream(body) }

        return Retry.retry(
            operation = LAGRE_DOKUMENT_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0
        ) {
            val (request, response, result) = Operation.monitored(
                app = "k9-ettersending-prosessering",
                operation = LAGRE_DOKUMENT_OPERATION,
                resultResolver = { 201 == it.second.statusCode }
            ) {
                completeUrl.toString()
                    .httpPost()
                    .body(contentStream)
                    .header(
                        HttpHeaders.Authorization to authorizationHeader,
                        HttpHeaders.XCorrelationId to correlationId.value,
                        HttpHeaders.ContentType to "application/json"
                    )
                    .awaitStringResponseResult()
            }
            result.fold(
                { URI(response.header(HttpHeaders.Location).first()) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw HttpError(response.statusCode, "Feil ved lagring av dokument.")
                }
            )
        }
    }

    internal suspend fun slettDokmenter(
        vedleggId: List<String>,
        dokumentEier: DokumentEier,
        correlationId: CorrelationId
    ) {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(k9MellomlagringScopes).asAuthoriationHeader()
        coroutineScope {
            val deferred = mutableListOf<Deferred<Unit>>()
            vedleggId.forEach {vedleggId ->
                deferred.add(async {
                    val url = Url.buildURL(
                        baseUrl = completeUrl,
                        pathParts = listOf(vedleggId)
                    )

                    requestSlettDokument(
                        url = url,
                        correlationId = correlationId,
                        dokumentEier = dokumentEier,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    private suspend fun requestSlettDokument(
        url: URI,
        dokumentEier: DokumentEier,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) {
        val body = objectMapper.writeValueAsBytes(dokumentEier)
        val contentStream = { ByteArrayInputStream(body) }

        val httpRequest = url.toString()
            .httpDelete()
            .body(contentStream)
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.XCorrelationId to correlationId.value,
                HttpHeaders.ContentType to "application/json"
            )

        val (request, _, result) = Operation.monitored(
            app = "k9-ettersending-prosessering",
            operation = SLETTE_DOKUMENT_OPERATION,
            resultResolver = { 204 == it.second.statusCode }
        ) {
            httpRequest.awaitStringResponseResult()
        }


        result.fold(
            {},
            { error ->
                logger.warn("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.warn("Feil ved sletting av dokument. $error")
            }
        )
    }
}