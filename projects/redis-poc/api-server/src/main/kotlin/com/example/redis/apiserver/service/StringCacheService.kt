package com.example.redis.apiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class StringCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val valueOps = redisTemplate.opsForValue()!!

    fun saveProduct(id: String, product: Map<String, Any>): String {
        val json = objectMapper.writeValueAsString(product)
        val key = "product:string:$id"
        valueOps.set(key, json, Duration.ofHours(1))
        return "Saved product:$id with TTL 1h"
    }

    fun getProduct(id: String): Map<String, Any>? {
        val json = valueOps.get("product:string:$id") ?: return null
        return objectMapper.readValue(json, Map::class.java) as Map<String, Any>
    }

    fun deleteProduct(id: String): String {
        val deleted = redisTemplate.delete("product:string:$id")
        return if (deleted == true) "Deleted product:$id" else "Key not found"
    }

    fun getTTL(id: String): Long? {
        return redisTemplate.getExpire("product:string:$id")
    }

    @Suppress("UNCHECKED_CAST")
    fun decreaseStockUnsafe(id: String, quantity: Int): Map<String, Any> {
        val key = "product:string:$id"
        val json = valueOps.get(key) ?: return mapOf("error" to "Product not found")

        val product = objectMapper.readValue(json, Map::class.java) as MutableMap<String, Any>
        val currentStock = (product["stock"] as Number).toInt()
        val newStock = currentStock - quantity

        if (newStock < 0) {
            return mapOf("error" to "Insufficient stock", "currentStock" to currentStock)
        }

        product["stock"] = newStock
        valueOps.set(key, objectMapper.writeValueAsString(product), Duration.ofHours(1))

        return mapOf("productId" to id, "previousStock" to currentStock, "newStock" to newStock, "method" to "String (unsafe)")
    }
}
