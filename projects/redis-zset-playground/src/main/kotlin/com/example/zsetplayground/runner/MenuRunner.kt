package com.example.zsetplayground.runner

import com.example.zsetplayground.examples.DelayQueueExample
import com.example.zsetplayground.examples.FcfsExample
import com.example.zsetplayground.examples.RankingExample
import com.example.zsetplayground.examples.RecentSearchExample
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.Scanner

@Component
class MenuRunner(
    private val rankingExample: RankingExample,
    private val fcfsExample: FcfsExample,
    private val delayQueueExample: DelayQueueExample,
    private val recentSearchExample: RecentSearchExample,
) : CommandLineRunner {

    private val scanner = Scanner(System.`in`)

    override fun run(vararg args: String?) {
        println()
        println("╔══════════════════════════════════════╗")
        println("║  Redis Sorted Set Playground         ║")
        println("╚══════════════════════════════════════╝")
        println()
        println("Sorted Set (ZSet) 실무 패턴 4가지를 직접 실행해보는 학습용 프로젝트")
        println("각 단계마다 redis-cli 명령어 + Spring API 호출 + 결과가 함께 출력됨")
        println()

        while (true) {
            printMenu()
            val input = readLineTrimmed()

            when (input) {
                "1" -> rankingExample.run()
                "2" -> fcfsExample.run()
                "3" -> delayQueueExample.run()
                "4" -> recentSearchExample.run()
                "5" -> runAll()
                "0", "q", "quit", "exit" -> {
                    println("\n👋 종료합니다.")
                    return
                }
                "" -> continue
                else -> println("\n⚠️  잘못된 입력: $input (1-5 또는 0)")
            }

            println("\n엔터 치면 메뉴로 돌아감...")
            readLineTrimmed()
        }
    }

    private fun runAll() {
        rankingExample.run()
        fcfsExample.run()
        delayQueueExample.run()
        recentSearchExample.run()
    }

    private fun printMenu() {
        println()
        println("┌─────────────────────────────────────┐")
        println("│  메뉴 선택                          │")
        println("├─────────────────────────────────────┤")
        println("│  1. 🏆 실시간 랭킹보드              │")
        println("│     (ZADD/ZREVRANGE/ZREVRANK/ZINCRBY)│")
        println("│  2. 🎟️  선착순 이벤트               │")
        println("│     (ZADD/ZCARD/ZRANGE/ZRANK)       │")
        println("│  3. ⏰  지연 큐                     │")
        println("│     (ZADD/ZRANGEBYSCORE/ZREM)       │")
        println("│  4. 🔍 최근 검색어                  │")
        println("│     (ZADD/ZRANGE/ZREMRANGEBYRANK)   │")
        println("│  5. 🚀 전체 실행                    │")
        println("│  0. 종료                            │")
        println("└─────────────────────────────────────┘")
        print("선택> ")
    }

    private fun readLineTrimmed(): String = scanner.nextLine().trim()
}
