package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Hand;

@DisplayName("DefaultActionValidator")
class DefaultActionValidatorTest {

    private TurnTracker turnTracker;
    private AbilityManager abilityManager;
    private GameState gameState;
    private ActionValidator validator;
    private Player player1;

    @BeforeEach
    void setUp() {
        turnTracker = mock(TurnTracker.class);
        abilityManager = mock(AbilityManager.class);
        gameState = mock(GameState.class);
        validator = new DefaultActionValidator(turnTracker, abilityManager);
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }
    }

    @Nested
    @DisplayName("PlayLand validation")
    class PlayLandValidationTests {

        private Hand hand;

        @BeforeEach
        void setUp() {
            hand = mock(Hand.class);
            when(gameState.hand(player1)).thenReturn(hand);
        }

        @Test
        void isLegalWhenPlayerHasPriorityAndLandInHand() {
            var land = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(hand.contains(land)).thenReturn(true);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var land = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenLandNotInHand() {
            var land = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(hand.contains(land)).thenReturn(false);
            var action = new PlayerAction.PlayLand(player1, land);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NOT_IN_ZONE);
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
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

        @Test
        void isLegalWhenPlayerHasPriority() {
            var card = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            var action = new PlayerAction.CastSpell(player1, card);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var card = mock(Card.class);
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.CastSpell(player1, card);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenSourceNotFound() {
            var source = mock(Card.class);
            when(gameState.findZone(source)).thenReturn(Optional.empty());

            var action = new PlayerAction.ActivateAbility(player1, source, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.reason()).isEqualTo("Not an activated ability");
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
            assertThat(illegal.type()).isEqualTo(IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.reason()).isEqualTo("Ability cannot be activated");
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
