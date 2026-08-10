package com.example.kafkamvc.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * concurrency=3 이면 이 리스너 메서드를 실행하는 스레드가 3개 뜬다.
 * 로그의 thread 이름과 partition 을 보면 "어느 스레드가 어느 파티션을 처리하는지" 가 보인다.
 */
@Component
class DemoConsumer {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.topic}"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consume(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        // (1) 먼저 처리
        log.info(
            "consume thread={} partition={} offset={} key={} value={}",
            Thread.currentThread().name,
            record.partition(),
            record.offset(),
            record.key(),
            record.value(),
        )

        // (2) 처리 후 매뉴얼 커밋 -> at-least-once
        //     리밸런스로 파티션이 넘어가도 여기서 커밋된 지점까지는 재처리되지 않는다.
        ack.acknowledge()
    }
}
