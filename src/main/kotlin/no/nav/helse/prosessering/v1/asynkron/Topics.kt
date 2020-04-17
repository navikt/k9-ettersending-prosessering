package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.ettersending.PreprosessertEttersendingV1
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.k9.ettersendelse.Ettersendelse
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(val metadata: Metadata, val data: V)

data class CleanupEttersending(val metadata: Metadata, val melding: PreprosessertEttersendingV1, val journalførtMelding: JournalfortEttersending)
data class JournalfortEttersending(val journalpostId: String, val søknad: Ettersendelse)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object Topics {
    val MOTTATT_ETTERSENDING = Topic(
        name = "privat-k9-ettersending-mottatt",
        serDes = MottattSoknadSerDesEttersending()
    )
    val PREPROSSESERT_ETTERSENDING = Topic(
        name = "privat-k9-ettersending-preprossesert",
        serDes = PreprossesertSerDesEttersending()
    )
    val CLEANUP_ETTERSENDING = Topic(
        name = "privat-k9-ettersending-cleanup",
        serDes = CleanupSerDesEttersending()
    )
    val JOURNALFORT_ETTERSENDING = Topic(
        name = "privat-k9-digital-ettersendelse-journalfort",
        serDes = JournalfortSerDesEttersending()
    )
}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = jacksonObjectMapper()
        .dusseldorfConfigured()
        .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

private class MottattSoknadSerDesEttersending: SerDes<TopicEntry<EttersendingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<EttersendingV1>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<EttersendingV1>>(it)
        }
    }
}

private class PreprossesertSerDesEttersending: SerDes<TopicEntry<PreprosessertEttersendingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprosessertEttersendingV1>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class CleanupSerDesEttersending: SerDes<TopicEntry<CleanupEttersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<CleanupEttersending>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class JournalfortSerDesEttersending: SerDes<TopicEntry<JournalfortEttersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<JournalfortEttersending>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
