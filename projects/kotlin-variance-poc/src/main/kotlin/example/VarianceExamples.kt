package example

sealed interface FeedingCommand {
    val animalName: String
}

data class CatFeedingCommand(
    override val animalName: String,
    val food: String,
) : FeedingCommand

data class DogFeedingCommand(
    override val animalName: String,
    val food: String,
) : FeedingCommand
