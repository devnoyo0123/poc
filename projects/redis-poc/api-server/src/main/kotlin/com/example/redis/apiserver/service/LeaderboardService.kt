package com.example.redis.apiserver.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class LeaderboardService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val zSetOps = redisTemplate.opsForZSet()!!
    private val leaderboardKey = "game:leaderboard"

    fun addScore(playerId: String, score: Double): String {
        zSetOps.add(leaderboardKey, playerId, score)
        return "Added $playerId with score $score"
    }

    fun incrementScore(playerId: String, delta: Double): Double? {
        return zSetOps.incrementScore(leaderboardKey, playerId, delta)
    }

    fun getTopN(n: Long): List<Map<String, Any>> {
        val typedTuples = zSetOps.reverseRangeWithScores(leaderboardKey, 0, n - 1)
        return typedTuples?.mapIndexed { index, tuple ->
            mapOf(
                "rank" to (index + 1),
                "playerId" to (tuple.value ?: "unknown"),
                "score" to (tuple.score ?: 0.0)
            )
        } ?: emptyList()
    }

    fun getMyRank(playerId: String): Map<String, Any> {
        val rank = zSetOps.reverseRank(leaderboardKey, playerId)
        val score = zSetOps.score(leaderboardKey, playerId)

        return if (rank != null && score != null) {
            mapOf("playerId" to playerId, "rank" to (rank + 1), "score" to score)
        } else {
            mapOf("playerId" to playerId, "rank" to "not found", "score" to "N/A")
        }
    }

    fun getTotalPlayers(): Long {
        return zSetOps.size(leaderboardKey) ?: 0L
    }

    fun getByScoreRange(min: Double, max: Double): List<Map<String, Any>> {
        val typedTuples = zSetOps.reverseRangeByScoreWithScores(leaderboardKey, min, max)
        return typedTuples?.mapIndexed { index, tuple ->
            mapOf(
                "rank" to (index + 1),
                "playerId" to (tuple.value ?: "unknown"),
                "score" to (tuple.score ?: 0.0)
            )
        } ?: emptyList()
    }

    fun clear(): String {
        redisTemplate.delete(leaderboardKey)
        return "Leaderboard cleared"
    }
}
