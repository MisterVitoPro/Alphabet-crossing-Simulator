import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class Card(val letter: Char, val isHopForward: Boolean = false)

data class Results(
    val turns: Int,
    val deckSize: Int,
    val winningPlayer: Player?,
    val playerMoves: List<Int>,
    val players: List<Player>
)

/**
 * Creates a Game with n players
 */
class Game(private val numOfPlayers: Int) {

    private val players: MutableList<Player> = mutableListOf()
    private val cardList: MutableList<Card> = createCardList()
    private val deck: MutableList<Card> = mutableListOf()
    var winner: Player? = null
    private val currentTurn = AtomicInteger()
    private lateinit var currentPlayer: Player
    var previouslyAskedChars: HashMap<Char, Player> = HashMap()

    /**
     * Set up the Game and players with cards and establish any starter matches
     */
    private fun setup() {
        if (numOfPlayers < 2) throw Exception("numOfPlayers must be greater than or equal to 2")

        val startingHand: Int = if (numOfPlayers > 3) 5 else 7

        deck.addAll(cardList.filter { !it.isHopForward })
        deck.shuffle()

        (0 until numOfPlayers).forEach {
            val p = setupNewPlayer(it, startingHand, AIDifficulty.values()[it%AIDifficulty.values().size])
            players.add(p)
        }

        deck.addAll(cardList.filter { it.isHopForward })
        deck.shuffle()

        nextTurn()
    }

    private fun setupNewPlayer(id: Int, startingHand: Int, aiDifficulty: AIDifficulty): Player {
        val p = Player(id, aiDifficulty)
        (0 until startingHand).forEach { _ ->
            draw(p, false)
        }
        p.printHand()
        return p
    }

    fun run(): Results {
        println("########## Starting new Game ##########")
        setup()

        while (winner == null && currentTurn.get() < 150) {
            // Ask for Card
            val askedCard: Pair<Char, Player> = currentPlayer.callForCard(players, previouslyAskedChars)
            val hadMatch = askPlayerAndCheckMatches(askedCard)

            // Check if players need to draw cards
            val askedPlayer: Player = askedCard.second
            if (shouldDrawCard(askedPlayer)) {
                draw(askedPlayer)
            }

            handSizeAndDeckCheck(currentPlayer)
            winnerCheck()
            // Check if a player needs to be eliminated
            players.forEach { eliminatePlayer(it) }

            if (winner == null && (!hadMatch || currentPlayer.isEliminated)) {
                nextTurn()
            }
        }

        logger.info { "!!!!!! Player ${winner!!.id} Won in $currentTurn Turns (${winner!!.aiDifficulty}) !!!!!!" }
        return Results(currentTurn.get(), deck.size, winner, players.map { it.spacesMoved }, players)
    }

    private fun askPlayerAndCheckMatches(playerCardAsk: Pair<Char, Player>): Boolean {
        val askedPlayer = playerCardAsk.second
        val char = playerCardAsk.first
        val card: Card? = askedPlayer.hasLetterCard(char)
        return if (card != null) {
            logger.debug { "Player ${askedPlayer.id}: \"Yes I do!\"" }
            askedPlayer.playCard(card)
            currentPlayer.gainCard(card)
            checkForMatches(currentPlayer)
            previouslyAskedChars.remove(char)

            currentPlayer.removeAskedCardsFromList(char)
            askedPlayer.removeAskedCardsFromList(char)
            true
        } else {
            if (doesDeckHaveCards()) draw()
            previouslyAskedChars[char] = currentPlayer
            false
        }
    }

    private fun nextTurn() {
        val activePlayers = players.filter { !it.isEliminated }
        currentPlayer = activePlayers[currentTurn.incrementAndGet() % activePlayers.size]
        logger.debug { "=== Turn $currentTurn with Player ${currentPlayer.id} (${currentPlayer.spacesMoved}) ===" }
    }

    private fun draw(p: Player = currentPlayer, log: Boolean = true): Card {
        val c = deck.removeFirst()
        p.gainCard(c)
        if (log) logger.debug { "Player ${p.id} drew a ${if (c.isHopForward) "Hop Forward" else "'${c.letter}'"}." }
        hopForwardCheck(p)
        checkForMatches(p)
        return c
    }

    private fun checkForMatches(p: Player) {
        val matches = p.getMatches()
        if (matches.isNotEmpty()) {
            matches.keys.forEach {
                logger.info { "Player ${p.id} has matching '${it.letter}'" }
                p.playCard(it)
                p.playCard(it)
                p.moveSpace()
                p.removeAskedCardsFromList(it.letter)
            }
            handSizeAndDeckCheck(p)
        }
    }

    private fun shouldDrawCard(p: Player): Boolean {
        return p.handSize() == 0 && doesDeckHaveCards()
    }

    private fun canEliminate(p: Player): Boolean {
        return p.handSize() == 0 && !doesDeckHaveCards()
    }

    private fun eliminatePlayer(p: Player): Boolean {
        return if (canEliminate((p)) && !p.isEliminated) {
            logger.debug { "## Player ${p.id} cannot win and must be eliminated." }
            p.eliminate()
            true
        } else false
    }

    private fun handSizeAndDeckCheck(p: Player): Boolean {
        return if (shouldDrawCard(p)) {
            draw(p)
            true
        } else !(p.handSize() == 0 && !doesDeckHaveCards())
    }

    private fun hopForwardCheck(p: Player) {
        val hopCard: Card? = p.hasHopCard()
        if (hopCard != null) {
            logger.debug { "Player ${p.id} played a Hop Forward." }
            p.playCard(hopCard)
            p.moveSpace()
            if (shouldDrawCard(p)) draw(p)
        }
    }

    private fun winnerCheck() {
        for (p in players) {
            if (p.spacesMoved >= SPACES_TO_WIN) {
                winner = p
                break;
            }
        }
    }

    private fun doesDeckHaveCards(): Boolean {
        return deck.size > 0
    }

    companion object {
        const val SPACES_TO_WIN = 6
    }
}

fun createCardList(): MutableList<Card> {
    val l = mutableListOf<Card>()
    var c = 'a'
    while (c <= 'z') {
        l.add(Card(c))
        l.add(Card(c))
        ++c
    }
    // Add 4 Hop Forward Cards to the deck
    (0..3).forEach { _ ->
        l.add(Card(' ', true))
    }
    return l
}