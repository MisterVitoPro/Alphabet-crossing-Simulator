import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    val numOfPlayer = 5
    val numOfGames = 2000
    val aggregated = mutableListOf<Results>()

    (0 until numOfGames).forEach { _ ->
        val r = Game(numOfPlayer).run()
        aggregated.add(r)
    }

    val averageTurns = aggregated.map { it.turns }.average()
    val averageSpaces = aggregated.map { it.playerMoves.average() }.average()
    val playerWins = aggregated.groupingBy { it.winningPlayer!!.id }.eachCount().toSortedMap()

    logger.info { "Games Played: ${aggregated.size}." }
    for (w in playerWins) {
        logger.info { "${w.key} (${w.value} wins) - ${aggregated[0].players.first { it.id == w.key }.aiDifficulty}." }
    }
    logger.info { "Average Spaces Moved: ${String.format("%.2f", averageSpaces)}." }
    logger.info { "Average Number of Turns: ${String.format("%.2f", averageTurns)}." }
    logger.info { "Shortest Game: ${aggregated.map { it.turns }.minByOrNull { it }}." }
    logger.info { "Longest Game: ${aggregated.map { it.turns }.maxByOrNull { it }}." }
    logger.info { "Average Game Time: ${String.format("%.2f", (averageTurns * 18 / 60))} minutes." }
}

