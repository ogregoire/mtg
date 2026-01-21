package be.imgn.mtg.engine.action.internal.turnbased;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.Battlefield;

@DisplayName("UntapAction")
class UntapActionTest {

    private UntapAction action;
    private GameState gameState;
    private GameEventProcessor processor;
    private Player activePlayer;
    private Battlefield battlefield;

    @BeforeEach
    void setUp() {
        action = new UntapAction();
        gameState = mock(GameState.class);
        processor = mock(GameEventProcessor.class);
        activePlayer = mock(Player.class);
        battlefield = mock(Battlefield.class);

        when(gameState.activePlayer()).thenReturn(activePlayer);
        when(gameState.battlefield()).thenReturn(battlefield);
    }

    @Nested
    @DisplayName("timing")
    class TimingTests {

        @Test
        void returnsUntapStepUntap() {
            assertThat(action.timing()).isEqualTo(TurnBasedTiming.UNTAP_STEP_UNTAP);
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        void untapsTappedPermanentsControlledByActivePlayer() {
            var tappedPermanent = mockPermanent(true, true); // tapped, can untap
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(tappedPermanent));

            action.execute(gameState, processor);

            verify(tappedPermanent).untap();
        }

        @Test
        void doesNotUntapPermanentsThatCannotUntap() {
            var tappedPermanent = mockPermanent(true, false); // tapped, cannot untap
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(tappedPermanent));

            action.execute(gameState, processor);

            verify(tappedPermanent, never()).untap();
        }

        @Test
        void doesNotUntapUntappedPermanents() {
            var untappedPermanent = mockPermanent(false, true); // untapped, can untap
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(untappedPermanent));

            action.execute(gameState, processor);

            verify(untappedPermanent, never()).untap();
        }

        @Test
        void untapsMultiplePermanentsSimultaneously() {
            var permanent1 = mockPermanent(true, true);
            var permanent2 = mockPermanent(true, true);
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(permanent1, permanent2));

            action.execute(gameState, processor);

            verify(permanent1).untap();
            verify(permanent2).untap();
        }

        @Test
        void handlesEmptyBattlefield() {
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of());

            // Should not throw
            action.execute(gameState, processor);
        }

        @Test
        void handlesMixedTappedAndUntappedPermanents() {
            var tapped = mockPermanent(true, true);
            var untapped = mockPermanent(false, true);
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(tapped, untapped));

            action.execute(gameState, processor);

            verify(tapped).untap();
            verify(untapped, never()).untap();
        }

        private Permanent mockPermanent(boolean isTapped, boolean canUntap) {
            var permanent = mock(Permanent.class);
            when(permanent.isTapped()).thenReturn(isTapped);
            when(permanent.canUntap()).thenReturn(canUntap);
            return permanent;
        }
    }
}
