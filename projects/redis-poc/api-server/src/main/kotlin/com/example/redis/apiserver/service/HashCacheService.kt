package com.example.redis.apiserver.service

import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class HashCacheService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val hashOps: HashOperations<String, String, String> = redisTemplate.opsForHash()!!

    fun saveProduct(id: String, name: String, price: Int, stock: Int): String {
        val key = "product:hash:$id"
        hashOps.putAll(key, mapOf(
            "name" to name,
            "price" to price.toString(),
            "stock" to stock.toString()
        ))
        redisTemplate.expire(key, Duration.ofHours(1))
        return "Saved product:hash:$id as Hash fields"
    }

    @Suppress("UNCHECKED_CAST")
    fun getProduct(id: String): Map<String, String> {
        return hashOps.entries("product:hash:$id") as Map<String, String>
    }

    fun getField(id: String, field: String): String? {
        return hashOps.get("product:hash:$id", field)
    }

    fun decreaseStock(id: String, quantity: Int): Map<String, Any> {
        val key = "product:hash:$id"
        val newStock = hashOps.increment(key, "stock", -quantity.toLong())!!

        if (newStock < 0) {
            hashOps.increment(key, "stock", quantity.toLong())
            return mapOf("error" to "Insufficient stock", "method" to "Hash (atomic)")
        }

        return mapOf("productId" to id, "newStock" to newStock, "method" to "Hash (atomic)")
    }

    fun updateName(id: String, newName: String): String {
        hashOps.put("product:hash:$id", "name", newName)
        return "Updated name field only"
    }

    fun incrementApiCounter(apiName: String): Long {
        return hashOps.increment("api:counters", apiName, 1) ?: 0L
    }

    @Suppress("UNCHECKED_CAST")
    fun getAllCounters(): Map<String, String> {
        return hashOps.entries("api:counters") as Map<String, String>
    }

    fun concurrencyTest(id: String, iterations: Int = 100): Map<String, Any> {
        val key = "product:hash:$id"
        hashOps.put(key, "stock", iterations.toString())

        val threads = (1..iterations).map {
            Thread {
                hashOps.increment(key, "stock", -1L)
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val finalStock = hashOps.get(key, "stock")?.toString()?.toLong()

        return mapOf(
            "initialStock" to iterations as Any,
            "iterations" to iterations as Any,
            "finalStock" to (finalStock as Any),
            "expectedStock" to 0 as Any,
            "isCorrect" to (finalStock == 0L) as Any,
            "method" to "Hash HINCRBY (atomic)" as Any
        )
    }
}
