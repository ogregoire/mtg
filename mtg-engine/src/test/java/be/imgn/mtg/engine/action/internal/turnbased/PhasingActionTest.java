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

@DisplayName("PhasingAction")
class PhasingActionTest {

    private PhasingAction action;
    private GameState gameState;
    private GameEventProcessor processor;
    private Player activePlayer;
    private Battlefield battlefield;

    @BeforeEach
    void setUp() {
        action = new PhasingAction();
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
        void returnsUntapStepPhasing() {
            assertThat(action.timing()).isEqualTo(TurnBasedTiming.UNTAP_STEP_PHASING);
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        void phasesInPhasedOutPermanentsControlledByActivePlayer() {
            var phasedOutPermanent = mockPermanent(true, true); // phased out, can phase in
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(phasedOutPermanent));

            action.execute(gameState, processor);

            verify(phasedOutPermanent).phaseIn();
        }

        @Test
        void doesNotPhaseInPermanentsThatCannotPhaseIn() {
            var phasedOutPermanent = mockPermanent(true, false); // phased out, cannot phase in
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(phasedOutPermanent));

            action.execute(gameState, processor);

            verify(phasedOutPermanent, never()).phaseIn();
        }

        @Test
        void doesNotPhaseInPhasedInPermanents() {
            var phasedInPermanent = mockPermanent(false, true); // phased in, can phase in
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(phasedInPermanent));

            action.execute(gameState, processor);

            verify(phasedInPermanent, never()).phaseIn();
        }

        @Test
        void phasesInMultiplePermanentsSimultaneously() {
            var permanent1 = mockPermanent(true, true);
            var permanent2 = mockPermanent(true, true);
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of(permanent1, permanent2));

            action.execute(gameState, processor);

            verify(permanent1).phaseIn();
            verify(permanent2).phaseIn();
        }

        @Test
        void handlesEmptyBattlefield() {
            when(battlefield.controlledBy(activePlayer)).thenReturn(List.of());

            // Should not throw
            action.execute(gameState, processor);
        }

        private Permanent mockPermanent(boolean isPhasedOut, boolean canPhaseIn) {
            var permanent = mock(Permanent.class);
            when(permanent.isPhasedOut()).thenReturn(isPhasedOut);
            when(permanent.canPhaseIn()).thenReturn(canPhaseIn);
            return permanent;
        }
    }
}
