package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;
import be.imgn.mtg.engine.turn.PrioritySystem;

@DisplayName("DefaultActionExecutor")
class DefaultActionExecutorTest {

    private PrioritySystem prioritySystem;
    private SpecialActionHandler specialActionHandler;
    private AbilityManager abilityManager;
    private GameState gameState;
    private ActionExecutor executor;
    private Player player;

    @BeforeEach
    void setUp() {
        prioritySystem = mock(PrioritySystem.class);
        specialActionHandler = mock(SpecialActionHandler.class);
        abilityManager = mock(AbilityManager.class);
        gameState = mock(GameState.class);
        executor = new DefaultActionExecutor(prioritySystem, specialActionHandler, abilityManager);
        player = mock(Player.class);
    }

    @Nested
    @DisplayName("Pass execution")
    class PassExecutionTests {

        @Test
        void callsPassOnPrioritySystem() {
            var action = new PlayerAction.Pass(player);

            executor.execute(action, gameState);

            verify(prioritySystem).pass(player);
        }

        @Test
        void returnsSuccessWithEmptyEvents() {
            var action = new PlayerAction.Pass(player);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            assertThat(((ExecutionResult.Success) result).events()).isEmpty();
        }

        @Test
        void doesNotResetPrioritySystem() {
            var action = new PlayerAction.Pass(player);

            executor.execute(action, gameState);

            verify(prioritySystem, never()).reset();
        }
    }

    @Nested
    @DisplayName("PlayLand execution")
    class PlayLandExecutionTests {

        @Test
        void delegatesToSpecialActionHandler() {
            var landId = new ObjectId();
            var action = new PlayerAction.PlayLand(player, landId);
            when(specialActionHandler.playLand(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(specialActionHandler).playLand(action, gameState);
        }

        @Test
        void resetsPriorityOnSuccess() {
            var landId = new ObjectId();
            var action = new PlayerAction.PlayLand(player, landId);
            when(specialActionHandler.playLand(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(prioritySystem).reset();
        }

        @Test
        void returnsResultFromHandler() {
            var landId = new ObjectId();
            var action = new PlayerAction.PlayLand(player, landId);
            var expectedResult = new ExecutionResult.Success(List.of());
            when(specialActionHandler.playLand(action, gameState)).thenReturn(expectedResult);

            var result = executor.execute(action, gameState);

            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    @DisplayName("SpecialAction execution")
    class SpecialActionExecutionTests {

        @Test
        void delegatesToSpecialActionHandler() {
            var targetId = new ObjectId();
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.SUSPEND, targetId);
            when(specialActionHandler.execute(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(specialActionHandler).execute(action, gameState);
        }

        @Test
        void resetsPriorityOnSuccess() {
            var targetId = new ObjectId();
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.SUSPEND, targetId);
            when(specialActionHandler.execute(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(prioritySystem).reset();
        }
    }

    @Nested
    @DisplayName("CastSpell execution")
    class CastSpellExecutionTests {

        @Test
        void throwsUnsupportedOperationException() {
            var spellId = new ObjectId();
            var action = new PlayerAction.CastSpell(player, spellId);

            assertThatThrownBy(() -> executor.execute(action, gameState))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("not yet implemented");
        }
    }

    @Nested
    @DisplayName("ActivateAbility execution")
    class ActivateAbilityExecutionTests {

        @Test
        void returnsIllegalWhenSourceNotFound() {
            var sourceId = new ObjectId();
            when(gameState.findObject(sourceId)).thenReturn(Optional.empty());
            var action = new PlayerAction.ActivateAbility(player, sourceId, 0);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
            var illegal = (ExecutionResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Source object not found");
        }

        @Test
        void delegatesToAbilityManager() {
            var sourceId = new ObjectId();
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));
            when(abilityManager.activate(eq(ability), eq(source), any(AbilityContext.class)))
                    .thenReturn(new ActivationResult.ManaAbilitySuccess(List.of()));

            var action = new PlayerAction.ActivateAbility(player, sourceId, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }
    }
}
