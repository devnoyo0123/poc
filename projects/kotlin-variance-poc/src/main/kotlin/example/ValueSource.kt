package example

fun interface ValueSource<out T> {
    fun next(): T
}
