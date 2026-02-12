package com.example.queue.queue

import mu.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Component
class RedisQueueBackend(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) : QueueBackend {

    companion object {
        private const val QUEUE_KEY = "queue:waiting"
        private const val ACTIVE_KEY_PREFIX = "active:"
    }

    override fun addToQueue(userId: String, timestamp: Long): Mono<Boolean> {
        return redisTemplate.opsForZSet()
            .addIfAbsent(QUEUE_KEY, userId, timestamp.toDouble())
            .doOnSuccess { added ->
                if (added) {
                    logger.debug { "User $userId added to queue with timestamp $timestamp" }
                } else {
                    logger.debug { "User $userId already in queue" }
                }
            }
    }

    override fun getPosition(userId: String): Mono<Long?> {
        return redisTemplate.opsForZSet()
            .rank(QUEUE_KEY, userId)
            .doOnSuccess { rank ->
                logger.debug { "User $userId position: $rank" }
            }
    }

    override fun getQueueSize(): Mono<Long> {
        return redisTemplate.opsForZSet()
            .size(QUEUE_KEY)
            .defaultIfEmpty(0L)
    }

    override fun removeFromQueue(userId: String): Mono<Boolean> {
        return redisTemplate.opsForZSet()
            .remove(QUEUE_KEY, userId)
            .map { it > 0 }
            .doOnSuccess { removed ->
                if (removed) {
                    logger.debug { "User $userId removed from queue" }
                }
            }
    }

    override fun popNext(count: Long): Mono<List<String>> {
        return redisTemplate.opsForZSet()
            .popMin(QUEUE_KEY, count)
            .map { it.value }
            .collectList()
            .doOnSuccess { users ->
                logger.debug { "Popped ${users.size} users from queue: $users" }
            }
    }

    override fun setActiveToken(userId: String, token: String, ttlSeconds: Long): Mono<Boolean> {
        val key = ACTIVE_KEY_PREFIX + userId
        return redisTemplate.opsForValue()
            .set(key, token, Duration.ofSeconds(ttlSeconds))
            .doOnSuccess {
                logger.debug { "Set active token for user $userId with TTL $ttlSeconds seconds" }
            }
    }

    override fun getActiveToken(userId: String): Mono<String?> {
        val key = ACTIVE_KEY_PREFIX + userId
        return redisTemplate.opsForValue()
            .get(key)
    }

    override fun isInQueue(userId: String): Mono<Boolean> {
        return redisTemplate.opsForZSet()
            .rank(QUEUE_KEY, userId)
            .map { true }
            .defaultIfEmpty(false)
    }
}
