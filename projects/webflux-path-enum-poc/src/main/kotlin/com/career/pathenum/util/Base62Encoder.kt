package com.career.pathenum.util

/**
 * Snowflake ID (Long) → base62 5자리 고정폭 문자열 인코더.
 *
 * - 문자 순서: 0-9, A-Z, a-z (ASCII/Unicode 코드 포인트 순서 = base62 값 순서)
 * - 5자리 zero padding (예: 1 → "00001", 500 → "0007u")
 * - 5자리 base62 표현 가능 수: 62^5 = 916,132,832 (약 9억)
 */
object Base62Encoder {
    private const val BASE = 62
    private const val FIXED_WIDTH = 5
    private val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()

    init {
        // 검증: CHARS가 ASCII 코드 포인트 순서(오름차순)와 일치하는지.
        // (원본 스펙의 의도 보존 — 컴파일 가능하도록 배열 비교 API 사용)
        require(CHARS.contentEquals(CHARS.sortedArray())) { "CHARS must be sorted" }
    }

    fun encode(value: Long): String {
        require(value >= 0) { "value must be non-negative: $value" }
        if (value == 0L) return "0".padStart(FIXED_WIDTH, '0')

        val sb = StringBuilder()
        var v = value
        while (v > 0) {
            sb.insert(0, CHARS[(v % BASE).toInt()])
            v /= BASE
        }
        return sb.toString().padStart(FIXED_WIDTH, '0')
    }

    fun decode(s: String): Long {
        require(s.length == FIXED_WIDTH) { "expected $FIXED_WIDTH chars, got ${s.length}" }
        return s.fold(0L) { acc, c ->
            val idx = CHARS.indexOf(c)
            require(idx >= 0) { "invalid char: $c" }
            acc * BASE + idx
        }
    }

    /** path 문자열에서 depth 계산 (path 길이 / 5) */
    fun depthOf(path: String): Int = path.length / FIXED_WIDTH

    /** path에서 마지막 5자리(자기 자신 ID) 추출 */
    fun ownIdSegment(path: String): String = path.takeLast(FIXED_WIDTH)

    /** 부모 path 추출 (없으면 null) */
    fun parentPath(path: String): String? {
        if (path.length <= FIXED_WIDTH) return null
        return path.dropLast(FIXED_WIDTH)
    }

    /** 자손 검색용 LIKE prefix (부모 path 그대로, % 안 붙임) */
    fun descendantPrefix(path: String): String = path
}
