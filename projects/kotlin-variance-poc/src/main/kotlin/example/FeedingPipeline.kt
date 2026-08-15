package example

interface FeedingPipeline {
    fun supports(): Class<out FeedingCommand>
}
