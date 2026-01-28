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
import be.imgn.mtg.engine.object.ObjectId;
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
            var landId = new ObjectId();
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(hand.findById(landId)).thenReturn(Optional.of(mock(Card.class)));
            var action = new PlayerAction.PlayLand(player1, landId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var landId = new ObjectId();
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.PlayLand(player1, landId);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isIllegalWhenLandNotInHand() {
            var landId = new ObjectId();
            when(turnTracker.hasPriority(player1)).thenReturn(true);
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
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, targetId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriorityForPriorityRequiringAction() {
            var targetId = new ObjectId();
            when(turnTracker.hasPriority(player1)).thenReturn(false);
            var action = new PlayerAction.SpecialAction(player1, SpecialActionType.SUSPEND, targetId);

            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.NO_PRIORITY);
        }

        @Test
        void isLegalForNonPriorityRequiringAction() {
            var targetId = new ObjectId();
            // TURN_FACE_UP does not require priority
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
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            var action = new PlayerAction.CastSpell(player1, spellId);

            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void isIllegalWhenPlayerDoesNotHavePriority() {
            var spellId = new ObjectId();
            when(turnTracker.hasPriority(player1)).thenReturn(false);
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
            when(turnTracker.hasPriority(player1)).thenReturn(true);
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
            when(turnTracker.hasPriority(player1)).thenReturn(false);

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
        void isIllegalWhenAbilityIndexNegative() {
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

            var action = new PlayerAction.ActivateAbility(player1, sourceId, -1);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
        }

        @Test
        void isIllegalWhenAbilityIndexTooLarge() {
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

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 10);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.ILLEGAL_TARGET);
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
        }

        @Test
        void isIllegalWhenAbilityIsNotActivated() {
            var sourceId = new ObjectId();
            var staticAbility = mock(StaticAbility.class);
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .abilities(Abilities.of(staticAbility))
                    .build();
            var source = Permanent.fromCard(card, player1).build();

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.reason()).isEqualTo("Not an activated ability");
        }

        @Test
        void isIllegalWhenAbilityCannotBeActivated() {
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
            when(turnTracker.hasPriority(player1)).thenReturn(true);
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(false);

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.type()).isEqualTo(IllegalActionType.RESTRICTION_VIOLATED);
            assertThat(illegal.reason()).isEqualTo("Ability cannot be activated");
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
            when(turnTracker.hasPriority(player1)).thenReturn(false); // No priority
            when(abilityManager.canActivate(ability, source, gameState)).thenReturn(true);

            var action = new PlayerAction.ActivateAbility(player1, sourceId, 0);
            var result = validator.validate(action, gameState);

            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }
}
