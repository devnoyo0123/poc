package com.example.kafkamvc.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener
import org.springframework.kafka.listener.ContainerProperties

/**
 * 핵심 데모 포인트
 *  1) concurrency = N  -> 인스턴스(JVM) 하나 안에서 컨슈머 스레드 N개 (= KafkaConsumer N개)
 *  2) AckMode.MANUAL_IMMEDIATE -> 매뉴얼 커밋 (acknowledge() 호출 즉시 오프셋 커밋)
 *  3) ConsumerRebalanceListener -> 리밸런스 시 onPartitionsRevoked / onPartitionsAssigned 로그
 */
@Configuration
class KafkaConsumerConfig(
    @Value("\${app.concurrency:3}") private val concurrency: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun kafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        consumerFactory: ConsumerFactory<Any, Any>,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        configurer.configure(factory, consumerFactory)

        // (1) 스레드 N개 = 컨슈머 N개. 단 실제 활성 스레드는 (할당받은 파티션 수)로 제한된다.
        factory.setConcurrency(concurrency)

        // (2) 매뉴얼 커밋: 처리 -> acknowledge() 순서로 at-least-once
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE

        // (3) 리밸런스 발생 시 파티션이 어떻게 회수/할당되는지 눈으로 확인
        factory.containerProperties.setConsumerRebalanceListener(loggingRebalanceListener())

        return factory
    }

    private fun loggingRebalanceListener() = object : ConsumerAwareRebalanceListener {
        // 협력적(Cooperative) 리밸런스에서 "이동이 필요한 파티션만" 회수될 때 호출
        override fun onPartitionsRevokedBeforeCommit(
            consumer: Consumer<*, *>,
            partitions: Collection<TopicPartition>,
        ) {
            log.warn("[REBALANCE] >> REVOKE (commit 직전) thread={} partitions={}",
                Thread.currentThread().name, partitions.map { it.partition() }.sorted())
        }

        override fun onPartitionsAssigned(
            consumer: Consumer<*, *>,
            partitions: Collection<TopicPartition>,
        ) {
            log.warn("[REBALANCE] << ASSIGNED thread={} partitions={}",
                Thread.currentThread().name, partitions.map { it.partition() }.sorted())
        }

        override fun onPartitionsLost(
            consumer: Consumer<*, *>,
            partitions: Collection<TopicPartition>,
        ) {
            log.error("[REBALANCE] !! LOST thread={} partitions={}",
                Thread.currentThread().name, partitions.map { it.partition() }.sorted())
        }
    }
}
