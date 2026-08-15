package example

import kotlin.test.Test
import kotlin.test.assertEquals

class VarianceTest {
    @Test
    fun `invariant - a box reads and writes only its exact command type`() {
        val box: CommandBox<CatFeedingCommand> =
            CommandBox(CatFeedingCommand("maru", "tuna"))

        box.value = CatFeedingCommand("nabi", "salmon")

        assertEquals("nabi", box.value.animalName)

        // Green 후 아래 주석을 해제하면 컴파일 오류가 나야 합니다.
        // val commandBox: CommandBox<FeedingCommand> = box
    }
}
