package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.GameState;

@DisplayName("DefaultSpecialActionHandler")
class DefaultSpecialActionHandlerTest {

    private GameEventProcessor eventProcessor;
    private GameState gameState;
    private SpecialActionHandler handler;
    private Player player;

    @BeforeEach
    void setUp() {
        eventProcessor = mock(GameEventProcessor.class);
        gameState = mock(GameState.class);
        handler = new DefaultSpecialActionHandler(eventProcessor);
        player = mock(Player.class);
    }

    @Nested
    @DisplayName("playLand")
    class PlayLandTests {

        @Test
        void returnsSuccessResult() {
            var land = mock(Card.class);
            var action = new PlayerAction.PlayLand(player, land);

            var result = handler.playLand(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }

        @Test
        void returnsEmptyEventsForNow() {
            // This is a stub implementation - full implementation will produce events
            var land = mock(Card.class);
            var action = new PlayerAction.PlayLand(player, land);

            var result = handler.playLand(action, gameState);

            var success = (ExecutionResult.Success) result;
            assertThat(success.events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("execute special action")
    class ExecuteSpecialActionTests {

        @Test
        void returnsIllegalForPlayLandType() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.PLAY_LAND, target);

            var result = handler.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
        }

        @Test
        void returnsSuccessForTurnFaceUp() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.TURN_FACE_UP, target);

            var result = handler.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }

        @Test
        void returnsSuccessForSuspend() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.SUSPEND, target);

            var result = handler.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }

        @Test
        void returnsSuccessForCompanion() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.COMPANION, target);

            var result = handler.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }

        @Test
        void returnsSuccessForForetell() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.FORETELL, target);

            var result = handler.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }
    }
}
