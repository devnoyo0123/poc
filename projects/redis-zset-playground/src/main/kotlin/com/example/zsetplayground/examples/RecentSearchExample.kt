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
 * 최근 검색어 예제
 *
 * 시나리오: 사용자별 최근 검색어 (최대 20개, 시간순)
 * - score: 검색 timestamp
 * - member: 검색어
 *
 * 핵심 특징:
 * - 같은 검색어 재검색 → score 갱신 → 자동으로 최근으로 이동
 * - ZREMRANGEBYRANK: 개수 제한 (최근 20개만 유지)
 */
@Component
class RecentSearchExample(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val userId = "user:1"
    private val key = "recent:search:$userId"
    private val baseTime = 1_719_900_000_000L

    fun run() {
        banner("🔍 최근 검색어 (Recent Search)")
        cleanUp()

        step1_search()
        step2_responsiveRange()
        step3_searchAgain()
        step4_enforceLimit()
        step5_delete()

        cleanUp()
    }

    /** 검색어 등록 — ZADD (insert or update) */
    private fun step1_search() {
        blank()
        note("[Step 1] ZADD — 검색어 등록 (score=timestamp)")

        val searches = listOf(
            "아이폰" to baseTime + 100,
            "갤럭시" to baseTime + 200,
            "아이폰15" to baseTime + 300,
            "맥북" to baseTime + 400,
            "에어팟" to baseTime + 500,
        )

        searches.forEach { (term, ts) ->
            // Raw: ZADD recent:search:user:1 1719900000100 "아이폰"
            cmd("ZADD $key $ts \"$term\"")
            redisTemplate.opsForZSet().add(key, term, ts.toDouble())
            result("검색", "\"$term\" @ $ts")
        }
    }

    /** ZRANGE: 최근 N개 (오름차순 → 마지막이 가장 최근) */
    private fun step2_responsiveRange() {
        blank()
        note("[Step 2] ZRANGE — 최근 검색어 조회 (오름차순)")

        // Raw: ZRANGE recent:search:user:1 0 -1
        cmd("ZRANGE $key 0 -1")
        api("""redis.opsForZSet().range("$key", 0, -1)""")

        val all: Set<String>? = redisTemplate.opsForZSet().range(key, 0, -1)
        result("전체 (오래된→최근)", all?.toList())

        blank()
        note("최근 3개만 (음수 인덱스 활용)")
        cmd("ZRANGE $key -3 -1")
        api("""redis.opsForZSet().range("$key", -3, -1)""")

        val recent3: Set<String>? = redisTemplate.opsForZSet().range(key, -3, -1)
        result("최근 3개", recent3?.toList())
        note("※ 음수 인덱스: -1=마지막, -2=뒤에서 2번째, ...")
    }

    /** 같은 검색어 재검색 — score 갱신 → 자동으로 최근으로 이동 */
    private fun step3_searchAgain() {
        blank()
        note("[Step 3] \"아이폰\" 재검색 — 가장 최근 timestamp로 갱신")

        val latest = baseTime + 9999
        cmd("ZADD $key $latest \"아이폰\"")
        api("""redis.opsForZSet().add("$key", "아이폰", $latest.toDouble())""")
        redisTemplate.opsForZSet().add(key, "아이폰", latest.toDouble())

        blank()
        note("재검색 후 순서 확인 — \"아이폰\"이 맨 뒤(최근)로 이동")
        cmd("ZRANGE $key 0 -1")
        val after = redisTemplate.opsForZSet().range(key, 0, -1)
        result("갱신 후", after?.toList())
        note("※ ZADD insert-or-update 덕분에 자동 갱신 — 별도 로직 필요 X")
    }

    /** ZREMRANGEBYRANK: 개수 제한 (최근 20개만 유지) */
    private fun step4_enforceLimit() {
        blank()
        note("[Step 4] 개수 제한 — 3개만 유지 (초과분 삭제)")

        // 일부러 3개 초과 데이터 추가
        listOf(
            "태블릿" to baseTime + 700,
            "워치" to baseTime + 800,
            "모니터" to baseTime + 900,
        ).forEach { (term, ts) ->
            redisTemplate.opsForZSet().add(key, term, ts.toDouble())
            result("추가 검색", "\"$term\"")
        }

        blank()
        result("현재 검색어 수", "${redisTemplate.opsForZSet().zCard(key)}개")
        val before = redisTemplate.opsForZSet().range(key, 0, -1)
        result("현재 목록", before?.toList())

        blank()
        note("최근 3개만 유지 — 나머지 삭제")
        // Raw: ZREMRANGEBYRANK recent:search:user:1 0 -4
        // → 0부터 뒤에서 4번째까지 (= 가장 오래된 것부터 개수-3개까지)
        cmd("ZREMRANGEBYRANK $key 0 -4")
        api("""redis.opsForZSet().removeRange("$key", 0, -4)""")

        val removed: Long? = redisTemplate.opsForZSet().removeRange(key, 0, -4)
        result("삭제된 수", "$removed 건")

        blank()
        val after = redisTemplate.opsForZSet().range(key, 0, -1)
        result("최종 목록 (최근 3개)", after?.toList())
        note("※ -4 = 뒤에서 4번째 = 보존하려는 개수+1 위치")
        note("  20개 유지: removeRange(key, 0, -21)")
    }

    /** ZREM: 개별 검색어 삭제 */
    private fun step5_delete() {
        blank()
        note("[Step 5] ZREM — 특정 검색어 삭제")

        cmd("ZREM $key \"맥북\"")
        api("""redis.opsForZSet().remove("$key", "맥북")""")
        val removed: Long? = redisTemplate.opsForZSet().remove(key, "맥북")
        result("삭제 결과", "$removed 건")

        val after = redisTemplate.opsForZSet().range(key, 0, -1)
        result("남은 목록", after?.toList())
    }

    private fun cleanUp() {
        redisTemplate.delete(key)
    }
}
