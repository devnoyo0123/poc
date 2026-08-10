package com.example.zsetplayground.examples

import com.example.zsetplayground.runner.Console
import com.example.zsetplayground.runner.Console.api
import com.example.zsetplayground.runner.Console.banner
import com.example.zsetplayground.runner.Console.blank
import com.example.zsetplayground.runner.Console.cmd
import com.example.zsetplayground.runner.Console.note
import com.example.zsetplayground.runner.Console.result
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Component

/**
 * 실시간 랭킹보드 예제
 *
 * 시나리오: 게임 점수 랭킹
 * - score: 점수 (클수록 높은 순위)
 * - member: 유저 ID
 *
 * 핵심 명령:
 * - ZADD: 점수 등록/갱신
 * - ZREVRANGE: 상위 N명 (내림차순)
 * - ZREVRANK: 특정 멤버 순위
 * - ZINCRBY: 점수 증분
 */
@Component
class RankingExample(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val key = "game:rank"

    fun run() {
        banner("🏆 실시간 랭킹보드 (Ranking)")
        cleanUp()

        // ──────────────────────────────────────────────────────────
        step1_zadd()
        // ──────────────────────────────────────────────────────────
        step2_topN()
        // ──────────────────────────────────────────────────────────
        step3_rank()
        // ──────────────────────────────────────────────────────────
        step4_increment()
        // ──────────────────────────────────────────────────────────
        step5_topNAgain()

        cleanUp()
    }

    /** ZADD: 멤버 추가/갱신 */
    private fun step1_zadd() {
        blank()
        note("[Step 1] ZADD — 멤버와 점수 추가")

        // Raw: ZADD game:rank 1500 user:1
        cmd("ZADD $key 1500 user:1")
        api("""redis.opsForZSet().add("$key", "user:1", 1500.0)""")
        redisTemplate.opsForZSet().add(key, "user:1", 1500.0)
        result("user:1 점수", "1500 등록")

        cmd("ZADD $key 2300 user:2")
        redisTemplate.opsForZSet().add(key, "user:2", 2300.0)
        result("user:2 점수", "2300 등록")

        cmd("ZADD $key 1800 user:3")
        redisTemplate.opsForZSet().add(key, "user:3", 1800.0)
        result("user:3 점수", "1800 등록")

        cmd("ZADD $key 900 user:4")
        redisTemplate.opsForZSet().add(key, "user:4", 900.0)
        result("user:4 점수", "900 등록")

        note("※ ZADD = insert or update (같은 멤버는 score 갱신)")
    }

    /** ZREVRANGE: 상위 N명 (내림차순 = 점수 높은 순) */
    private fun step2_topN() {
        blank()
        note("[Step 2] ZREVRANGE — Top 3 (점수 내림차순, WITHSCORES)")

        // Raw: ZREVRANGE game:rank 0 2 WITHSCORES
        cmd("ZREVRANGE $key 0 2 WITHSCORES")
        api("""redis.opsForZSet().reverseRangeWithScores("$key", 0, 2)""")

        val top3: Set<ZSetOperations.TypedTuple<String>> =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 2) ?: emptySet()

        top3.forEachIndexed { idx, tuple ->
            result("${idx + 1}등", "${tuple.value} (score=${tuple.score?.toLong()})")
        }
    }

    /** ZREVRANK: 특정 멤버 순위 (0부터 시작, 내림차순 기준) */
    private fun step3_rank() {
        blank()
        note("[Step 3] ZREVRANK — user:3 순위 조회")

        cmd("ZREVRANK $key user:3")
        api("""redis.opsForZSet().reverseRank("$key", "user:3")""")

        val rank: Long? = redisTemplate.opsForZSet().reverseRank(key, "user:3")
        result("user:3 순위", "${(rank ?: -1) + 1}등 (0-based rank=$rank)")
        note("※ ZREVRANK는 0-based → 사람이 읽을 땐 +1 필요")
    }

    /** ZINCRBY: 점수 증분 (원자적 연산) */
    private fun step4_increment() {
        blank()
        note("[Step 4] ZINCRBY — user:4 점수 1000 증가 (900 → 1900)")

        cmd("ZINCRBY $key 1000 user:4")
        api("""redis.opsForZSet().incrementScore("$key", "user:4", 1000.0)""")

        val newScore: Double? = redisTemplate.opsForZSet().incrementScore(key, "user:4", 1000.0)
        result("user:4 새 점수", newScore?.toLong())
        note("※ 동시성 안전 (여러 스레드가 동시에 INCRBY 해도 정확)")
    }

    /** 갱신 후 다시 Top 3 */
    private fun step5_topNAgain() {
        blank()
        note("[Step 5] 갱신 후 Top 3 — 순위 변화 확인")

        cmd("ZREVRANGE $key 0 2 WITHSCORES")
        val top3 = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 2) ?: emptySet()
        top3.forEachIndexed { idx, tuple ->
            result("${idx + 1}등", "${tuple.value} (score=${tuple.score?.toLong()})")
        }
        note("user:4가 900→1900으로 2등으로 승격됨")
    }

    private fun cleanUp() {
        redisTemplate.delete(key)
    }
}
