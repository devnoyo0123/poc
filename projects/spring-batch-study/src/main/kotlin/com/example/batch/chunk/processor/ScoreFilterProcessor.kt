package com.example.batch.chunk.processor

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class ScoreFilterProcessor : ItemProcessor<Map<String, String>, Map<String, String>> {

    override fun process(item: Map<String, String>): Map<String, String>? {
        val score = item["score"]?.toIntOrNull() ?: 0

        // 점수가 60점 미만이면 필터링 (null 반환)
        return if (score >= 60) {
            // 점수 등급 추가
            val grade = when (score) {
                in 90..100 -> "A"
                in 80..89 -> "B"
                in 70..79 -> "C"
                else -> "D"
            }

            val result = item.toMutableMap()
            result["grade"] = grade
            result
        } else {
            println("❌ Filtered: ${item["name"]} (score: $score)")
            null
        }
    }
}
