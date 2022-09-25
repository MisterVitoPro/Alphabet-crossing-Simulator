import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

private val logger = KotlinLogging.logger {}

class Player(val id: Int, val aiDifficulty: AIDifficulty) {

    private val hand: MutableList<Card> = mutableListOf()

    var spacesMoved = 0
        private set
    var isEliminated = false
        private set

    fun getMatches(): Map<Card, Int> {
        return this.hand.groupingBy { it }.eachCount().filter { it.value > 1 }
    }

    fun eliminate() {
        isEliminated = true
    }

    fun hasHopCard(): Card? {
        return this.hand.firstOrNull { it.isHopForward }
    }

    fun hasLetterCard(char: Char): Card? {
        return hand.firstOrNull { it.letter == char }
    }

    fun handSize(): Int {
        return this.hand.size
    }

    fun printHand() {
        logger.debug { "Player $id: ${hand.map { it.letter }}" }
    }

    fun gainCard(c: Card) {
        hand.add(c)
    }

    fun playCard(c: Card) {
        hand.remove(c)
    }

    fun moveSpace() {
        spacesMoved += 1
        logger.debug { "Player $id moves. ($spacesMoved)" }
    }

    private fun printHandAndAsk(playerAndChar: Pair<Player, Char>) {
        this.printHand()
        logger.debug { "Player ${this.id} asks, \"Player ${playerAndChar.first.id}, do you have a(n) '${playerAndChar.second}'\"" }
        playerAndChar.first.printHand()
    }

    fun callForCard(players: List<Player>, previouslyAskedChars: HashMap<Char, Player>): Pair<Player, Char> {
        val playersWithThisPlayer = players.filter { it.id != this.id }

        if (aiDifficulty == AIDifficulty.EXPERT) {
            if (previouslyAskedChars.size > 0) {
                logger.debug { "Previously Asked: ${previouslyAskedChars.mapValues { "Player ${it.value.id}" }}" }
                val handCharsInPreviouslyAsked: Char? =
                    hand.map { it.letter }.firstOrNull { previouslyAskedChars.keys.contains(it) }
                if (handCharsInPreviouslyAsked != null && previouslyAskedChars[handCharsInPreviouslyAsked] != this) {
                    val pair = Pair(previouslyAskedChars[handCharsInPreviouslyAsked]!!, handCharsInPreviouslyAsked)
                    printHandAndAsk(pair)
                    return pair
                }
            } else {
                val p = playersWithThisPlayer.maxByOrNull { it.handSize() }!!
                val pI = playersWithThisPlayer.indexOf(p)
                return Pair(playersWithThisPlayer[pI], hand.map { it.letter }.random())
            }
        }

        //Determine which player to ask for a card
        val pIndex = ThreadLocalRandom.current().nextInt(0, playersWithThisPlayer.size)
        val p = playersWithThisPlayer[pIndex]
        val askLetter: Char = hand.map { it.letter }.random()
        val pair = Pair(p, askLetter)
        printHandAndAsk(pair)
        return pair
    }

}

enum class AIDifficulty {
    EASY, // Ask a random player
    EXPERT
}