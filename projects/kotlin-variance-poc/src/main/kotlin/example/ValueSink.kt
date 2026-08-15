package example

fun interface ValueSink<in T> {
    fun accept(it: T)
}
