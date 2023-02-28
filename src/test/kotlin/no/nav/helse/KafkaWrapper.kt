package no.nav.helse

import no.nav.helse.prosessering.v1.asynkron.Data
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import no.nav.helse.prosessering.v1.asynkron.Topics.CLEANUP_ETTERSENDING
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT_ETTERSENDING_V2
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSESSERT_ETTERSENDING
import no.nav.helse.prosessering.v1.asynkron.k9EttersendingKonfigurertMapper
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.felles.Metadata
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

private const val confluentVersion = "7.2.1"
private lateinit var kafkaContainer: KafkaContainer

object KafkaWrapper {
    fun bootstrap(): KafkaContainer {
        kafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:$confluentVersion")
        )
        kafkaContainer.start()
        kafkaContainer.createTopicsForTest()
        return kafkaContainer
    }
}

private fun KafkaContainer.createTopicsForTest() {
    // Dette er en workaround for att testcontainers (pr. versjon 1.17.5) ikke håndterer autocreate topics
    AdminClient.create(testProducerProperties("admin")).createTopics(
        listOf(
            NewTopic(MOTTATT_ETTERSENDING_V2.name, 1, 1),
            NewTopic(PREPROSESSERT_ETTERSENDING.name, 1, 1),
            NewTopic(CLEANUP_ETTERSENDING.name, 1, 1),
        )
    )
}

private fun KafkaContainer.testConsumerProperties(groupId: String): MutableMap<String, Any> {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
}

private fun KafkaContainer.testProducerProperties(clientId: String): MutableMap<String, Any> {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    }
}

fun KafkaContainer.cleanupKonsumerEttersending(): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("EttersendingDagerKonsumer"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(CLEANUP_ETTERSENDING.name))
    return consumer
}

fun KafkaContainer.meldingEttersendingProducer() = KafkaProducer(
    testProducerProperties("K9EttersendingProsesseringTestProducer"),
    MOTTATT_ETTERSENDING_V2.keySerializer,
    MOTTATT_ETTERSENDING_V2.serDes
)

fun KafkaConsumer<String, String>.hentCleanupMeldingEttersending(
    soknadId: String,
    maxWaitInSeconds: Long = 30
): String {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(CLEANUP_ETTERSENDING.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke opprettet oppgave for søknad $soknadId etter $maxWaitInSeconds sekunder.")
}

fun KafkaProducer<String, TopicEntry>.leggTilMottak(soknad: EttersendingV1) {
    send(
        ProducerRecord(
            MOTTATT_ETTERSENDING_V2.name,
            soknad.søknadId,
            TopicEntry(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString(),
                    soknadDialogCommitSha = "abc-123"
                ),
                data = Data(k9EttersendingKonfigurertMapper().writeValueAsString(soknad))
            )
        )
    ).get()
}
