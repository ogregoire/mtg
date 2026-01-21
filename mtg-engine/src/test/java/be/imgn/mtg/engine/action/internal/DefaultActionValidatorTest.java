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
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.zone.Hand;

@DisplayName("DefaultActionValidator")
class DefaultActionValidatorTest {

    private PrioritySystem prioritySystem;
    private AbilityManager abilityManager;
    private GameState gameState;
    private ActionValidator validator;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        prioritySystem = mock(PrioritySystem.class);
        abilityManager = mock(AbilityManager.class);
        gameState = mock(GameState.class);
        validator = new DefaultActionValidator(prioritySystem, abilityManager);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
    }

    @Nested
    @DisplayName("Pass validation")
    class PassValidationTests {

        @Test
        void isLegalWhenPlayerHasPriority() {
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            var action = new PlayerAction.Pass(player1);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);
            var action = new PlayerAction.Pass(player1);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenNobodyHasPriority() {
            when(prioritySystem.currentPriorityHolder()).thenReturn(null);
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
            var landId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            when(hand.findById(landId)).thenReturn(Optional.of(mock(Card.class)));
            var action = new PlayerAction.PlayLand(player1, landId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var landId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);
            var action = new PlayerAction.PlayLand(player1, landId);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenLandNotInHand() {
            var landId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            when(hand.findById(landId)).thenReturn(Optional.empty());
            var action = new PlayerAction.PlayLand(player1, landId);

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
            var targetId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, targetId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriorityForPriorityRequiringAction() {
            var targetId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, targetId);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isLegalWhenPlayerDoesNotHavePriorityForNonPriorityRequiringAction() {
            var targetId = new ObjectId();
            // TURN_FACE_UP does not require priority
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.TURN_FACE_UP, targetId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }

    @Nested
    @DisplayName("CastSpell validation")
    class CastSpellValidationTests {

        @Test
        void isLegalWhenPlayerHasPriority() {
            var spellId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            var action = new PlayerAction.CastSpell(player1, spellId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var spellId = new ObjectId();
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);
            var action = new PlayerAction.CastSpell(player1, spellId);

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
            var sourceId = new ObjectId();
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));
            when(ability.isManaAbility()).thenReturn(false);
            when(prioritySystem.currentPriorityHolder()).thenReturn(player1);
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(true);

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var sourceId = new ObjectId();
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));
            when(ability.isManaAbility()).thenReturn(false);
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2);

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenSourceNotFound() {
            var sourceId = new ObjectId();
            when(gameState.findObject(sourceId)).thenReturn(Optional.empty());

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
        }

        @Test
        void manaAbilitiesDoNotRequirePriority() {
            var sourceId = new ObjectId();
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));
            when(ability.isManaAbility()).thenReturn(true);
            when(prioritySystem.currentPriorityHolder()).thenReturn(player2); // Different player
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(true);

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }
}
