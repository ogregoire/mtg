package be.imgn.mtg.engine.turn.internal;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.TurnState;

class DurationTrackerTest {

    private DurationTracker tracker;
    private Step mockStep;
    private TurnState mockTurnState;

    @BeforeEach
    void setUp() {
        tracker = new DefaultDurationTracker();
        mockStep = mock(Step.class);
        mockTurnState = mock(TurnState.class);
    }

    @Nested
    class ExpireUntilStep {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            tracker.expireUntilStep(mockStep);
        }
    }

    @Nested
    class ExpireUntilEndOfStep {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            tracker.expireUntilEndOfStep(mockStep);
        }
    }

    @Nested
    class ExpireUntilEndOfTurn {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            tracker.expireUntilEndOfTurn();
        }
    }

    @Nested
    class ExpireUntilEndOfCombat {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            tracker.expireUntilEndOfCombat();
        }
    }

    @Nested
    class ExpireUntilNextTurn {

        @Test
        void canBeCalledWithoutException() {
            // Currently a stub - verify it doesn't throw
            tracker.expireUntilNextTurn(mockTurnState);
        }
    }
}
