package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.StepType;

class SkipTrackerTest {

    private SkipTracker skipTracker;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        skipTracker = new DefaultSkipTracker();
        player1 = mock(Player.class);
        player2 = mock(Player.class);
    }

    @Nested
    class PhaseSkipping {

        @Test
        void phaseNotSkippedByDefault() {
            assertThat(skipTracker.isSkipped(PhaseType.COMBAT)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.MAIN)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.BEGINNING)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.ENDING)).isFalse();
        }

        @Test
        void phaseCanBeSkipped() {
            skipTracker.skipPhase(PhaseType.COMBAT);

            assertThat(skipTracker.isSkipped(PhaseType.COMBAT)).isTrue();
        }

        @Test
        void skipPhaseDoesNotAffectOtherPhases() {
            skipTracker.skipPhase(PhaseType.COMBAT);

            assertThat(skipTracker.isSkipped(PhaseType.MAIN)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.BEGINNING)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.ENDING)).isFalse();
        }

        @Test
        void multiplePhaseCanBeSkipped() {
            skipTracker.skipPhase(PhaseType.COMBAT);
            skipTracker.skipPhase(PhaseType.MAIN);

            assertThat(skipTracker.isSkipped(PhaseType.COMBAT)).isTrue();
            assertThat(skipTracker.isSkipped(PhaseType.MAIN)).isTrue();
        }

        @Test
        void phaseSkipClearedOnNewTurn() {
            skipTracker.skipPhase(PhaseType.COMBAT);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.isSkipped(PhaseType.COMBAT)).isFalse();
        }
    }

    @Nested
    class StepSkipping {

        @Test
        void stepNotSkippedByDefault() {
            assertThat(skipTracker.isSkipped(StepType.UNTAP)).isFalse();
            assertThat(skipTracker.isSkipped(StepType.DRAW)).isFalse();
            assertThat(skipTracker.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
        }

        @Test
        void stepCanBeSkipped() {
            skipTracker.skipStep(StepType.DRAW);

            assertThat(skipTracker.isSkipped(StepType.DRAW)).isTrue();
        }

        @Test
        void skipStepDoesNotAffectOtherSteps() {
            skipTracker.skipStep(StepType.DRAW);

            assertThat(skipTracker.isSkipped(StepType.UNTAP)).isFalse();
            assertThat(skipTracker.isSkipped(StepType.UPKEEP)).isFalse();
            assertThat(skipTracker.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
        }

        @Test
        void multipleStepsCanBeSkipped() {
            skipTracker.skipStep(StepType.DRAW);
            skipTracker.skipStep(StepType.COMBAT_DAMAGE);

            assertThat(skipTracker.isSkipped(StepType.DRAW)).isTrue();
            assertThat(skipTracker.isSkipped(StepType.COMBAT_DAMAGE)).isTrue();
        }

        @Test
        void stepSkipClearedOnNewTurn() {
            skipTracker.skipStep(StepType.DRAW);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.isSkipped(StepType.DRAW)).isFalse();
        }
    }

    @Nested
    class TurnSkipping {

        @Test
        void turnNotSkippedByDefault() {
            assertThat(skipTracker.shouldSkipNextTurn(player1)).isFalse();
            assertThat(skipTracker.shouldSkipNextTurn(player2)).isFalse();
        }

        @Test
        void turnCanBeSkippedForPlayer() {
            skipTracker.skipNextTurn(player1);

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isTrue();
        }

        @Test
        void turnSkipDoesNotAffectOtherPlayers() {
            skipTracker.skipNextTurn(player1);

            assertThat(skipTracker.shouldSkipNextTurn(player2)).isFalse();
        }

        @Test
        void multipleTurnsCanBeSkipped() {
            skipTracker.skipNextTurn(player1);
            skipTracker.skipNextTurn(player2);

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isTrue();
            assertThat(skipTracker.shouldSkipNextTurn(player2)).isTrue();
        }

        @Test
        void clearTurnSkipRemovesSkipForPlayer() {
            skipTracker.skipNextTurn(player1);

            skipTracker.clearTurnSkip(player1);

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isFalse();
        }

        @Test
        void clearTurnSkipDoesNotAffectOtherPlayers() {
            skipTracker.skipNextTurn(player1);
            skipTracker.skipNextTurn(player2);

            skipTracker.clearTurnSkip(player1);

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isFalse();
            assertThat(skipTracker.shouldSkipNextTurn(player2)).isTrue();
        }

        @Test
        void turnSkipNotClearedOnNewTurn() {
            // Turn skips persist across turns until consumed
            skipTracker.skipNextTurn(player1);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isTrue();
        }
    }

    @Nested
    class ResetForNewTurn {

        @Test
        void clearsPhaseSkips() {
            skipTracker.skipPhase(PhaseType.COMBAT);
            skipTracker.skipPhase(PhaseType.MAIN);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.isSkipped(PhaseType.COMBAT)).isFalse();
            assertThat(skipTracker.isSkipped(PhaseType.MAIN)).isFalse();
        }

        @Test
        void clearsStepSkips() {
            skipTracker.skipStep(StepType.DRAW);
            skipTracker.skipStep(StepType.COMBAT_DAMAGE);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.isSkipped(StepType.DRAW)).isFalse();
            assertThat(skipTracker.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
        }

        @Test
        void doesNotClearTurnSkips() {
            skipTracker.skipNextTurn(player1);

            skipTracker.resetForNewTurn();

            assertThat(skipTracker.shouldSkipNextTurn(player1)).isTrue();
        }
    }
}
