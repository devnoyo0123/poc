package com.example.redis.apiserver.service

import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class DistributedLockService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val unlockScript = DefaultRedisScript<Long>().apply {
        setScriptText("""
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("DEL", KEYS[1])
            else
                return 0
            end
        """.trimIndent())
        resultType = Long::class.java
    }

    fun acquireLock(lockKey: String, expireSeconds: Long = 10): Map<String, Any> {
        val lockValue = UUID.randomUUID().toString()
        val success = redisTemplate.opsForValue()
            ?.setIfAbsent(lockKey, lockValue, Duration.ofSeconds(expireSeconds))

        return mapOf(
            "lockKey" to lockKey,
            "lockValue" to lockValue,
            "acquired" to (success == true),
            "expireSeconds" to expireSeconds
        )
    }

    fun releaseLock(lockKey: String, lockValue: String): Map<String, Any> {
        val result = redisTemplate.execute(
            unlockScript,
            listOf(lockKey),
            lockValue
        )

        return mapOf(
            "lockKey" to lockKey,
            "released" to (result == 1L),
            "method" to "Lua script (atomic)"
        )
    }

    fun decreaseStockWithLock(productId: String, quantity: Int): Map<String, Any> {
        val lockKey = "lock:stock:$productId"
        val stockKey = "product:hash:$productId"
        val lockValue = UUID.randomUUID().toString()
        val hashOps: HashOperations<String, String, String> = redisTemplate.opsForHash()!!

        val acquired = redisTemplate.opsForValue()
            ?.setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))

        if (acquired != true) {
            return mapOf("error" to "Lock acquisition failed. Another process is handling this.", "productId" to productId)
        }

        try {
            val currentStock = hashOps.get(stockKey, "stock")?.toString()?.toLong()
                ?: return mapOf("error" to "Product not found")

            if (currentStock < quantity.toLong()) {
                return mapOf("error" to "Insufficient stock", "currentStock" to currentStock, "requested" to quantity)
            }

            val newStock = hashOps.increment(stockKey, "stock", -quantity.toLong())!!

            return mapOf(
                "productId" to productId,
                "previousStock" to currentStock,
                "newStock" to newStock,
                "method" to "Distributed lock + Hash HINCRBY"
            )
        } finally {
            redisTemplate.execute(unlockScript, listOf(lockKey), lockValue)
        }
    }

    fun releaseLockUnsafe(lockKey: String, expectedValue: String): Map<String, Any> {
        val currentValue = redisTemplate.opsForValue()?.get(lockKey)

        if (currentValue == expectedValue) {
            redisTemplate.delete(lockKey)
            return mapOf("released" to true, "warning" to "UNSAFE: Race condition possible between GET and DEL")
        }

        return mapOf("released" to false, "reason" to "Lock value mismatch")
    }
}
