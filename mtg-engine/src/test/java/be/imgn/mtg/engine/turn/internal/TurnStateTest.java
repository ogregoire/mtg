package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.StepType;

class TurnStateTest {

    private GameState gameState;
    private DefaultTurnState turnState;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);

        when(gameState.players()).thenReturn(List.of(player1, player2, player3));
        when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player2);
        when(gameState.nextPlayerInTurnOrder(player2)).thenReturn(player3);
        when(gameState.nextPlayerInTurnOrder(player3)).thenReturn(player1);

        turnState = new DefaultTurnState(gameState);
    }

    @Nested
    class Initialization {

        @Test
        void turnNumberStartsAtZero() {
            assertThat(turnState.turnNumber()).isZero();
        }

        @Test
        void activePlayerThrowsBeforeInitialized() {
            assertThatThrownBy(() -> turnState.activePlayer()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void initializeSetsActivePlayer() {
            turnState.initialize(player1);
            assertThat(turnState.activePlayer()).isEqualTo(player1);
        }

        @Test
        void initializeUpdatesGameState() {
            turnState.initialize(player1);
            verify(gameState).setActivePlayer(player1);
        }
    }

    @Nested
    class NextTurn {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void incrementsTurnNumber() {
            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(1);

            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(2);
        }

        @Test
        void advancesToNextPlayerInTurnOrder() {
            turnState.nextTurn(); // Turn 1 - player1 initialized, advances to player2
            assertThat(turnState.activePlayer()).isEqualTo(player2);

            turnState.nextTurn(); // Turn 2
            assertThat(turnState.activePlayer()).isEqualTo(player3);

            turnState.nextTurn(); // Turn 3 - wraps around
            assertThat(turnState.activePlayer()).isEqualTo(player1);
        }

        @Test
        void returnsNewActivePlayer() {
            var newActive = turnState.nextTurn();
            assertThat(newActive).isEqualTo(player2);
        }

        @Test
        void updatesGameStateActivePlayer() {
            turnState.nextTurn();
            verify(gameState).setActivePlayer(player2);
        }
    }

    @Nested
    class ExtraTurns {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void noExtraTurnsByDefault() {
            assertThat(turnState.peekExtraTurn()).isEmpty();
        }

        @Test
        void addExtraTurnMakesItAvailable() {
            turnState.addExtraTurn(player2);
            assertThat(turnState.peekExtraTurn()).contains(player2);
        }

        @Test
        void extraTurnsAreLIFO() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);

            // Player3's extra turn should be taken first (LIFO)
            assertThat(turnState.peekExtraTurn()).contains(player3);
        }

        @Test
        void nextTurnUsesExtraTurnFirst() {
            turnState.addExtraTurn(player3);

            var activePlayer = turnState.nextTurn();

            assertThat(activePlayer).isEqualTo(player3);
            assertThat(turnState.peekExtraTurn()).isEmpty();
        }

        @Test
        void extraTurnOrderingWithMultipleExtraTurns() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);

            // First extra turn (LIFO - player3)
            assertThat(turnState.nextTurn()).isEqualTo(player3);

            // Second extra turn (player2)
            assertThat(turnState.nextTurn()).isEqualTo(player2);

            // Normal turn order resumes (after player1, next is player2)
            assertThat(turnState.nextTurn()).isEqualTo(player3);
        }
    }

    @Nested
    class RemovePlayer {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void removesPlayerExtraTurns() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);
            turnState.addExtraTurn(player2);

            turnState.removePlayer(player2);

            // Only player3's extra turn should remain
            assertThat(turnState.peekExtraTurn()).contains(player3);
        }
    }

    @Nested
    class OccurrenceTracking {

        @Nested
        class PhaseTracking {

            @Test
            void initialOccurrenceIsZero() {
                assertThat(turnState.occurrence(PhaseType.MAIN)).isZero();
            }

            @Test
            void incrementOccurrenceReturnsNewCount() {
                assertThat(turnState.incrementOccurrence(PhaseType.MAIN)).isEqualTo(1);
                assertThat(turnState.incrementOccurrence(PhaseType.MAIN)).isEqualTo(2);
            }

            @Test
            void occurrenceReturnsCurrentValue() {
                turnState.incrementOccurrence(PhaseType.COMBAT);
                turnState.incrementOccurrence(PhaseType.COMBAT);
                assertThat(turnState.occurrence(PhaseType.COMBAT)).isEqualTo(2);
            }

            @Test
            void differentPhasesTrackedIndependently() {
                turnState.incrementOccurrence(PhaseType.BEGINNING);
                turnState.incrementOccurrence(PhaseType.MAIN);
                turnState.incrementOccurrence(PhaseType.MAIN);

                assertThat(turnState.occurrence(PhaseType.BEGINNING)).isEqualTo(1);
                assertThat(turnState.occurrence(PhaseType.MAIN)).isEqualTo(2);
                assertThat(turnState.occurrence(PhaseType.COMBAT)).isZero();
            }
        }

        @Nested
        class StepTracking {

            @Test
            void initialOccurrenceIsZero() {
                assertThat(turnState.occurrence(StepType.UPKEEP)).isZero();
            }

            @Test
            void incrementOccurrenceReturnsNewCount() {
                assertThat(turnState.incrementOccurrence(StepType.COMBAT_DAMAGE))
                        .isEqualTo(1);
                assertThat(turnState.incrementOccurrence(StepType.COMBAT_DAMAGE))
                        .isEqualTo(2);
            }

            @Test
            void differentStepsTrackedIndependently() {
                turnState.incrementOccurrence(StepType.UNTAP);
                turnState.incrementOccurrence(StepType.UPKEEP);
                turnState.incrementOccurrence(StepType.DRAW);

                assertThat(turnState.occurrence(StepType.UNTAP)).isEqualTo(1);
                assertThat(turnState.occurrence(StepType.UPKEEP)).isEqualTo(1);
                assertThat(turnState.occurrence(StepType.DRAW)).isEqualTo(1);
                assertThat(turnState.occurrence(StepType.CLEANUP)).isZero();
            }
        }

        @Nested
        class ResetOccurrences {

            @Test
            void resetOccurrencesClearsAllPhaseCounts() {
                turnState.incrementOccurrence(PhaseType.BEGINNING);
                turnState.incrementOccurrence(PhaseType.MAIN);
                turnState.incrementOccurrence(PhaseType.MAIN);
                turnState.incrementOccurrence(PhaseType.COMBAT);

                turnState.resetOccurrences();

                assertThat(turnState.occurrence(PhaseType.BEGINNING)).isZero();
                assertThat(turnState.occurrence(PhaseType.MAIN)).isZero();
                assertThat(turnState.occurrence(PhaseType.COMBAT)).isZero();
            }

            @Test
            void resetOccurrencesClearsAllStepCounts() {
                turnState.incrementOccurrence(StepType.UNTAP);
                turnState.incrementOccurrence(StepType.UPKEEP);
                turnState.incrementOccurrence(StepType.DRAW);

                turnState.resetOccurrences();

                assertThat(turnState.occurrence(StepType.UNTAP)).isZero();
                assertThat(turnState.occurrence(StepType.UPKEEP)).isZero();
                assertThat(turnState.occurrence(StepType.DRAW)).isZero();
            }

            @Test
            void canIncrementOccurrenceAfterReset() {
                turnState.incrementOccurrence(PhaseType.MAIN);
                turnState.resetOccurrences();

                assertThat(turnState.incrementOccurrence(PhaseType.MAIN)).isEqualTo(1);
            }
        }
    }

    @Nested
    class SkipTracking {

        @Nested
        class PhaseSkipping {

            @Test
            void phaseNotSkippedByDefault() {
                assertThat(turnState.isSkipped(PhaseType.COMBAT)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.MAIN)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.BEGINNING)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.ENDING)).isFalse();
            }

            @Test
            void phaseCanBeSkipped() {
                turnState.skipPhase(PhaseType.COMBAT);

                assertThat(turnState.isSkipped(PhaseType.COMBAT)).isTrue();
            }

            @Test
            void skipPhaseDoesNotAffectOtherPhases() {
                turnState.skipPhase(PhaseType.COMBAT);

                assertThat(turnState.isSkipped(PhaseType.MAIN)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.BEGINNING)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.ENDING)).isFalse();
            }

            @Test
            void multiplePhasesCanBeSkipped() {
                turnState.skipPhase(PhaseType.COMBAT);
                turnState.skipPhase(PhaseType.MAIN);

                assertThat(turnState.isSkipped(PhaseType.COMBAT)).isTrue();
                assertThat(turnState.isSkipped(PhaseType.MAIN)).isTrue();
            }

            @Test
            void phaseSkipClearedOnNewTurn() {
                turnState.skipPhase(PhaseType.COMBAT);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.isSkipped(PhaseType.COMBAT)).isFalse();
            }
        }

        @Nested
        class StepSkipping {

            @Test
            void stepNotSkippedByDefault() {
                assertThat(turnState.isSkipped(StepType.UNTAP)).isFalse();
                assertThat(turnState.isSkipped(StepType.DRAW)).isFalse();
                assertThat(turnState.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
            }

            @Test
            void stepCanBeSkipped() {
                turnState.skipStep(StepType.DRAW);

                assertThat(turnState.isSkipped(StepType.DRAW)).isTrue();
            }

            @Test
            void skipStepDoesNotAffectOtherSteps() {
                turnState.skipStep(StepType.DRAW);

                assertThat(turnState.isSkipped(StepType.UNTAP)).isFalse();
                assertThat(turnState.isSkipped(StepType.UPKEEP)).isFalse();
                assertThat(turnState.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
            }

            @Test
            void multipleStepsCanBeSkipped() {
                turnState.skipStep(StepType.DRAW);
                turnState.skipStep(StepType.COMBAT_DAMAGE);

                assertThat(turnState.isSkipped(StepType.DRAW)).isTrue();
                assertThat(turnState.isSkipped(StepType.COMBAT_DAMAGE)).isTrue();
            }

            @Test
            void stepSkipClearedOnNewTurn() {
                turnState.skipStep(StepType.DRAW);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.isSkipped(StepType.DRAW)).isFalse();
            }
        }

        @Nested
        class TurnSkipping {

            @Test
            void turnNotSkippedByDefault() {
                assertThat(turnState.shouldSkipNextTurn(player1)).isFalse();
                assertThat(turnState.shouldSkipNextTurn(player2)).isFalse();
            }

            @Test
            void turnCanBeSkippedForPlayer() {
                turnState.skipNextTurn(player1);

                assertThat(turnState.shouldSkipNextTurn(player1)).isTrue();
            }

            @Test
            void turnSkipDoesNotAffectOtherPlayers() {
                turnState.skipNextTurn(player1);

                assertThat(turnState.shouldSkipNextTurn(player2)).isFalse();
            }

            @Test
            void multipleTurnsCanBeSkipped() {
                turnState.skipNextTurn(player1);
                turnState.skipNextTurn(player2);

                assertThat(turnState.shouldSkipNextTurn(player1)).isTrue();
                assertThat(turnState.shouldSkipNextTurn(player2)).isTrue();
            }

            @Test
            void clearTurnSkipRemovesSkipForPlayer() {
                turnState.skipNextTurn(player1);

                turnState.clearTurnSkip(player1);

                assertThat(turnState.shouldSkipNextTurn(player1)).isFalse();
            }

            @Test
            void clearTurnSkipDoesNotAffectOtherPlayers() {
                turnState.skipNextTurn(player1);
                turnState.skipNextTurn(player2);

                turnState.clearTurnSkip(player1);

                assertThat(turnState.shouldSkipNextTurn(player1)).isFalse();
                assertThat(turnState.shouldSkipNextTurn(player2)).isTrue();
            }

            @Test
            void turnSkipNotClearedOnResetSkipsForNewTurn() {
                // Turn skips persist across turns until consumed
                turnState.skipNextTurn(player1);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.shouldSkipNextTurn(player1)).isTrue();
            }
        }

        @Nested
        class ResetSkipsForNewTurn {

            @Test
            void clearsPhaseSkips() {
                turnState.skipPhase(PhaseType.COMBAT);
                turnState.skipPhase(PhaseType.MAIN);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.isSkipped(PhaseType.COMBAT)).isFalse();
                assertThat(turnState.isSkipped(PhaseType.MAIN)).isFalse();
            }

            @Test
            void clearsStepSkips() {
                turnState.skipStep(StepType.DRAW);
                turnState.skipStep(StepType.COMBAT_DAMAGE);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.isSkipped(StepType.DRAW)).isFalse();
                assertThat(turnState.isSkipped(StepType.COMBAT_DAMAGE)).isFalse();
            }

            @Test
            void doesNotClearTurnSkips() {
                turnState.skipNextTurn(player1);

                turnState.resetSkipsForNewTurn();

                assertThat(turnState.shouldSkipNextTurn(player1)).isTrue();
            }
        }
    }
}
