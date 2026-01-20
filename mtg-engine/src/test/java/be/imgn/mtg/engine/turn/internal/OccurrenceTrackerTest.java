package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.StepType;

class OccurrenceTrackerTest {

    private OccurrenceTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new DefaultOccurrenceTracker();
    }

    @Nested
    class PhaseTracking {

        @Test
        void initialCountIsZero() {
            assertThat(tracker.count(PhaseType.MAIN)).isZero();
        }

        @Test
        void incrementReturnsNewCount() {
            assertThat(tracker.increment(PhaseType.MAIN)).isEqualTo(1);
            assertThat(tracker.increment(PhaseType.MAIN)).isEqualTo(2);
        }

        @Test
        void countReturnsCurrentValue() {
            tracker.increment(PhaseType.COMBAT);
            tracker.increment(PhaseType.COMBAT);
            assertThat(tracker.count(PhaseType.COMBAT)).isEqualTo(2);
        }

        @Test
        void differentPhasesTrackedIndependently() {
            tracker.increment(PhaseType.BEGINNING);
            tracker.increment(PhaseType.MAIN);
            tracker.increment(PhaseType.MAIN);

            assertThat(tracker.count(PhaseType.BEGINNING)).isEqualTo(1);
            assertThat(tracker.count(PhaseType.MAIN)).isEqualTo(2);
            assertThat(tracker.count(PhaseType.COMBAT)).isZero();
        }
    }

    @Nested
    class StepTracking {

        @Test
        void initialCountIsZero() {
            assertThat(tracker.count(StepType.UPKEEP)).isZero();
        }

        @Test
        void incrementReturnsNewCount() {
            assertThat(tracker.increment(StepType.COMBAT_DAMAGE)).isEqualTo(1);
            assertThat(tracker.increment(StepType.COMBAT_DAMAGE)).isEqualTo(2);
        }

        @Test
        void differentStepsTrackedIndependently() {
            tracker.increment(StepType.UNTAP);
            tracker.increment(StepType.UPKEEP);
            tracker.increment(StepType.DRAW);

            assertThat(tracker.count(StepType.UNTAP)).isEqualTo(1);
            assertThat(tracker.count(StepType.UPKEEP)).isEqualTo(1);
            assertThat(tracker.count(StepType.DRAW)).isEqualTo(1);
            assertThat(tracker.count(StepType.CLEANUP)).isZero();
        }
    }

    @Nested
    class Reset {

        @Test
        void resetClearsAllPhaseCounts() {
            tracker.increment(PhaseType.BEGINNING);
            tracker.increment(PhaseType.MAIN);
            tracker.increment(PhaseType.MAIN);
            tracker.increment(PhaseType.COMBAT);

            tracker.reset();

            assertThat(tracker.count(PhaseType.BEGINNING)).isZero();
            assertThat(tracker.count(PhaseType.MAIN)).isZero();
            assertThat(tracker.count(PhaseType.COMBAT)).isZero();
        }

        @Test
        void resetClearsAllStepCounts() {
            tracker.increment(StepType.UNTAP);
            tracker.increment(StepType.UPKEEP);
            tracker.increment(StepType.DRAW);

            tracker.reset();

            assertThat(tracker.count(StepType.UNTAP)).isZero();
            assertThat(tracker.count(StepType.UPKEEP)).isZero();
            assertThat(tracker.count(StepType.DRAW)).isZero();
        }

        @Test
        void canIncrementAfterReset() {
            tracker.increment(PhaseType.MAIN);
            tracker.reset();

            assertThat(tracker.increment(PhaseType.MAIN)).isEqualTo(1);
        }
    }
}
