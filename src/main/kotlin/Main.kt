import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

fun main() {
    print("Set Number of Players (2-5): ")
    val numOfPlayer = Integer.valueOf(readLine())
    println("Number of Players set to: $numOfPlayer")

    print("Human Playing (y/n): ")
    val humanPlaying: Boolean = readLine().equals("y")

    val numOfGames = if (!humanPlaying) {
        print("Set Number of Games to Simulate: ")
        Integer.valueOf(readLine())
    } else {
        1
    }
    println("Number of Games set to: $numOfGames")
    val aggregated: MutableList<Results> = mutableListOf<Results>()

    (0 until numOfGames).forEach { _ ->
        val r = Game(numOfPlayer, humanPlaying).run()
        aggregated.add(r)
    }

    val averageTurns = aggregated.map { it.turns }.average()
    val averageSpaces = aggregated.map { it.playerMoves.average() }.average()
    val playerWins = aggregated.groupingBy { it.winningPlayer.id }.eachCount().toSortedMap()
    val winsWithHopCards = aggregated.map { it.winningPlayerHopCardsPlayed }.average()


    logger.info { "--------------- SIMULATION COMPLETE ---------------" }
    logger.info { "Games Played: ${aggregated.size}" }

    if (!humanPlaying) {
        for (w in playerWins) {
            logger.info { "${w.key} (${w.value} wins) - ${(aggregated[0].players.first { it.id == w.key } as AIPlayer).aiDifficulty}" }
        }
    }
    logger.info { "Average Spaces Moved: ${String.format("%.2f", averageSpaces)}" }
    logger.info { "Average Number of Turns: ${String.format("%.2f", averageTurns)}" }
    logger.info { "Average Number of Hop Cards Played by Winner: ${String.format("%.2f", winsWithHopCards)}" }
    logger.info { "Shortest Game: ${aggregated.map { it.turns }.minByOrNull { it }}" }
    logger.info { "Longest Game: ${aggregated.map { it.turns }.maxByOrNull { it }}" }
    logger.info { "Average Game Time: ${String.format("%.2f", (averageTurns * 18 / 60))} minutes." }
    winPercentageLikelihoodWhenPlayHopCard(aggregated)
}

fun winPercentageLikelihoodWhenPlayHopCard(aggregated: MutableList<Results>) {
    val winsWithHopCardPlayed =
        aggregated.map { it.winningPlayerHopCardsPlayed }.groupingBy { it }.eachCount().toSortedMap()
    val lossesWithHopCardPlayed =
        aggregated.map { r -> r.players.filter { it != r.winningPlayer }.map { it.hopCardsPlayed } }.flatten()
            .groupingBy { it }.eachCount().toSortedMap()
    for (n in winsWithHopCardPlayed) {
        if (lossesWithHopCardPlayed[n.key] != null) {
            logger.info {
                "Playing ${n.key} Hop Cards: ${
                    String.format(
                        "%.2f",
                        min((n.value.toDouble() / lossesWithHopCardPlayed[n.key]!!) * 100.00, 100.00)
                    )
                }% you are likely to win."
            }
        }
    }

}