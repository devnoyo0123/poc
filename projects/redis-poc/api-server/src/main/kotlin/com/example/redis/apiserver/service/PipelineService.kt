package com.example.redis.apiserver.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class PipelineService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    fun setIndividually(count: Int): Map<String, Any> {
        val start = System.currentTimeMillis()

        val valueOps = redisTemplate.opsForValue()!!
        for (i in 1..count) {
            valueOps.set("pipeline:individual:$i", "value-$i")
        }

        val elapsed = System.currentTimeMillis() - start

        (1..count).forEach { redisTemplate.delete("pipeline:individual:$it") }

        return mapOf(
            "method" to "Individual SET",
            "count" to count,
            "elapsedMs" to elapsed,
            "rttCount" to count
        )
    }

    fun setWithPipeline(count: Int): Map<String, Any> {
        val start = System.currentTimeMillis()

        redisTemplate.executePipelined { connection ->
            for (i in 1..count) {
                connection.stringCommands().set(
                    "pipeline:batch:$i".toByteArray(),
                    "value-$i".toByteArray()
                )
            }
            null
        }

        val elapsed = System.currentTimeMillis() - start

        (1..count).forEach { redisTemplate.delete("pipeline:batch:$it") }

        return mapOf(
            "method" to "Pipeline SET",
            "count" to count,
            "elapsedMs" to elapsed,
            "rttCount" to 1
        )
    }

    fun getMultipleWithPipeline(keys: List<String>): Map<String, Any> {
        val start = System.currentTimeMillis()

        val results = redisTemplate.executePipelined { connection ->
            for (key in keys) {
                connection.stringCommands().get(key.toByteArray())
            }
            null
        }

        val elapsed = System.currentTimeMillis() - start

        val resultMap = mutableMapOf<String, String?>()
        keys.forEachIndexed { index, key ->
            resultMap[key] = results[index] as? String
        }

        return mapOf(
            "method" to "Pipeline GET",
            "keyCount" to keys.size,
            "elapsedMs" to elapsed,
            "results" to resultMap
        )
    }
}
