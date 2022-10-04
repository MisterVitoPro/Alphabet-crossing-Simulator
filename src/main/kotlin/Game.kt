import Game.Companion.HOP_CARDS_IN_DECK
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class Card(val letter: Char, val isHopForward: Boolean = false)

data class Results(
    val turns: Int,
    val deckSize: Int,
    val winningPlayer: Player,
    val playerMoves: List<Int>,
    val players: List<Player>,
    val winningPlayerHopCardsPlayed: Int = winningPlayer.hopCardsPlayed
)

/**
 * Creates a Game with n players
 */
class Game(private val numOfPlayers: Int, private val isHumanPlaying: Boolean = false) {

    private val players: MutableList<Player> = mutableListOf()
    private val cardList: List<Card> by lazy {
        createCardList()
    }
    private val deck: MutableList<Card> = mutableListOf()
    private val currentTurn = AtomicInteger()
    private var previouslyAskedChars: HashMap<Char, Player> = HashMap()

    /**
     * Set up the Game and players with cards and establish any starter matches
     */
    private fun setup() {
        if (numOfPlayers < 2) throw Exception("numOfPlayers must be greater than or equal to 2")

        val startingHand: Int = if (numOfPlayers > 3) 5 else 7

        deck.addAll(cardList.filter { !it.isHopForward })
        deck.shuffle()

        (0 until numOfPlayers).forEach {
            val p = if(isHumanPlaying && it == 0){
                setupNewPlayer(it, startingHand, true)
            } else {
                setupNewPlayer(it, startingHand, false, AIDifficulty.values()[it % AIDifficulty.values().size])
            }
            players.add(p)
        }

        deck.addAll(cardList.filter { it.isHopForward })
        deck.shuffle()

        // We need to initialize the Turns
        nextPlayersTurn()
    }

    /**
     * Creates a new Player, sets its difficulty, and creates the players starting hand
     */
    private fun setupNewPlayer(id: Int, startingHand: Int, isHuman: Boolean, aiDifficulty: AIDifficulty = AIDifficulty.EASY): Player {
        val p = if(isHuman) Player(id) else AIPlayer(id, aiDifficulty)
        (0 until startingHand).forEach { _ ->
            drawCard(p, false)
        }
        p.printHand()
        return p
    }

    fun run(): Results {
        println("########## Starting new Game ##########")
        setup()

        var currentPlayer: Player = players[0]
        do {

            val askedCard: Pair<Char, Player> = if(currentPlayer is AIPlayer){
                currentPlayer.callForCard(players, previouslyAskedChars)
            } else {
                currentPlayer.printHand()

                var letter: Char? = null
                do {
                    print("Call Letter: ")
                    val l: Char? = readLine()!!.trim().toCharArray().getOrNull(0)
                    if(l == null){
                        println("Must be a single letter")
                        continue
                    }
                    letter = if(currentPlayer.hasLetterCard(l) != null) l else null
                    if(letter == null){
                        println("Not a valid letter. Valid numbers are [1-$numOfPlayers]")
                    }
                }while(letter==null)

                var player: Player?
                do {
                    print("Call Player by Id (1-4): ")
                    val id = Integer.valueOf(readLine())
                    player = players.firstOrNull() { it.id == id && it.id != 0}
                    if(player == null){
                        println("Not a valid number. Valid numbers are [1-$numOfPlayers]")
                    }
                }while(player==null)



                Pair(letter, player)
            }
            // Ask for Card

            val hadMatch = askPlayerAndCheckMatches(currentPlayer, askedCard)

            // Check if players need to draw cards
            val askedPlayer: Player = askedCard.second
            if (shouldDrawCard(askedPlayer)) {
                drawCard(askedPlayer)
            }
            handSizeAndDeckCheck(currentPlayer)

            // Check for a Winner
            if (hasWinner()) {
                logger.info { "!!!!!! Player ${currentPlayer.id} Won in $currentTurn Turns !!!!!!" }
                break
            }

            // Check if a player needs to be eliminated
            players.forEach { eliminate(it) }

            // Check if Player needs to go again or Next Turn
            if (!hadMatch || currentPlayer.isEliminated) {
                currentPlayer = nextPlayersTurn()
            }
        } while (currentTurn.get() < 150)

        return Results(currentTurn.get(), deck.size, currentPlayer, players.map { it.spacesMoved }, players)
    }

    private fun askPlayerAndCheckMatches(askingPlayer:  Player, playerCardAsk: Pair<Char, Player>): Boolean {
        val askedPlayer = playerCardAsk.second
        val char = playerCardAsk.first
        val card: Card? = askedPlayer.hasLetterCard(char)
        return if (card != null) {
            logger.debug { "Player ${askedPlayer.id}: \"Yes I do!\"" }
            askedPlayer.playCard(card)
            askingPlayer.gainCard(card)
            checkForMatches(askingPlayer)
            previouslyAskedChars.remove(char)

            askingPlayer.removeAskedCardsFromList(char)
            askedPlayer.removeAskedCardsFromList(char)
            true
        } else {
            if (doesDeckHaveCards()) drawCard(askingPlayer)
            previouslyAskedChars[char] = askingPlayer
            false
        }
    }

    private fun nextPlayersTurn(): Player {
        val activePlayers = players.filter { !it.isEliminated }
        val nextPlayer = activePlayers[currentTurn.incrementAndGet() % activePlayers.size]
        logger.debug { "=== Turn $currentTurn with Player ${nextPlayer.id} (${nextPlayer.spacesMoved}) ===" }
        return nextPlayer
    }

    private fun drawCard(p: Player, log: Boolean = true): Card {
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

    private fun eliminate(p: Player): Boolean {
        return if (canEliminate((p)) && !p.isEliminated) {
            logger.debug { "## Player ${p.id} cannot win and must be eliminated." }
            p.eliminate()
            true
        } else false
    }

    private fun handSizeAndDeckCheck(p: Player): Boolean {
        return if (shouldDrawCard(p)) {
            drawCard(p)
            true
        } else !(p.handSize() == 0 && !doesDeckHaveCards())
    }

    private fun hopForwardCheck(p: Player) {
        val hopCard: Card? = p.hasHopCard()
        if (hopCard != null) {
            logger.debug { "Player ${p.id} played a Hop Forward." }
            p.playCard(hopCard)
            p.moveSpace()
            p.incrementHopCardsPlayed()
            if (shouldDrawCard(p)) drawCard(p)
        }
    }

    private fun hasWinner(): Boolean {
        for (p in players) {
            if (p.spacesMoved >= SPACES_TO_WIN) {
                return true
            }
        }
        return false
    }

    private fun doesDeckHaveCards(): Boolean {
        return deck.size > 0
    }

    companion object {
        const val SPACES_TO_WIN = 6
        const val HOP_CARDS_IN_DECK = 4
    }
}

fun createCardList(): List<Card> {
    val l = mutableListOf<Card>()
    var c = 'a'
    while (c <= 'z') {
        l.add(Card(c))
        l.add(Card(c))
        ++c
    }
    // Add 4 Hop Forward Cards to the deck
    (0 until HOP_CARDS_IN_DECK).forEach { _ ->
        l.add(Card(' ', true))
    }
    return l
}
