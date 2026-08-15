package example

class CatFeedingPipeline : FeedingPipeline {
    override fun supports(): Class<CatFeedingCommand> {
        return CatFeedingCommand::class.java
    }
}
