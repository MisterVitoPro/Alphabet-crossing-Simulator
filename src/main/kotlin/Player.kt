import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

private val logger = KotlinLogging.logger {}

open class Player(val id: Int) {

    protected val hand: MutableList<Card> = mutableListOf()
    protected val myAskedCards: MutableList<Pair<Char, Player>> = mutableListOf()
    var hopCardsPlayed: Int = 0
        private set

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

    fun incrementHopCardsPlayed(){
        this.hopCardsPlayed += 1
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

    protected fun createPairWithPrint(ch: Char, p: Player): Pair<Char, Player> {
        val pair = Pair(ch, p)
        this.printHand()
        logger.debug { "Player ${this.id} asks, \"Player ${pair.second.id}, do you have a(n) '${pair.first}'\"" }
        pair.second.printHand()
        myAskedCards.add(pair)
        return pair
    }

    fun removeAskedCardsFromList(askedChar: Char) {
        myAskedCards.removeAll { it.first == askedChar }
    }

    protected fun getAvailableAsks(opponents: List<Player>): List<Pair<Char, Player>> {
        val l = mutableListOf<Pair<Char, Player>>()
        hand.map { it.letter }.forEach { char ->
            opponents.forEach { p ->
                l.add(Pair(char, p))
            }
        }
        return l
    }

}
