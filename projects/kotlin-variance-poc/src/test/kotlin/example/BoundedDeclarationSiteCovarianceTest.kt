package example

import kotlin.test.Test
import kotlin.test.assertEquals

class BoundedDeclarationSiteCovarianceTest {
    @Test
    fun `out T Base - a cat command source can be used as a feeding command source`() {
        val catSource: CommandSource<CatFeedingCommand> =
            CommandSource { CatFeedingCommand("maru", "tuna") }

        val commandSource: CommandSource<FeedingCommand> = catSource

        assertEquals("maru", commandSource.next().animalName)

        // Green 후 아래 주석을 해제하면 상한 제한 때문에 컴파일 오류가 나야 합니다.
        // val invalid: CommandSource<String> = CommandSource { "hello" }
    }
}
