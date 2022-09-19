import java.util.concurrent.ThreadLocalRandom

class Player(val id: Int, val aiPlayerAsking: AIPlayerAsking, val aiLetterSelecting: AILetterSelecting) {

    private val hand: MutableList<Card> = mutableListOf()
    private val previouslyAsked: MutableList<Char> = mutableListOf()

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
        println("Player $id: ${hand.map { it.letter }}")
    }

    fun gainCard(c: Card) {
        hand.add(c)
    }

    fun playCard(c: Card) {
        hand.remove(c)
    }

    fun moveSpace() {
        spacesMoved += 1
        println("Player $id moves. ($spacesMoved)")
    }

    private fun printHandAndAsk(playerAndChar: Pair<Player, Char>) {
        this.printHand()
        println("Player ${this.id} asks, \"Player ${playerAndChar.first.id}, do you have a(n) '${playerAndChar.second}'\"")
        playerAndChar.first.printHand()
    }

    fun callForCard(players: List<Player>, previouslyAskedChars: HashMap<Char, Player>): Pair<Player, Char> {
        val playersWithThisPlayer = players.filter { it.id != this.id }

        if (aiPlayerAsking == AIPlayerAsking.REMEMBERS_PREVIOUS_ASKS && previouslyAskedChars.size > 0) {
            println("Previously Asked: ${previouslyAskedChars.mapValues { "Player ${it.value.id}"}}")
            val handCharsInPreviouslyAsked: Char? =
                hand.map { it.letter }.firstOrNull { previouslyAskedChars.keys.contains(it) }
            if (handCharsInPreviouslyAsked != null && previouslyAskedChars[handCharsInPreviouslyAsked] != this) {
                val pair = Pair(previouslyAskedChars[handCharsInPreviouslyAsked]!!, handCharsInPreviouslyAsked)
                printHandAndAsk(pair)
                return pair
            }
        }

        //Determine which player to ask for a card
        val pIndex = when (aiPlayerAsking) {
            AIPlayerAsking.LARGEST_HAND -> {
                val player = playersWithThisPlayer.maxByOrNull { it.handSize() }!!
                playersWithThisPlayer.indexOf(player)
            }

            else -> ThreadLocalRandom.current().nextInt(0, playersWithThisPlayer.size)
        }
        val p = playersWithThisPlayer[pIndex]

        //Determine which letter to ask for
        val handChars = hand.map { it.letter }

        val askLetter: Char = when (aiLetterSelecting) {
            AILetterSelecting.PREVIOUSLY_ASKED_LETTER -> {
                val nonAsked = handChars.filter { !previouslyAsked.contains(it) }
                val c: Char = if (nonAsked.isEmpty()) {
                    previouslyAsked.clear()
                    println("Reset Previously Asked Letters.")
                    handChars.random()
                } else {
                    println("Previously Asked: $previouslyAsked")
                    nonAsked.random()
                }
                previouslyAsked.add(c)
                c
            }

            else -> handChars.random()
        }

        val pair = Pair(p, askLetter)
        printHandAndAsk(pair)
        return pair
    }

}

enum class AIPlayerAsking {
    RANDOM, // Ask a random player
    LARGEST_HAND, //Ask a player with the largest hand
    REMEMBERS_PREVIOUS_ASKS
}

enum class AILetterSelecting {
    RANDOM, // Ask for random letter from hand
    PREVIOUSLY_ASKED_LETTER //Asks only for cards it has not asked before
}