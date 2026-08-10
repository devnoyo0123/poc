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
 * 지연 큐 (Delay Queue) 예제
 *
 * 시나리오: 주문 생성 후 30분 뒤 자동 취소
 * - score: 실행 시각 (epoch ms)
 * - member: 작업 ID (예: "order:5001:expire")
 *
 * 핵심 패턴:
 * - 폴러가 주기적으로 ZRANGEBYSCORE로 "현재 시각 이전" 작업 조회
 * - 처리 후 ZREM으로 제거
 * - score 비교이므로 정확한 시간에 실행 가능
 */
@Component
class DelayQueueExample(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val key = "delay:queue"
    private val now = 1_719_900_000_000L

    fun run() {
        banner("⏰ 지연 큐 (Delay Queue)")
        cleanUp()

        step1_enqueue()
        step2_pollDue()
        step3_concurrentPoll()
        step4_cleanProcessed()

        cleanUp()
    }

    /** 작업 등록 — score = 실행 시각 */
    private fun step1_enqueue() {
        blank()
        note("[Step 1] ZADD — 지연 작업 등록 (score = 실행 시각)")

        val jobs = listOf(
            "order:5001:expire" to now + 30 * 60 * 1000, // 30분 뒤
            "order:5002:expire" to now + 30 * 60 * 1000,
            "order:5003:expire" to now + 10 * 60 * 1000, // 10분 뒤
            "order:5004:expire" to now - 60 * 1000,       // 이미 1분 전 만료
            "notify:200:schedule" to now - 30 * 1000,     // 이미 30초 전 만료
        )

        jobs.forEach { (jobId, executeAt) ->
            val delta = (executeAt - now) / 1000
            cmd("ZADD $key $executeAt $jobId")
            redisTemplate.opsForZSet().add(key, jobId, executeAt.toDouble())
            result(jobId, "실행 ${delta}초 후")
        }

        note("※ 음수 delta = 이미 실행 시각 지난 작업 (폴러가 즉시 처리)")
    }

    /** ZRANGEBYSCORE: 현재 시각까지 지난 작업 조회 */
    private fun step2_pollDue() {
        blank()
        note("[Step 2] ZRANGEBYSCORE — 실행 시각 지난 작업 조회 (now=$now)")

        // Raw: ZRANGEBYSCORE delay:queue 0 <now>
        cmd("ZRANGEBYSCORE $key 0 $now")
        api("""redis.opsForZSet().rangeByScore("$key", 0.0, $now.toDouble())""")

        val due: Set<String>? = redisTemplate.opsForZSet().rangeByScore(key, 0.0, now.toDouble())
        result("실행 대상", "${due?.size ?: 0}건")
        due?.forEach { result("  작업", it) }
    }

    /** 폴링 후 처리 + 제거 패턴 (실무 핵심) */
    private fun step3_concurrentPoll() {
        blank()
        note("[Step 3] 처리 패턴 — ZRANGEBYSCORE 후 ZREM (원자적 처리)")

        val due = redisTemplate.opsForZSet().rangeByScore(key, 0.0, now.toDouble()) ?: emptySet()

        for (job in due) {
            // 1. 작업 처리 (비즈니스 로직)
            note("처리 중: $job")

            // 2. ZREM으로 큐에서 제거
            // Raw: ZREM delay:queue "order:5004:expire"
            cmd("ZREM $key \"$job\"")
            api("""redis.opsForZSet().remove("$key", "$job")""")
            val removed: Long? = redisTemplate.opsForZSet().remove(key, job)
            result("제거 결과", "$removed 건")
        }

        blank()
        note("★ 실무 핵심: 멀티 인스턴스 환경에서는 Lua 스크립트로")
        note("  ZRANGEBYSCORE + ZREM을 원자적으로 수행해야 중복 처리 방지")
        note("  (또는 ZPOPMIN 또는 Redis Streams 고려)")
    }

    /** 남은 작업 확인 */
    private fun step4_cleanProcessed() {
        blank()
        note("[Step 4] 남은 작업 확인 — 아직 실행 시각 안 된 작업")

        cmd("ZRANGE $key 0 -1 WITHSCORES")
        api("""redis.opsForZSet().rangeWithScores("$key", 0, -1)""")

        val remaining = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1) ?: emptySet()
        result("남은 작업", "${remaining.size}건")
        remaining.forEach { tuple ->
            val delta = ((tuple.score?.toLong() ?: 0) - now) / 1000
            result("  작업", "${tuple.value} (실행 ${delta}초 후)")
        }
    }

    private fun cleanUp() {
        redisTemplate.delete(key)
    }
}
