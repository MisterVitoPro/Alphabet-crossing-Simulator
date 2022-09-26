import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

private val logger = KotlinLogging.logger {}

class Player(val id: Int, val aiDifficulty: AIDifficulty) {

    private val hand: MutableList<Card> = mutableListOf()
    private val myAskedCards: MutableList<Pair<Char, Player>> = mutableListOf()

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

    private fun createPairWithPrint(ch: Char, p: Player): Pair<Char, Player> {
        val pair = Pair(ch, p)
        this.printHand()
        logger.debug { "Player ${this.id} asks, \"Player ${pair.second.id}, do you have a(n) '${pair.first}'\"" }
        pair.second.printHand()
        myAskedCards.add(pair)
        return pair
    }

    fun removeAskedCardsFromList(askedChar: Char){
        myAskedCards.removeAll { it.first == askedChar}
    }

    private fun getAvailableAsks(opponents: List<Player>): List<Pair<Char, Player>>{
        val l = mutableListOf<Pair<Char, Player>>()
        hand.map { it.letter }.forEach { char ->
            opponents.forEach { p ->
                l.add(Pair(char, p))
            }
        }
        return l
    }

    fun callForCard(players: List<Player>, previouslyAskedChars: HashMap<Char, Player>): Pair<Char, Player> {
        val opponents: List<Player> = players.filter { it.id != this.id }

        if (aiDifficulty != AIDifficulty.EASY) {
            if (aiDifficulty != AIDifficulty.MEDIUM && previouslyAskedChars.size > 0) {
                logger.debug { "Global Previously Asked: ${previouslyAskedChars.mapValues { "Player ${it.value.id}" }}" }
                val handCharsInPreviouslyAsked: Char? = hand.map { it.letter }.firstOrNull { previouslyAskedChars.keys.contains(it) }
                if (handCharsInPreviouslyAsked != null && previouslyAskedChars[handCharsInPreviouslyAsked] != this) {
                    return createPairWithPrint(handCharsInPreviouslyAsked, previouslyAskedChars[handCharsInPreviouslyAsked]!!)
                }
            }
            if(myAskedCards.size > 0) {
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

        //Determine which player to ask for a card
        val pIndex = ThreadLocalRandom.current().nextInt(0, opponents.size)
        val p = opponents[pIndex]
        val askLetter: Char = hand.map { it.letter }.random()
        return createPairWithPrint(askLetter, p)
    }

}

enum class AIDifficulty {
    EASY, // Ask a random player
    MEDIUM,
    EXPERT
}
