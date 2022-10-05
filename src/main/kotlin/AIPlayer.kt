import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

private val logger = KotlinLogging.logger {}

enum class AIDifficulty {
    EASY, // Ask a random player
    MEDIUM,
    EXPERT
}

class AIPlayer(id: Int, val aiDifficulty: AIDifficulty) : Player(id) {

    fun callForCard(players: List<Player>, previouslyAskedChars: HashMap<Char, Player>): Pair<Char, Player> {
        val opponents: List<Player> = players.filter { it.id != this.id }

        if (aiDifficulty != AIDifficulty.EASY) {
            if (aiDifficulty != AIDifficulty.MEDIUM && previouslyAskedChars.size > 0) {
                logger.debug { "Global Previously Asked: ${previouslyAskedChars.mapValues { "Player ${it.value.id}" }}" }
                // Check if we have any letters that have been asked by other players
                val handCharsInPreviouslyAsked: Char? =
                    hand.map { it.letter }.firstOrNull { previouslyAskedChars.keys.contains(it) }
                // If we have a letter that another player has asked, lets ask that player for the letter
                if (handCharsInPreviouslyAsked != null && previouslyAskedChars[handCharsInPreviouslyAsked] != this) {
                    return createPairWithPrint(
                        handCharsInPreviouslyAsked,
                        previouslyAskedChars[handCharsInPreviouslyAsked]!!
                    )
                }
            }
            // We want to ask a letter from a player that we have not already asked
            if (myAskedCards.size > 0) {
                logger.debug { "My previously Asked: ${myAskedCards.map { "${it.first}=Player ${it.second.id}" }}" }
                val allAsks = getAvailableAsks(opponents)
                val availableAsks = allAsks.filter { !myAskedCards.contains(it) }
                if (availableAsks.isNotEmpty()) {
                    val myPair = availableAsks.random()
                    return createPairWithPrint(myPair.first, myPair.second)
                }
            }
            val p = opponents.maxByOrNull { it.handSize() }!!
            val pI = opponents.indexOf(p)
            return createPairWithPrint(hand.map { it.letter }.random(), opponents[pI])
        }

        // Determine which player to ask for a card
        val pIndex = ThreadLocalRandom.current().nextInt(0, opponents.size)
        val p = opponents[pIndex]
        val askLetter: Char = hand.map { it.letter }.random()
        return createPairWithPrint(askLetter, p)
    }

}