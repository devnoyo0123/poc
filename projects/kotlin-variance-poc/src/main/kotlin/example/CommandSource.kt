package example

fun interface CommandSource<out T> {
    fun next(): T
}
