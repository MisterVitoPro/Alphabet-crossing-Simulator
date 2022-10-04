import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    print("Set Number of Players (2-5): ")
    val numOfPlayer = Integer.valueOf(readLine())
    println("Number of Players set to: $numOfPlayer")

    print("Set Number of Games to Simulate: ")
    val numOfGames = Integer.valueOf(readLine())
    println("Number of Games set to: $numOfGames")

    val aggregated = mutableListOf<Results>()

    (0 until numOfGames).forEach { _ ->
        val r = Game(numOfPlayer).run()
        aggregated.add(r)
    }

    val averageTurns = aggregated.map { it.turns }.average()
    val averageSpaces = aggregated.map { it.playerMoves.average() }.average()
    val playerWins = aggregated.groupingBy { it.winningPlayer.id }.eachCount().toSortedMap()
    val numberOfHopCardsPlayed = aggregated.groupingBy { it.winningPlayerHopCardsPlayed }.eachCount().toSortedMap().map { "${it.key} - ${(it.value.toDouble() / numOfGames) * 100.00}%" }
    val winsWithHopCards = aggregated.map { it.winningPlayerHopCardsPlayed }.average()

    logger.info { "Games Played: ${aggregated.size}" }
    for (w in playerWins) {
        logger.info { "${w.key} (${w.value} wins) - ${aggregated[0].players.first { it.id == w.key }.aiDifficulty}" }
    }
    logger.info { "Average Spaces Moved: ${String.format("%.2f", averageSpaces)}" }
    logger.info { "Average Number of Turns: ${String.format("%.2f", averageTurns)}" }
    logger.info { "Average Number of Hop Cards Played for Winner: ${String.format("%.2f", winsWithHopCards)}" }
    logger.info { "Win Percentage Based on Number Hop Cards Played: $numberOfHopCardsPlayed" }
    logger.info { "Shortest Game: ${aggregated.map { it.turns }.minByOrNull { it }}" }
    logger.info { "Longest Game: ${aggregated.map { it.turns }.maxByOrNull { it }}" }
    logger.info { "Average Game Time: ${String.format("%.2f", (averageTurns * 18 / 60))} minutes." }
    logger.info { "Average Game Time: ${String.format("%.2f", (averageTurns * 18 / 60))} minutes." }
}
