package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.LandPlayedEvent;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.Battlefield;

@DisplayName("DefaultSpecialActionHandler")
class DefaultSpecialActionHandlerTest {

    private EventBus eventBus;
    private GameState gameState;
    private Battlefield battlefield;
    private SpecialActionHandler handler;
    private Player player;

    @BeforeEach
    void setUp() {
        eventBus = mock(EventBus.class);
        gameState = mock(GameState.class);
        battlefield = mock(Battlefield.class);
        when(gameState.battlefield()).thenReturn(battlefield);
        handler = new DefaultSpecialActionHandler(eventBus);
        player = mock(Player.class);
    }

    @Nested
    @DisplayName("playLand")
    class PlayLandTests {

        private Card land;

        @BeforeEach
        void setUp() {
            land = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Forest")
                    .build();
        }

        @Test
        void returnsSuccessResult() {
            var action = new PlayerAction.PlayLand(player, land);

            var result = handler.playLand(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }

        @Test
        void postsLandPlayedEventToEventBus() {
            var action = new PlayerAction.PlayLand(player, land);

            handler.playLand(action, gameState);

            verify(eventBus).post(new LandPlayedEvent(player, land));
        }

        @Test
        void delegatesEnterToBattlefield() {
            var action = new PlayerAction.PlayLand(player, land);

            handler.playLand(action, gameState);

            verify(battlefield).enter(land, player);
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
