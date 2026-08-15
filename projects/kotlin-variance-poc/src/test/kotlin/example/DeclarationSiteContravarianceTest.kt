package example

import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarationSiteContravarianceTest {
    @Test
    fun `in T - a general sink can be used as a specific sink`() {
        val received = mutableListOf<String>()
        val anySink: ValueSink<Any> = ValueSink { received += it.toString() }

        val textSink: ValueSink<String> = anySink
        textSink.accept("hello")

        assertEquals(listOf("hello"), received)
    }
}
