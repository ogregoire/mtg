package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.LandPlayedEvent;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.action.ValidationResult.ValidationError;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Stack;

@DisplayName("DefaultActionValidator")
class DefaultActionValidatorTest {

    private TurnTracker turnTracker;
    private AbilityManager abilityManager;
    private EventTracker eventTracker;
    private GameState gameState;
    private ActionValidator validator;
    private Player player1;

    @BeforeEach
    void setUp() {
        turnTracker = mock(TurnTracker.class);
        abilityManager = mock(AbilityManager.class);
        eventTracker = mock(EventTracker.class);
        gameState = mock(GameState.class);
        validator = new DefaultActionValidator(turnTracker, abilityManager, eventTracker);
        player1 = mock(Player.class);
    }

    @Nested
    @DisplayName("Pass validation")
    class PassValidationTests {

        @Test
        void isLegalWhenPlayerHasPriority() {
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            var action = new PlayerAction.Pass(player1);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.Pass(player1);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors())
                    .containsExactly(
                            new ValidationError("Player does not have priority", IllegalActionType.NO_PRIORITY));
        }
    }

    @Nested
    @DisplayName("PlayLand validation")
    class PlayLandValidationTests {

        private Hand hand;
        private Stack stack;

        @BeforeEach
        void setUp() {
            hand = mock(Hand.class);
            stack = mock(Stack.class);
            when(gameState.hand(player1)).thenReturn(hand);
            when(gameState.stack()).thenReturn(stack);
        }

        /// Sets up all mocks so that playing a land is legal.
        private void setupLegalLandPlay(Card land) {
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(turnTracker.activePlayer()).thenReturn(player1);
            when(turnTracker.currentPhase()).thenReturn(Phase.MAIN);
            when(stack.all()).thenReturn(List.of());
            when(hand.contains(land)).thenReturn(true);
            when(eventTracker.eventsFromThisTurn(LandPlayedEvent.class)).thenReturn(Stream.empty());
        }

        @Test
        void isLegalWhenAllConditionsMet() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenNotActivePlayer() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            var otherPlayer = mock(Player.class);
            when(turnTracker.activePlayer()).thenReturn(otherPlayer);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isIllegalWhenNotInMainPhase() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isIllegalWhenStackIsNotEmpty() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            when(stack.all()).thenReturn(List.of(mock(Spell.class)));
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isIllegalWhenLandNotInHand() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            when(hand.contains(land)).thenReturn(false);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NOT_IN_ZONE);
        }

        @Test
        void isIllegalWhenLandDropAlreadyUsed() {
            var land = mock(Card.class);
            setupLegalLandPlay(land);
            // Override: a land was already played this turn
            when(eventTracker.eventsFromThisTurn(LandPlayedEvent.class))
                    .thenReturn(Stream.of(new LandPlayedEvent(player1, mock(Card.class))));
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.LAND_ALREADY_PLAYED);
        }

        @Test
        void collectsMultipleViolations() {
            var land = mock(Card.class);
            var otherPlayer = mock(Player.class);
            // No priority, wrong active player, wrong phase, non-empty stack
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            when(turnTracker.activePlayer()).thenReturn(otherPlayer);
            when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
            when(stack.all()).thenReturn(List.of(mock(Spell.class)));
            when(hand.contains(land)).thenReturn(true);
            when(eventTracker.eventsFromThisTurn(LandPlayedEvent.class)).thenReturn(Stream.empty());
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NO_PRIORITY);
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }
    }

    @Nested
    @DisplayName("SpecialAction validation")
    class SpecialActionValidationTests {

        @Test
        void isLegalWhenPlayerHasPriorityForPriorityRequiringAction() {
            var target = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, target);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriorityForPriorityRequiringAction() {
            var target = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, target);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isLegalForNonPriorityRequiringAction() {
            var target = mock(Card.class);
            // TURN_FACE_UP does not require priority
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.TURN_FACE_UP, target);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }

    @Nested
    @DisplayName("CastSpell validation")
    class CastSpellValidationTests {

        private Hand hand;
        private Stack stack;

        @BeforeEach
        void setUp() {
            hand = mock(Hand.class);
            stack = mock(Stack.class);
            when(gameState.hand(player1)).thenReturn(hand);
            when(gameState.stack()).thenReturn(stack);
        }

        private Card createInstant() {
            return Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Lightning Bolt")
                    .type(Type.INSTANT)
                    .build();
        }

        private Card createSorcery() {
            return Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Divination")
                    .type(Type.SORCERY)
                    .build();
        }

        private Card createCreature() {
            return Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Grizzly Bears")
                    .type(Type.CREATURE)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();
        }

        private void setupLegalCast(Card card) {
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(turnTracker.activePlayer()).thenReturn(player1);
            when(turnTracker.currentPhase()).thenReturn(Phase.MAIN);
            when(stack.all()).thenReturn(List.of());
            when(hand.contains(card)).thenReturn(true);
        }

        @Test
        void isLegalForInstantWhenPlayerHasPriority() {
            var card = createInstant();
            setupLegalCast(card);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isLegalForSorceryDuringMainPhaseWithEmptyStack() {
            var card = createSorcery();
            setupLegalCast(card);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenCardNotInHand() {
            var card = createInstant();
            setupLegalCast(card);
            when(hand.contains(card)).thenReturn(false);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NOT_IN_ZONE);
        }

        @Test
        void isIllegalForSorcerySpeedWhenNotActivePlayer() {
            var card = createCreature();
            setupLegalCast(card);
            var otherPlayer = mock(Player.class);
            when(turnTracker.activePlayer()).thenReturn(otherPlayer);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isIllegalForSorcerySpeedWhenNotInMainPhase() {
            var card = createSorcery();
            setupLegalCast(card);
            when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isIllegalForSorcerySpeedWhenStackNotEmpty() {
            var card = createCreature();
            setupLegalCast(card);
            when(stack.all()).thenReturn(List.of(mock(Spell.class)));
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
        }

        @Test
        void isLegalForInstantDuringCombatPhase() {
            var card = createInstant();
            setupLegalCast(card);
            when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isLegalForInstantWithNonEmptyStack() {
            var card = createInstant();
            setupLegalCast(card);
            when(stack.all()).thenReturn(List.of(mock(Spell.class)));
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalForInstantWhenPlayerHasNoPriority() {
            var card = createInstant();
            setupLegalCast(card);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NO_PRIORITY);
        }

        @Test
        void collectsMultipleViolationsForSorcerySpeed() {
            var card = createSorcery();
            var otherPlayer = mock(Player.class);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            when(turnTracker.activePlayer()).thenReturn(otherPlayer);
            when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
            when(stack.all()).thenReturn(List.of(mock(Spell.class)));
            when(hand.contains(card)).thenReturn(false);
            var action = new PlayerAction.CastSpell(player1, card, SpellContext.empty());

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("ActivateAbility validation")
    class ActivateAbilityValidationTests {

        @Test
        void isLegalWhenPlayerHasPriorityAndAbilityCanActivate() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));
            when(ability.isManaAbility()).thenReturn(false);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(true);

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));
            when(ability.isManaAbility()).thenReturn(false);
            when(turnTracker.hasPriority(player1)).thenReturn(false);

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenSourceNotFound() {
            var source = mock(Card.class);
            when(gameState.findZone(source)).thenReturn(Optional.empty());

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.ILLEGAL_TARGET);
        }

        @Test
        void isIllegalWhenAbilityIndexNegative() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));

            var action = new PlayerAction.ActivateAbility(player1, source, -1);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.errors()).anyMatch(e -> e.reason().equals("Invalid ability index"));
        }

        @Test
        void isIllegalWhenAbilityIndexTooLarge() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));

            var action = new PlayerAction.ActivateAbility(player1, source, 10);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.errors()).anyMatch(e -> e.reason().equals("Invalid ability index"));
        }

        @Test
        void isIllegalWhenAbilityIsNotActivated() {
            var staticAbility = mock(StaticAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(staticAbility))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.errors()).anyMatch(e -> e.reason().equals("Not an activated ability"));
        }

        @Test
        void isIllegalWhenAbilityCannotBeActivated() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));
            when(ability.isManaAbility()).thenReturn(false);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(false);

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.errors()).anyMatch(e -> e.reason().equals("Ability cannot be activated"));
        }

        @Test
        void manaAbilitiesDoNotRequirePriority() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findZone(source)).thenReturn(Optional.of(mock(Hand.class)));
            when(ability.isManaAbility()).thenReturn(true);
            when(turnTracker.hasPriority(player1)).thenReturn(false); // No priority
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(true);

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }
}
