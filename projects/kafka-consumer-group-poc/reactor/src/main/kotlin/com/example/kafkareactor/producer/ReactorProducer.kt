package com.example.kafkareactor.producer

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord

/**
 * reactor-kafka 의 리액티브 프로듀서.
 *   POST /produce?count=60
 */
@RestController
class ReactorProducer(
    @Value("\${app.bootstrap-servers}") bootstrapServers: String,
    @Value("\${app.topic}") private val topic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val sender: KafkaSender<String, String> = run {
        val props = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        )
        KafkaSender.create(SenderOptions.create(props))
    }

    @PostMapping("/produce")
    fun produce(@RequestParam(defaultValue = "60") count: Int): Mono<String> {
        val records = Flux.range(0, count).map { i ->
            SenderRecord.create(ProducerRecord(topic, "key-$i", "message-$i"), i)
        }
        return sender.send(records)
            .then(Mono.fromCallable {
                log.info("produced {} messages to topic={}", count, topic)
                "produced $count messages to $topic"
            })
    }
}
