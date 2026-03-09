package be.imgn.mtg.engine.game;

import java.util.List;

import com.google.inject.Guice;

import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.ability.SpellAbility;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.card.CardFetcher;
import be.imgn.mtg.engine.card.internal.CardModule;
import be.imgn.mtg.engine.effect.CounterSpellEffect;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.effect.ExileEffect;
import be.imgn.mtg.engine.effect.TapEffect;
import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.internal.GameConfigurationModule;
import be.imgn.mtg.engine.game.internal.GameModule;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.Mana;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.spell.TargetChoice;
import be.imgn.mtg.engine.spell.TargetChoices;
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
    private final AbilityManager abilityManager;
    private final ActionExecutor actionExecutor;

    private TestGame() {
        var team1 = new TestTeam("team1");
        var team2 = new TestTeam("team2");
        var playersData = List.of(
                new PlayerData(new TestPlayerId("player1"), team1), new PlayerData(new TestPlayerId("player2"), team2));

        var format = new TestFormat();
        var parentInjector = Guice.createInjector(new GameModule(), new CardModule());
        var gameInjector = parentInjector.createChildInjector(new GameConfigurationModule(format, playersData));

        this.game = gameInjector.getInstance(Game.class);
        this.gameState = gameInjector.getInstance(GameState.class);
        this.turnTracker = gameInjector.getInstance(TurnTracker.class);
        this.cardFetcher = parentInjector.getInstance(CardFetcher.class);
        this.abilityManager = gameInjector.getInstance(AbilityManager.class);
        this.actionExecutor = gameInjector.getInstance(ActionExecutor.class);

        turnTracker.startGame(player1());
    }

    /// Creates a TestGame with 2 players.
    public static TestGame create() {
        return new TestGame();
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

    /// Casts a spell from a player's hand, targeting the specified target.
    ///
    /// The card must be in the caster's hand. The method finds the targeting subject
    /// from the spell's effects and constructs the appropriate [SpellContext].
    /// Mana cost is paid from the caster's mana pool.
    ///
    /// @param card the card to cast (must be in the caster's hand)
    /// @param caster the player casting the spell
    /// @param target the target for the spell's effect
    /// @return the execution result
    public ExecutionResult castSpell(Card card, Player caster, Selectable target) {
        if (!gameState.hand(caster).contains(card)) {
            throw new IllegalStateException(card.name() + " is not in " + caster + "'s hand");
        }

        // Find the targeting subject from the spell ability's effects
        var subject = card.abilities().stream()
                .filter(SpellAbility.class::isInstance)
                .map(SpellAbility.class::cast)
                .flatMap(sa -> sa.effects().stream())
                .map(TestGame::extractSubject)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(card.name() + " has no targetable effect"));

        var choices = new TargetChoices(List.of(new TargetChoice(subject, target)));
        var context = new SpellContext(choices);
        var action = new PlayerAction.CastSpell(caster, card, context);
        return actionExecutor.execute(action, gameState);
    }

    /// Casts a spell from player 1's hand, targeting the specified target.
    public ExecutionResult castSpell(Card card, Selectable target) {
        return castSpell(card, player1(), target);
    }

    /// Casts a spell with a pre-built context (for cards whose oracle text isn't parsed yet).
    ///
    /// @param card the card to cast (must be in the caster's hand)
    /// @param caster the player casting the spell
    /// @param context the spell context with targeting choices
    /// @return the execution result
    public ExecutionResult castSpell(Card card, Player caster, SpellContext context) {
        if (!gameState.hand(caster).contains(card)) {
            throw new IllegalStateException(card.name() + " is not in " + caster + "'s hand");
        }
        var action = new PlayerAction.CastSpell(caster, card, context);
        return actionExecutor.execute(action, gameState);
    }

    private static Subject extractSubject(Effect effect) {
        return switch (effect) {
            case DealDamageEffect e -> e.target();
            case DestroyEffect e -> e.subject();
            case ExileEffect e -> e.subject();
            case TapEffect e -> e.subject();
            case CounterSpellEffect e -> e.subject();
            default ->
                throw new IllegalStateException(
                        "Cannot extract subject from: " + effect.getClass().getSimpleName());
        };
    }

    /// Activates a mana ability on a permanent, choosing the specified mana type.
    ///
    /// Finds the first mana ability on the permanent, activates it through the
    /// [AbilityManager], and for effects that require a color choice (e.g. "any color"),
    /// adds the chosen mana type to the controller's pool.
    ///
    /// @param permanent the permanent to activate the mana ability on
    /// @param chosenType the mana type to produce
    /// @return the activation result
    public ActivationResult activateManaAbility(Permanent permanent, ManaType chosenType) {
        var ability = permanent.abilities().stream()
                .filter(ActivatedAbility.class::isInstance)
                .map(ActivatedAbility.class::cast)
                .filter(ActivatedAbility::isManaAbility)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(permanent.name() + " has no mana ability"));

        var controller = permanent.controller();
        var context = new AbilityContext(permanent, controller, gameState);
        var result = abilityManager.activate(ability, permanent, context);

        // For effects that require a choice (Combination, Selection), add the chosen mana
        if (result instanceof ActivationResult.ManaAbilitySuccess
                && ability.effect().addsMana()) {
            if (!(ability.effect() instanceof AddManaEffect.Exact)) {
                controller.manaPool().add(Mana.of(chosenType, permanent));
            }
        }

        return result;
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
