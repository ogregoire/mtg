package be.imgn.mtg.engine.game;

import java.util.List;

import com.google.inject.Guice;

import be.imgn.mtg.engine.card.CardFetcher;
import be.imgn.mtg.engine.card.internal.CardModule;
import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.internal.GameConfigurationModule;
import be.imgn.mtg.engine.game.internal.GameModule;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

/// Test utility that wraps a real Guice-backed game for writing card interaction scenarios.
///
/// Usage:
/// ```java
/// var game = TestGame.create();
/// var permanent = game.createPermanent("Grizzly Bears");
/// game.advanceTo(Phase.MAIN);
/// ```
public final class TestGame {

    private final Game game;
    private final GameState gameState;
    private final TurnTracker turnTracker;
    private final CardFetcher cardFetcher;

    private TestGame(Game game, GameState gameState, TurnTracker turnTracker, CardFetcher cardFetcher) {
        this.game = game;
        this.gameState = gameState;
        this.turnTracker = turnTracker;
        this.cardFetcher = cardFetcher;
    }

    /// Creates a TestGame with 2 players.
    public static TestGame create() {
        var team1 = new TestTeam("team1");
        var team2 = new TestTeam("team2");
        var playersData = List.of(
                new PlayerData(new TestPlayerId("player1"), team1), new PlayerData(new TestPlayerId("player2"), team2));

        var format = new TestFormat();
        var parentInjector = Guice.createInjector(new GameModule(), new CardModule());
        var gameInjector = parentInjector.createChildInjector(new GameConfigurationModule(format, playersData));

        var game = gameInjector.getInstance(Game.class);
        var gameState = gameInjector.getInstance(GameState.class);
        var turnTracker = gameInjector.getInstance(TurnTracker.class);
        var cardFetcher = parentInjector.getInstance(CardFetcher.class);

        var testGame = new TestGame(game, gameState, turnTracker, cardFetcher);

        // Start the game with player1 as the starting player
        turnTracker.startGame(testGame.player1());

        return testGame;
    }

    /// Returns player 1.
    public Player player1() {
        return game.players().get(0);
    }

    /// Returns player 2.
    public Player player2() {
        return game.players().get(1);
    }

    /// Returns the game state.
    public GameState gameState() {
        return gameState;
    }

    /// Returns the battlefield.
    public Battlefield battlefield() {
        return gameState.battlefield();
    }

    /// Returns the stack.
    public Stack stack() {
        return gameState.stack();
    }

    /// Creates a permanent on the battlefield under player 1's control.
    ///
    /// @param cardName the card name to fetch from the database
    /// @return the created permanent
    public Permanent createPermanent(String cardName) {
        return createPermanent(cardName, player1());
    }

    /// Creates a permanent on the battlefield under the specified controller.
    ///
    /// @param cardName the card name to fetch from the database
    /// @param controller the player who controls the permanent
    /// @return the created permanent
    public Permanent createPermanent(String cardName, Player controller) {
        var card = fetchCard(cardName, controller);
        return battlefield().enter(card, controller, ZoneType.HAND);
    }

    /// Adds a card to player 1's hand.
    ///
    /// @param cardName the card name to fetch from the database
    /// @return the created card
    public Card addToHand(String cardName) {
        return addToHand(cardName, player1());
    }

    /// Adds a card to the specified player's hand.
    ///
    /// @param cardName the card name to fetch from the database
    /// @param player the player whose hand to add the card to
    /// @return the created card
    public Card addToHand(String cardName, Player player) {
        var card = fetchCard(cardName, player);
        gameState.hand(player).add(card);
        return card;
    }

    /// Adds a card to player 1's graveyard.
    ///
    /// @param cardName the card name to fetch from the database
    /// @return the created card
    public Card addToGraveyard(String cardName) {
        return addToGraveyard(cardName, player1());
    }

    /// Adds a card to the specified player's graveyard.
    ///
    /// @param cardName the card name to fetch from the database
    /// @param player the player whose graveyard to add the card to
    /// @return the created card
    public Card addToGraveyard(String cardName, Player player) {
        var card = fetchCard(cardName, player);
        gameState.graveyard(player).put(card);
        return card;
    }

    /// Advances the game to the specified phase by passing priority repeatedly.
    ///
    /// @param targetPhase the phase to advance to
    public void advanceTo(Phase targetPhase) {
        // Safety limit to avoid infinite loops
        for (var i = 0; i < 1000; i++) {
            if (turnTracker.currentPhase() == targetPhase) {
                return;
            }
            passPriority();
        }
        throw new IllegalStateException("Could not reach phase " + targetPhase + " within 1000 priority passes");
    }

    /// Passes priority for the player who currently has it.
    public void passPriority() {
        var holder = game.players().stream()
                .filter(turnTracker::hasPriority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No player has priority"));
        turnTracker.passPriority(holder);
    }

    private Card fetchCard(String cardName, Player owner) {
        return cardFetcher
                .fetchByName(cardName, owner)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardName));
    }

    record TestPlayerId(String id) implements PlayerId {}

    record TestTeam(String name) implements Team {}

    /// A permissive format that accepts any number of players.
    private static final class TestFormat implements Format {
        @Override
        public void checkPlayers(List<PlayerData> playersData) {
            // Accept any configuration
        }
    }
}
