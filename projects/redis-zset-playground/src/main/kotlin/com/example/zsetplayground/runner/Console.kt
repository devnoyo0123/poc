package com.example.zsetplayground.runner

/**
 * 콘솔 출력 헬퍼
 * - Raw Redis 명령어와 Spring API 호출 결과를 함께 보여주기 위함
 */
object Console {

    fun banner(title: String) {
        println()
        println("╔${"═".repeat(title.length + 8)}╗")
        println("║   $title   ║")
        println("╚${"═".repeat(title.length + 8)}╝")
    }

    /** redis-cli에서 치는 원시 명령어 표시 */
    fun cmd(command: String) {
        println("  redis> $command")
    }

    /** Spring API 호출 표시 */
    fun api(call: String) {
        println("  kotlin> $call")
    }

    /** 결과 출력 */
    fun result(label: String, value: Any?) {
        println("  → $label: ${format(value)}")
    }

    /** 설명 출력 */
    fun note(text: String) {
        println("  // $text")
    }

    fun blank() = println()

    private fun format(value: Any?): String = when (value) {
        null -> "(nil)"
        is Collection<*> -> if (value.isEmpty()) "(empty)" else value.joinToString(", ")
        is Map<*, *> -> if (value.isEmpty()) "(empty)" else value.entries.joinToString(", ") { "${it.key}=${it.value}" }
        else -> value.toString()
    }
}
