package com.example.kafkareactor.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord

/**
 * reactor-kafka 데모 포인트
 *  1) KafkaReceiver 를 N개 생성 -> 그룹 안의 컨슈머 N개 (인스턴스 1개로 컨슈머 여러 개)
 *  2) groupBy(partition) + publishOn(parallel) -> 파티션 내 순서 보장 + 파티션 간 병렬 처리
 *  3) addAssignListener / addRevokeListener -> 리밸런스 관찰
 *  4) receiverOffset().acknowledge() -> 매뉴얼 커밋 (auto-commit OFF)
 */
@Configuration
class ReactorConsumer(
    @Value("\${app.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${app.topic}") private val topic: String,
    @Value("\${app.group-id}") private val groupId: String,
    @Value("\${app.receivers:1}") private val receiverCount: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun startReceivers() = ApplicationRunner {
        repeat(receiverCount) { idx ->
            subscribe(idx, buildReceiver(idx))
        }
        log.info("started {} reactor KafkaReceiver(s) on group={}", receiverCount, groupId)
    }

    private fun buildReceiver(idx: Int): KafkaReceiver<String, String> {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor",
        )

        val options = ReceiverOptions.create<String, String>(props)
            .subscription(listOf(topic))
            // ★ 리밸런스 관찰: reactor-kafka 는 리스너를 ReceiverOptions 에 단다 (Spring Kafka 와 위치만 다름)
            .addAssignListener { partitions ->
                log.warn("[REBALANCE] << ASSIGNED receiver#{} partitions={}",
                    idx, partitions.map { it.topicPartition().partition() }.sorted())
            }
            .addRevokeListener { partitions ->
                log.warn("[REBALANCE] >> REVOKE receiver#{} partitions={}",
                    idx, partitions.map { it.topicPartition().partition() }.sorted())
            }

        return KafkaReceiver.create(options)
    }

    private fun subscribe(idx: Int, receiver: KafkaReceiver<String, String>) {
        receiver.receive()
            // ★ 파티션별로 스트림을 분리: 같은 파티션은 한 그룹 -> 순서 보장
            .groupBy { it.receiverOffset().topicPartition() }
            .flatMap { grouped ->
                grouped
                    .publishOn(Schedulers.parallel())   // 파티션 간 병렬
                    .concatMap { record -> handle(idx, record) } // 파티션 내 순서 보장
            }
            .subscribe()
    }

    private fun handle(idx: Int, record: ReceiverRecord<String, String>): Mono<Void> =
        Mono.fromRunnable<Void> {
            log.info(
                "consume receiver#{} thread={} partition={} offset={} key={} value={}",
                idx,
                Thread.currentThread().name,
                record.partition(),
                record.offset(),
                record.key(),
                record.value(),
            )
        }.doOnSuccess {
            // ★ 매뉴얼 커밋: acknowledge() 는 commit 대상으로 표시(기본 commitInterval 마다 flush).
            //   즉시 커밋이 필요하면 record.receiverOffset().commit() (Mono) 을 체이닝하면 된다.
            record.receiverOffset().acknowledge()
        }
}
