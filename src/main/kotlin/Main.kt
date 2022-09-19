fun main(args: Array<String>) {
    val numOfPlayer = 5
    val numOfGames = 5000
    val aggregated = mutableListOf<Results>()

    (0 until numOfGames).forEach { _ ->
        val r = Game(numOfPlayer).run()
        aggregated.add(r)
    }

    val averageTurns = aggregated.map { it.turns }.average()
    val averageSpaces = aggregated.map { it.playerMoves.average() }.average()
    val playerWins = aggregated.groupingBy { it.winningPlayer!!.id }.eachCount().toSortedMap()

    println("Games Played: ${aggregated.size}.")
    for (w in playerWins) {
        println("${w.key} (${w.value} wins) - ${aggregated[0].players.first { it.id == w.key }.aiPlayerAsking}, ${aggregated[0].players.first { it.id == w.key }.aiLetterSelecting}.")
    }
    println("Average Spaces Moved: ${String.format("%.2f", averageSpaces)}.")
    println("Average Number of Turns: ${String.format("%.2f", averageTurns)}.")
    println("Shortest Game: ${aggregated.map { it.turns }.minByOrNull { it }}.")
    println("Longest Game: ${aggregated.map { it.turns }.maxByOrNull { it }}.")
    println("Average Game Time: ${String.format("%.2f", (averageTurns * 20 / 60))} minutes.")
}

/**
Games Played: 500.
AI Type: RANDOM.
Average Spaces Moved: 3.56.
Average Number of Turns: 40.81.
Shortest Game: 13.
Longest Game: 103.
Average Game Time: 13.60 minutes.
 */

/**
Games Played: 500.
AI Type: LARGEST_HAND.
Average Spaces Moved: 3.58.
Average Number of Turns: 41.54.
Shortest Game: 10.
Longest Game: 111.
Average Game Time: 13.85 minutes.
 */

/**
Games Played: 2000.
0 (685 wins) - REMEMBERS_PREVIOUS_ASKS, RANDOM.
1 (534 wins) - LARGEST_HAND, RANDOM.
3 (388 wins) - RANDOM, RANDOM.
4 (393 wins) - RANDOM, RANDOM.
Average Spaces Moved: 3.83.
Average Number of Turns: 30.21.
Shortest Game: 3.
Longest Game: 67.
Average Game Time: 10.07 minutes.
 */

