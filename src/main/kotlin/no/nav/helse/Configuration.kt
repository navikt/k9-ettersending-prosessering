package no.nav.helse

import io.ktor.config.*
import no.nav.helse.dusseldorf.ktor.core.getOptionalString
import no.nav.helse.dusseldorf.ktor.core.getRequiredList
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import no.nav.helse.kafka.KafkaConfig
import java.net.URI
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class Configuration(private val config: ApplicationConfig) {

    fun getk9JoarkBaseUrl() = URI(config.getRequiredString("nav.K9_JOARK_BASE_URL", secret = false))
    fun getK9MellomlagringBaseUrl() = URI(config.getRequiredString("nav.k9_mellomlagring_base_url", secret = false))


    private fun unreadyAfterStreamStoppedIn() = Duration.of(
        config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.amount", secret = false).toLong(),
        ChronoUnit.valueOf(config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.unit", secret = false))
    )

    internal fun soknadDatoMottattEtter() = ZonedDateTime.parse(config.getRequiredString("nav.prosesser_soknader_mottatt_etter", secret = false))
    internal fun journalførMeldingDatoMottattEtter() = ZonedDateTime.parse(config.getRequiredString("nav.journalfor_meldinger_mottatt_etter", secret = false))
    internal fun cleanupMeldingDatoMottattEtter() = ZonedDateTime.parse(config.getRequiredString("nav.cleanup_meldinger_mottatt_etter", secret = false))

    internal fun getKafkaConfig() =
        config.getRequiredString("nav.kafka.bootstrap_servers", secret = false).let { bootstrapServers ->
            val trustStore = config.getOptionalString("nav.kafka.truststore_path", secret = false)?.let { trustStorePath ->
                config.getOptionalString("nav.kafka.credstore_password", secret = true)?.let { credstorePassword ->
                    Pair(trustStorePath, credstorePassword)
                }
            }

            val keyStore = config.getOptionalString("nav.kafka.keystore_path", secret = false)?.let { keystorePath ->
                config.getOptionalString("nav.kafka.credstore_password", secret = true)?.let { credstorePassword ->
                    Pair(keystorePath, credstorePassword)
                }
            }

            val autoOffsetReset = when(val offsetReset =
                config.getOptionalString(key = "nav.kafka.auto_offset_reset", secret = false)
                    ?.lowercase(Locale.getDefault())) {
                null -> "none"
                "none" -> offsetReset
                "latest" -> offsetReset
                "earliest" -> offsetReset
                else -> throw IllegalArgumentException("Ugyldig verdi for nav.kafka.auto_offset_reset: $offsetReset")
            }

            KafkaConfig(
                bootstrapServers = bootstrapServers,
                trustStore = trustStore,
                keyStore = keyStore,
                exactlyOnce = trustStore != null,
                autoOffsetReset = autoOffsetReset,
                unreadyAfterStreamStoppedIn = unreadyAfterStreamStoppedIn()
            )
        }

    private fun getScopesFor(operation: String) = config.getRequiredList("nav.auth.scopes.$operation", secret = false, builder = { it }).toSet()
    internal fun getJournalforeScopes() = getScopesFor("journalfore")
    internal fun getK9MellomlagringScopes() = getScopesFor("k9_mellomlagring")
}
