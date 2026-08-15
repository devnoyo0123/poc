package example

import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarationSiteCovarianceTest {
    @Test
    fun `out T - a specific source can be used as a general source`() {
        val textSource: ValueSource<String> = ValueSource { "hello" }

        val anySource: ValueSource<Any> = textSource

        assertEquals("hello", anySource.next())
    }
}
