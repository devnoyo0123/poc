package com.example.zsetplayground.examples

import com.example.zsetplayground.runner.Console
import com.example.zsetplayground.runner.Console.api
import com.example.zsetplayground.runner.Console.banner
import com.example.zsetplayground.runner.Console.blank
import com.example.zsetplayground.runner.Console.cmd
import com.example.zsetplayground.runner.Console.note
import com.example.zsetplayground.runner.Console.result
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 선착순 이벤트 예제
 *
 * 시나리오: 1000명 선착순 쿠폰 발급
 * - score: 요청 timestamp (ms, 빠를수록 앞 순위)
 * - member: 유저 ID
 *
 * 핵심 특징:
 * - ZADD = insert or update → 같은 유저 재참여 시 score만 갱신 (멤버 수 증가 X)
 * - ZCARD: 전체 등록자 수
 * - ZRANGE: 1~1000위까지 (오름차순 = 빠른 timestamp 순)
 */
@Component
class FcfsExample(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val key = "fcfs:event:coupon-1000"
    private val baseTime = 1_719_900_000_000L // 가상 기준 시각

    fun run() {
        banner("🎟️ 선착순 이벤트 (First-Come-First-Served)")
        cleanUp()

        step1_participate()
        step2_count()
        step3_duplicateCheck()
        step4_winners()
        step5_myRank()

        cleanUp()
    }

    /** 사용자 참여 */
    private fun step1_participate() {
        blank()
        note("[Step 1] ZADD — 사용자 참여 (score=timestamp, member=userId)")

        val users = listOf(
            "user:101" to baseTime + 1,
            "user:102" to baseTime + 50,
            "user:103" to baseTime + 100,
            "user:104" to baseTime + 200,
            "user:105" to baseTime + 500,
        )

        users.forEach { (userId, ts) ->
            // Raw: ZADD fcfs:event:coupon-1000 1719900000001 user:101
            cmd("ZADD $key $ts $userId")
            redisTemplate.opsForZSet().add(key, userId, ts.toDouble())
            result(userId, "참여 ($ts ms)")
        }

        note("score=timestamp → 빠를수록 앞 순위 (오름차순 = 선착순)")
    }

    /** ZCARD: 현재 등록자 수 */
    private fun step2_count() {
        blank()
        note("[Step 2] ZCARD — 현재 등록자 수")

        // Raw: ZCARD fcfs:event:coupon-1000
        cmd("ZCARD $key")
        api("""redis.opsForZSet().zCard("$key")""")

        val count: Long? = redisTemplate.opsForZSet().zCard(key)
        result("등록자 수", "$count 명")
    }

    /** ZADD 중복 참여 방지 시뮬레이션 */
    private fun step3_duplicateCheck() {
        blank()
        note("[Step 3] 중복 참여 — user:101이 더 빨리 다시 참여")

        // user:101이 더 빠른 timestamp로 재참여
        val earlier = baseTime - 100
        cmd("ZADD $key $earlier user:101")
        api("""redis.opsForZSet().add("$key", "user:101", $earlier.0)""")
        redisTemplate.opsForZSet().add(key, "user:101", earlier.toDouble())

        val count: Long? = redisTemplate.opsForZSet().zCard(key)
        result("등록자 수 (변경 없음)", "$count 명")
        note("※ ZADD는 insert-or-update — 같은 member는 score만 갱신, 중복 멤버 추가 X")
        note("※ 자동으로 중복 참여 방지됨 (별도 로직 불필요)")
    }

    /** ZRANGE: 1~3위 (선착순 컷) — score 오름차순 */
    private fun step4_winners() {
        blank()
        note("[Step 4] ZRANGE — 선착순 3명 (score 오름차순 = timestamp 빠른 순)")

        cmd("ZRANGE $key 0 2")
        api("""redis.opsForZSet().range("$key", 0, 2)""")

        val winners: Set<String>? = redisTemplate.opsForZSet().range(key, 0, 2)
        winners?.forEachIndexed { idx, userId ->
            result("${idx + 1}위", userId)
        }
    }

    /** ZRANK: 내 순위 확인 (0-based, 오름차순) */
    private fun step5_myRank() {
        blank()
        note("[Step 5] ZRANK — user:103 순위 (오름차순, 0-based)")

        cmd("ZRANK $key user:103")
        api("""redis.opsForZSet().rank("$key", "user:103")""")

        val rank: Long? = redisTemplate.opsForZSet().rank(key, "user:103")
        result("user:103 순위", "${(rank ?: -1) + 1}위 (0-based=$rank)")

        blank()
        note("★ 실무 패턴: 선착순 1000명 자르기")
        cmd("ZRANGE $key 0 999  → 1~1000위 (당첨)")
        cmd("ZREMRANGEBYRANK $key 0 999  → 당첨자 제거 (남은 사람은 탈락)")
    }

    private fun cleanUp() {
        redisTemplate.delete(key)
    }
}
