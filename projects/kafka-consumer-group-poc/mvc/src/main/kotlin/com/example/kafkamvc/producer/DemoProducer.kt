package com.example.kafkamvc.producer

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 테스트용 프로듀서. key 를 다양하게 줘서 6개 파티션에 메시지가 골고루 퍼지게 한다.
 *   POST /produce?count=60
 */
@RestController
class DemoProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${app.topic}") private val topic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/produce")
    fun produce(@RequestParam(defaultValue = "60") count: Int): String {
        repeat(count) { i ->
            // key 를 i 로 주면 hash(key) % partitions 로 분산된다.
            kafkaTemplate.send(topic, "key-$i", "message-$i")
        }
        log.info("produced {} messages to topic={}", count, topic)
        return "produced $count messages to $topic"
    }
}
