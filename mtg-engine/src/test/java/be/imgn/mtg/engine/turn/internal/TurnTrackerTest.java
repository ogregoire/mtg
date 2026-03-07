package be.imgn.mtg.engine.turn.internal;

import static be.imgn.mtg.engine.util.MoreGatherers.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerLeftEvent;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.PhaseEndedEvent;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepEndedEvent;
import be.imgn.mtg.engine.turn.StepStartedEvent;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.turn.TurnStartedEvent;
import be.imgn.mtg.engine.zone.Stack;

class TurnTrackerTest {

    private EventBus eventBus;
    private Stack stack;
    private Player player1;
    private Player player2;
    private Player player3;
    private List<Event> firedEvents;
    private Map<Class<?>, List<Consumer<?>>> eventSubscribers;

    @BeforeEach
    void setUp() {
        eventBus = mock(EventBus.class);
        stack = mock(Stack.class);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);

        when(stack.isEmpty()).thenReturn(true);

        firedEvents = new ArrayList<>();
        eventSubscribers = new HashMap<>();

        // Capture event posts
        doAnswer(inv -> {
                    firedEvents.add(inv.getArgument(0));
                    return null;
                })
                .when(eventBus)
                .post(any(Event.class));

        // Capture subscriptions with priority
        doAnswer(inv -> {
                    Class<?> eventType = inv.getArgument(0);
                    Consumer<?> handler = inv.getArgument(2);
                    eventSubscribers
                            .computeIfAbsent(eventType, _ -> new ArrayList<>())
                            .add(handler);
                    return null;
                })
                .when(eventBus)
                .subscribe(any(), anyInt(), any());
    }

    /// Simulates a player leaving the game by invoking the PlayerLeftEvent handler.
    @SuppressWarnings("unchecked")
    private void playerLeaves(Player player) {
        var handlers = eventSubscribers.get(PlayerLeftEvent.class);
        if (handlers != null) {
            var event = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.LOST);
            for (var handler : handlers) {
                ((Consumer<PlayerLeftEvent>) handler).accept(event);
            }
        }
    }

    private DefaultTurnTracker createTracker(List<Player> players) {
        return new DefaultTurnTracker(players, stack, eventBus, List.of());
    }

    private DefaultTurnTracker createTrackerWithSbaCheckers(List<Player> players, List<StateBasedAction> sbaCheckers) {
        return new DefaultTurnTracker(players, stack, eventBus, sbaCheckers);
    }

    // ========== StartGame Tests ==========

    @Nested
    class StartGame {

        @Test
        void setsActivePlayerToStartingPlayer() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.activePlayer()).isEqualTo(player1);
        }

        @Test
        void canStartWithAnyPlayerInTheGame() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player2);

            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void setsTurnNumberToOne() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.currentTurn().number()).isEqualTo(1);
        }

        @Test
        void startsInBeginningPhase() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.currentPhase()).isEqualTo(Phase.BEGINNING);
        }

        @Test
        void startsInUpkeepStep() {
            // Untap has no priority, so we advance to Upkeep
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
        }

        @Test
        void firesTurnStartedEvent() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            var turnStarted = firedEvents.stream()
                    .gather(instanceOf(TurnStartedEvent.class))
                    .findFirst()
                    .orElseThrow();

            assertThat(turnStarted.turnNumber()).isEqualTo(1);
            assertThat(turnStarted.activePlayer()).isEqualTo(player1);
        }

        @Test
        void throwsIfGameAlreadyStarted() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThatThrownBy(() -> tracker.startGame(player2))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already been started");
        }

        @Test
        void throwsIfStartingPlayerNotInGame() {
            var tracker = createTracker(List.of(player1, player2));
            var outsidePlayer = mock(Player.class);

            assertThatThrownBy(() -> tracker.startGame(outsidePlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not in the game");
        }

        @Test
        void grantsPriorityToActivePlayer() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.hasPriority(player1)).isTrue();
            assertThat(tracker.hasPriority(player2)).isFalse();
        }
    }

    @Nested
    class ActivePlayer {

        @Test
        void throwsIfGameNotStarted() {
            var tracker = createTracker(List.of(player1, player2));

            assertThatThrownBy(tracker::activePlayer)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not been started");
        }
    }

    @Nested
    class CurrentTurn {

        @Test
        void containsTurnNumberAndActivePlayer() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            var turn = tracker.currentTurn();

            assertThat(turn.number()).isEqualTo(1);
            assertThat(turn.activePlayer()).isEqualTo(player1);
        }
    }

    // ========== Priority Tests ==========

    @Nested
    class HasPriority {

        @Test
        void returnsTrueForPlayerWithPriority() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.hasPriority(player1)).isTrue();
        }

        @Test
        void returnsFalseForPlayerWithoutPriority() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.hasPriority(player2)).isFalse();
        }

        @Test
        void returnsFalseForAllPlayersWhenNoPriorityGranted() {
            var tracker = createTracker(List.of(player1, player2));
            // Don't start the game - no priority granted yet

            assertThat(tracker.hasPriority(player1)).isFalse();
            assertThat(tracker.hasPriority(player2)).isFalse();
        }
    }

    @Nested
    class PassPriority {

        @Test
        void throwsIfPlayerDoesNotHavePriority() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThatThrownBy(() -> tracker.passPriority(player2))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not have priority");
        }

        @Test
        void passesPriorityToNextPlayerInAPNAPOrder() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player1 has priority (active player)
            assertThat(tracker.hasPriority(player1)).isTrue();

            tracker.passPriority(player1);

            // player2 should have priority next
            assertThat(tracker.hasPriority(player2)).isTrue();
            assertThat(tracker.hasPriority(player1)).isFalse();

            tracker.passPriority(player2);

            // player3 should have priority next
            assertThat(tracker.hasPriority(player3)).isTrue();
        }

        @Test
        void advancesStepWhenAllPlayersPassWithEmptyStack() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // In UPKEEP step
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);

            // Both players pass
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // In 2-player game, DRAW is skipped on turn 1
            // So we should be in MAIN phase
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
        }

        @Test
        void advancesPhaseWhenAllPlayersPassInMainPhase() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Pass through UPKEEP (DRAW skipped in 2-player turn 1)
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Now in first MAIN phase
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(1);

            // Both players pass
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Should advance to COMBAT phase
            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);
        }

        @Test
        void activePlayerGetsPriorityFirstAfterAdvancing() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Pass through UPKEEP - all 3 players pass
            tracker.passPriority(player1);
            tracker.passPriority(player2);
            tracker.passPriority(player3);

            // Now in DRAW step - active player should have priority
            assertThat(tracker.hasPriority(player1)).isTrue();
        }
    }

    @Nested
    class APNAPOrder {

        @Test
        void followsTurnOrderStartingWithActivePlayer() {
            // Players in turn order: player1, player2, player3
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // APNAP for player1's turn: player1, player2, player3
            assertThat(tracker.hasPriority(player1)).isTrue();

            tracker.passPriority(player1);
            assertThat(tracker.hasPriority(player2)).isTrue();

            tracker.passPriority(player2);
            assertThat(tracker.hasPriority(player3)).isTrue();
        }

        @Test
        void wrapsAroundWhenActivePlayerIsNotFirst() {
            // Players in turn order: player1, player2, player3
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Complete turn 1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Now turn 2, player2 is active
            // APNAP for player2's turn: player2, player3, player1
            assertThat(tracker.activePlayer()).isEqualTo(player2);
            assertThat(tracker.hasPriority(player2)).isTrue();

            tracker.passPriority(player2);
            assertThat(tracker.hasPriority(player3)).isTrue();

            tracker.passPriority(player3);
            assertThat(tracker.hasPriority(player1)).isTrue();
        }
    }

    // ========== Phase and Step Tests ==========

    @Nested
    class CurrentPhase {

        @Test
        void startsInBeginningPhase() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.currentPhase()).isEqualTo(Phase.BEGINNING);
        }

        @Test
        void progressesThroughAllPhases() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // BEGINNING
            assertThat(tracker.currentPhase()).isEqualTo(Phase.BEGINNING);

            // Pass through BEGINNING steps
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // MAIN 1
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);

            passAll(tracker, player1, player2, player3); // MAIN 1

            // COMBAT
            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);

            // Pass through COMBAT steps
            passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
            passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
            passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
            passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
            passAll(tracker, player1, player2, player3); // END_OF_COMBAT

            // MAIN 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);

            passAll(tracker, player1, player2, player3); // MAIN 2

            // ENDING
            assertThat(tracker.currentPhase()).isEqualTo(Phase.ENDING);
        }
    }

    @Nested
    class CurrentStep {

        @Test
        void startsInUpkeepAfterUntapHasNoPriority() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // UNTAP has no priority, so we should be in UPKEEP
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
        }

        @Test
        void isNullDuringMainPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentStep()).isNull();
        }

        @Test
        void progressesThroughBeginningSteps() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // UNTAP already passed (no priority)
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);

            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.DRAW);
        }

        @Test
        void progressesThroughCombatSteps() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to COMBAT
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1

            assertThat(tracker.currentStep()).isEqualTo(Step.BEGINNING_OF_COMBAT);
            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.DECLARE_ATTACKERS);
            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.DECLARE_BLOCKERS);
            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.COMBAT_DAMAGE);
            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.END_OF_COMBAT);
        }

        @Test
        void progressesThroughEndingSteps() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to ENDING
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1
            passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
            passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
            passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
            passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
            passAll(tracker, player1, player2, player3); // END_OF_COMBAT
            passAll(tracker, player1, player2, player3); // MAIN 2

            assertThat(tracker.currentPhase()).isEqualTo(Phase.ENDING);
            assertThat(tracker.currentStep()).isEqualTo(Step.END);
        }
    }

    @Nested
    class TurnProgression {

        @Test
        void advancesToNextPlayerAfterTurnEnds() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Complete turn 1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2 is player2
            assertThat(tracker.activePlayer()).isEqualTo(player2);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void turnOrderWrapsAround() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Turn 1: player1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2: player2
            assertThat(tracker.activePlayer()).isEqualTo(player2);
            completeTurnMultiplayer(tracker, player2, player3, player1);

            // Turn 3: player3
            assertThat(tracker.activePlayer()).isEqualTo(player3);
            completeTurnMultiplayer(tracker, player3, player1, player2);

            // Turn 4: back to player1
            assertThat(tracker.activePlayer()).isEqualTo(player1);
        }

        @Test
        void turnNumberIncrementsEachTurn() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            assertThat(tracker.currentTurn().number()).isEqualTo(1);

            completeTurn(tracker, player1, player2);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);

            completeTurn(tracker, player2, player1);
            assertThat(tracker.currentTurn().number()).isEqualTo(3);
        }

        @Test
        void newTurnStartsInBeginningPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            completeTurnMultiplayer(tracker, player1, player2, player3);

            assertThat(tracker.currentPhase()).isEqualTo(Phase.BEGINNING);
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
        }
    }

    @Nested
    class PhaseOccurrenceTracking {

        @Test
        void firstMainPhaseIsOccurrenceOne() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(1);
        }

        @Test
        void secondMainPhaseIsOccurrenceTwo() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1
            passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
            passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
            passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
            passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
            passAll(tracker, player1, player2, player3); // END_OF_COMBAT

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(2);
        }

        @Test
        void occurrenceResetsOnNewTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2, first MAIN should be occurrence 1 again
            passAll(tracker, player2, player3, player1); // UPKEEP
            passAll(tracker, player2, player3, player1); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(1);
        }
    }

    @Nested
    class StepOccurrenceTracking {

        @Test
        void firstUpkeepIsOccurrenceOne() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1);
        }

        @Test
        void occurrenceIsZeroInMainPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(0);
        }

        @Test
        void occurrenceResetsOnNewPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // BEGINNING phase
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1);

            passAll(tracker, player1, player2, player3); // UPKEEP
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1); // DRAW is occurrence 1 too

            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN

            // COMBAT phase - BEGINNING_OF_COMBAT should be occurrence 1
            assertThat(tracker.currentStep()).isEqualTo(Step.BEGINNING_OF_COMBAT);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1);
        }
    }

    // ========== Event Order Tests ==========

    @Nested
    class EventOrder {

        @Test
        void eventsAreFiredInCorrectOrderForBeginningPhase_twoPlayerGame() {
            // In a 2-player game, turn 1 skips the draw step
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // After startGame, we should have:
            // 1. TurnStartedEvent
            // 2. PhaseStartedEvent(BEGINNING)
            // 3. StepStartedEvent(UNTAP) - no priority, immediately ends
            // 4. StepEndedEvent(UNTAP)
            // 5. StepStartedEvent(UPKEEP) - has priority, waiting

            assertThat(firedEvents).hasSize(5);
            assertThat(firedEvents.get(0)).isInstanceOf(TurnStartedEvent.class);
            assertThat(firedEvents.get(1)).isInstanceOf(PhaseStartedEvent.class);
            assertThat(((PhaseStartedEvent) firedEvents.get(1)).phase()).isEqualTo(Phase.BEGINNING);
            assertThat(firedEvents.get(2)).isInstanceOf(StepStartedEvent.class);
            assertThat(((StepStartedEvent) firedEvents.get(2)).step()).isEqualTo(Step.UNTAP);
            assertThat(firedEvents.get(3)).isInstanceOf(StepEndedEvent.class);
            assertThat(((StepEndedEvent) firedEvents.get(3)).step()).isEqualTo(Step.UNTAP);
            assertThat(firedEvents.get(4)).isInstanceOf(StepStartedEvent.class);
            assertThat(((StepStartedEvent) firedEvents.get(4)).step()).isEqualTo(Step.UPKEEP);

            // Now pass priority for both players to end upkeep
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // In a 2-player game, turn 1 skips the draw step, so after upkeep ends:
            // 6. StepEndedEvent(UPKEEP)
            // 7. PhaseEndedEvent(BEGINNING) - no more steps since DRAW is skipped
            // 8. PhaseStartedEvent(MAIN)
            assertThat(firedEvents.get(5)).isInstanceOf(StepEndedEvent.class);
            assertThat(((StepEndedEvent) firedEvents.get(5)).step()).isEqualTo(Step.UPKEEP);
            assertThat(firedEvents.get(6)).isInstanceOf(PhaseEndedEvent.class);
            assertThat(((PhaseEndedEvent) firedEvents.get(6)).phase()).isEqualTo(Phase.BEGINNING);
            assertThat(firedEvents.get(7)).isInstanceOf(PhaseStartedEvent.class);
            assertThat(((PhaseStartedEvent) firedEvents.get(7)).phase()).isEqualTo(Phase.MAIN);
        }

        @Test
        void eventsAreFiredInCorrectOrderForBeginningPhase_multiplayerGame() {
            // In multiplayer, draw step is NOT skipped on turn 1
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // After startGame: UNTAP ended, UPKEEP started
            assertThat(firedEvents).hasSize(5);

            // Pass priority for all 3 players to end upkeep
            tracker.passPriority(player1);
            tracker.passPriority(player2);
            tracker.passPriority(player3);

            // After upkeep ends, we get DRAW step (not skipped in multiplayer):
            // 6. StepEndedEvent(UPKEEP)
            // 7. StepStartedEvent(DRAW)
            assertThat(firedEvents.get(5)).isInstanceOf(StepEndedEvent.class);
            assertThat(((StepEndedEvent) firedEvents.get(5)).step()).isEqualTo(Step.UPKEEP);
            assertThat(firedEvents.get(6)).isInstanceOf(StepStartedEvent.class);
            assertThat(((StepStartedEvent) firedEvents.get(6)).step()).isEqualTo(Step.DRAW);

            // Pass priority for draw step
            tracker.passPriority(player1);
            tracker.passPriority(player2);
            tracker.passPriority(player3);

            // After draw ends:
            // 8. StepEndedEvent(DRAW)
            // 9. PhaseEndedEvent(BEGINNING)
            // 10. PhaseStartedEvent(MAIN)
            assertThat(firedEvents.get(7)).isInstanceOf(StepEndedEvent.class);
            assertThat(((StepEndedEvent) firedEvents.get(7)).step()).isEqualTo(Step.DRAW);
            assertThat(firedEvents.get(8)).isInstanceOf(PhaseEndedEvent.class);
            assertThat(((PhaseEndedEvent) firedEvents.get(8)).phase()).isEqualTo(Phase.BEGINNING);
            assertThat(firedEvents.get(9)).isInstanceOf(PhaseStartedEvent.class);
            assertThat(((PhaseStartedEvent) firedEvents.get(9)).phase()).isEqualTo(Phase.MAIN);
        }

        @Test
        void twoPlayerGameSkipsFirstDrawStep() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Clear events to see just what happens after upkeep
            firedEvents.clear();

            // Pass priority for upkeep
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // In a two-player game, turn 1 skips draw step
            // So we should go directly from UPKEEP to BEGINNING end -> MAIN
            var stepEvents = firedEvents.stream()
                    .filter(e -> e instanceof StepStartedEvent || e instanceof StepEndedEvent)
                    .toList();

            // Should NOT see any DRAW step events
            assertThat(stepEvents.stream()
                            .filter(e -> e instanceof StepStartedEvent s && s.step() == Step.DRAW)
                            .count())
                    .isZero();
        }

        @Test
        void twoPlayerGameHasDrawStepOnTurnsAfterFirst() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Complete turn 1 (draw skipped)
            completeTurn(tracker, player1, player2);

            // Turn 2 is player2's turn
            assertThat(tracker.activePlayer()).isEqualTo(player2);
            firedEvents.clear();

            // Pass through UPKEEP on turn 2
            passAll(tracker, player2, player1);

            // Turn 2 should have DRAW step
            var stepEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(stepEvents.stream().anyMatch(s -> s.step() == Step.DRAW)).isTrue();

            // Complete turn 2 (with draw)
            passAll(tracker, player2, player1); // DRAW
            passAll(tracker, player2, player1); // MAIN 1
            passAll(tracker, player2, player1); // BEGINNING_OF_COMBAT
            passAll(tracker, player2, player1); // DECLARE_ATTACKERS
            passAll(tracker, player2, player1); // DECLARE_BLOCKERS
            passAll(tracker, player2, player1); // COMBAT_DAMAGE
            passAll(tracker, player2, player1); // END_OF_COMBAT
            passAll(tracker, player2, player1); // MAIN 2
            passAll(tracker, player2, player1); // END

            // Turn 3 is player1's turn again
            assertThat(tracker.activePlayer()).isEqualTo(player1);
            firedEvents.clear();

            // Pass through UPKEEP on turn 3
            passAll(tracker, player1, player2);

            // Turn 3 should also have DRAW step
            var turn3StepEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(turn3StepEvents.stream().anyMatch(s -> s.step() == Step.DRAW))
                    .isTrue();
        }

        @Test
        void multiplayerGameDoesNotSkipFirstDrawStep() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Clear events to see just what happens after upkeep
            firedEvents.clear();

            // Pass priority for upkeep (all 3 players)
            tracker.passPriority(player1);
            tracker.passPriority(player2);
            tracker.passPriority(player3);

            // In multiplayer, draw step is NOT skipped
            var stepStartedEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            // Should see DRAW step
            assertThat(stepStartedEvents.stream().anyMatch(s -> s.step() == Step.DRAW))
                    .isTrue();
        }

        @Test
        void fullTurnEventOrder() {
            // Use 3 players so draw step is never skipped (multiplayer rule)
            var tracker = createTracker(List.of(player1, player2, player3));

            // Start the game - turn 1 events will be fired up to UPKEEP waiting for priority
            tracker.startGame(player1);

            // Expected full turn event order (turn 1 with draw step)
            List<Class<?>> expectedOrder = List.of(
                    TurnStartedEvent.class,
                    // BEGINNING phase
                    PhaseStartedEvent.class, // BEGINNING
                    StepStartedEvent.class, // UNTAP
                    StepEndedEvent.class, // UNTAP
                    StepStartedEvent.class, // UPKEEP
                    StepEndedEvent.class, // UPKEEP
                    StepStartedEvent.class, // DRAW
                    StepEndedEvent.class, // DRAW
                    PhaseEndedEvent.class, // BEGINNING
                    // First MAIN phase
                    PhaseStartedEvent.class, // MAIN
                    PhaseEndedEvent.class, // MAIN
                    // COMBAT phase
                    PhaseStartedEvent.class, // COMBAT
                    StepStartedEvent.class, // BEGINNING_OF_COMBAT
                    StepEndedEvent.class, // BEGINNING_OF_COMBAT
                    StepStartedEvent.class, // DECLARE_ATTACKERS
                    StepEndedEvent.class, // DECLARE_ATTACKERS
                    StepStartedEvent.class, // DECLARE_BLOCKERS
                    StepEndedEvent.class, // DECLARE_BLOCKERS
                    StepStartedEvent.class, // COMBAT_DAMAGE
                    StepEndedEvent.class, // COMBAT_DAMAGE
                    StepStartedEvent.class, // END_OF_COMBAT
                    StepEndedEvent.class, // END_OF_COMBAT
                    PhaseEndedEvent.class, // COMBAT
                    // Second MAIN phase
                    PhaseStartedEvent.class, // MAIN
                    PhaseEndedEvent.class, // MAIN
                    // ENDING phase
                    PhaseStartedEvent.class, // ENDING
                    StepStartedEvent.class, // END
                    StepEndedEvent.class, // END
                    StepStartedEvent.class, // CLEANUP
                    StepEndedEvent.class, // CLEANUP
                    PhaseEndedEvent.class, // ENDING
                    TurnEndedEvent.class);

            // Complete turn 1 (player1's turn, APNAP order: player1, player2, player3)
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // The firedEvents should contain turn 1 events plus the start of turn 2
            // We only verify the first 32 events (turn 1)
            assertThat(firedEvents.size()).isGreaterThanOrEqualTo(expectedOrder.size());
            for (var i = 0; i < expectedOrder.size(); i++) {
                assertThat(firedEvents.get(i))
                        .as(
                                "Event at index %d should be %s but was %s",
                                i, expectedOrder.get(i).getSimpleName(), firedEvents.get(i))
                        .isInstanceOf(expectedOrder.get(i));
            }
        }
    }

    // ========== Extra Turn Tests ==========

    @Nested
    class AddExtraTurn {

        @Test
        void extraTurnIsInsertedAfterCurrentTurn() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Add extra turn for player1
            tracker.addExtraTurn(player1);

            // Complete turn 1
            completeTurn(tracker, player1, player2);

            // Should now be player1's extra turn (turn 2), not player2's turn
            assertThat(tracker.activePlayer()).isEqualTo(player1);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void multipleExtraTurnsAreStackedLIFO() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Add extra turns: player2, then player3
            tracker.addExtraTurn(player2);
            tracker.addExtraTurn(player3);

            // Complete turn 1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2 should be player3 (LIFO - last added, first taken)
            assertThat(tracker.activePlayer()).isEqualTo(player3);

            // Complete player3's extra turn
            completeTurnMultiplayer(tracker, player3, player1, player2);

            // Turn 3 should be player2 (the earlier extra turn)
            assertThat(tracker.activePlayer()).isEqualTo(player2);

            // Complete player2's extra turn
            completeTurnMultiplayer(tracker, player2, player3, player1);

            // Turn 4 should resume normal order (player2 was turn 1's active, so next is player2)
            // Wait, after turn 1 (player1), normal next would be player2
            // But we had extra turns, so after all extra turns, next normal is player2
            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void canAddExtraTurnForAnyPlayer() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Player1 gets an extra turn
            tracker.addExtraTurn(player2);

            // Complete turn 1
            completeTurn(tracker, player1, player2);

            // Turn 2 is player2's extra turn
            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void extraTurnHasCorrectTurnNumber() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            tracker.addExtraTurn(player1);

            // Complete turn 1
            completeTurn(tracker, player1, player2);

            // Extra turn is turn 2
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void normalTurnOrderResumesAfterExtraTurns() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            tracker.addExtraTurn(player1);

            // Turn 1: player1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2: player1 (extra turn)
            assertThat(tracker.activePlayer()).isEqualTo(player1);
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 3: player2 (normal order resumes)
            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void skippedExtraTurnDoesNotAffectNormalTurnOrder() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Add extra turn for player2, then skip it
            tracker.addExtraTurn(player2);
            tracker.skipNextTurn(player2);

            // Complete turn 1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // The extra turn for player2 should be skipped, resuming normal order
            // Normal order after turn 1 (player1) is player2
            assertThat(tracker.activePlayer()).isEqualTo(player2);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }
    }

    // ========== Skip Tests ==========

    @Nested
    class SkipNextTurn {

        @Test
        void skipsPlayersNextTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player2 should skip their next turn
            tracker.skipNextTurn(player2);

            // Complete turn 1 (player1's turn)
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Should skip player2, go to player3
            assertThat(tracker.activePlayer()).isEqualTo(player3);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void multipleSkipsStack() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // Player2 should skip their next TWO turns
            tracker.skipNextTurn(player2);
            tracker.skipNextTurn(player2);

            // Complete turn 1
            completeTurn(tracker, player1, player2);

            // Turn 2 skips player2, goes to player1
            assertThat(tracker.activePlayer()).isEqualTo(player1);

            // Complete turn 2
            completeTurn(tracker, player1, player2);

            // Turn 3 skips player2 again, goes to player1
            assertThat(tracker.activePlayer()).isEqualTo(player1);

            // Complete turn 3
            completeTurn(tracker, player1, player2);

            // Turn 4 is finally player2's turn
            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void skippedTurnDoesNotFireTurnStartedEvent() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            tracker.skipNextTurn(player2);

            // Complete turn 1 (turn 2 starts automatically after cleanup)
            completeTurn(tracker, player1, player2);

            // Turn 2 should be player1 (skipped player2)
            // Check all TurnStartedEvents: should have turn 1 (player1) and turn 2 (player1)
            // No TurnStartedEvent for player2 should have been fired
            var turnStartedEvents = firedEvents.stream()
                    .gather(instanceOf(TurnStartedEvent.class))
                    .toList();

            // Should have exactly 2 TurnStartedEvents: turn 1 and turn 2
            assertThat(turnStartedEvents).hasSize(2);
            assertThat(turnStartedEvents.get(0).activePlayer()).isEqualTo(player1);
            assertThat(turnStartedEvents.get(0).turnNumber()).isEqualTo(1);
            assertThat(turnStartedEvents.get(1).activePlayer()).isEqualTo(player1);
            assertThat(turnStartedEvents.get(1).turnNumber()).isEqualTo(2);

            // Verify no event for player2
            assertThat(turnStartedEvents.stream()
                            .noneMatch(e -> e.activePlayer().equals(player2)))
                    .isTrue();
        }
    }

    @Nested
    class SkipAllSteps {

        @Test
        void permanentlySkipsStepForPlayer() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player1 permanently skips draw steps
            tracker.skipAllSteps(player1, Step.DRAW);

            firedEvents.clear();

            // Pass through UPKEEP
            passAll(tracker, player1, player2, player3);

            // DRAW should be skipped, we should go directly to MAIN
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);

            // Complete turn 1 and turn 2
            completeTurnMultiplayerFromMain(tracker, player1, player2, player3);
            completeTurnMultiplayer(tracker, player2, player3, player1);

            // Turn 3 is player3's turn
            completeTurnMultiplayer(tracker, player3, player1, player2);

            firedEvents.clear();

            // Turn 4 is player1's turn again
            // Pass through UPKEEP
            passAll(tracker, player1, player2, player3);

            // DRAW should still be skipped
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
        }

        @Test
        void onlyAffectsSpecifiedPlayer() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player1 permanently skips draw steps
            tracker.skipAllSteps(player1, Step.DRAW);

            // Complete turn 1 (player1's draw skipped)
            passAll(tracker, player1, player2, player3); // UPKEEP
            // DRAW skipped - now in MAIN
            completeTurnMultiplayerFromMain(tracker, player1, player2, player3);

            firedEvents.clear();

            // Turn 2 is player2's turn - DRAW should NOT be skipped
            passAll(tracker, player2, player3, player1); // UPKEEP

            var stepEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(stepEvents.stream().anyMatch(e -> e.step() == Step.DRAW)).isTrue();
        }
    }

    @Nested
    class SkipNextOccurrence {

        @Test
        void skipsNextOccurrenceOfStepOnce() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player1 skips their next draw step (one time)
            tracker.skipNextOccurrence(player1, Step.DRAW);

            firedEvents.clear();

            // Pass through UPKEEP
            passAll(tracker, player1, player2, player3);

            // DRAW should be skipped this turn
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);

            // Complete turn 1 and cycle back to player1
            completeTurnMultiplayerFromMain(tracker, player1, player2, player3);
            completeTurnMultiplayer(tracker, player2, player3, player1);
            completeTurnMultiplayer(tracker, player3, player1, player2);

            firedEvents.clear();

            // Turn 4 is player1's turn again
            passAll(tracker, player1, player2, player3); // UPKEEP

            // DRAW should NOT be skipped this time
            var stepEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(stepEvents.stream().anyMatch(e -> e.step() == Step.DRAW)).isTrue();
        }

        @Test
        void multipleSkipsStack() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player1 skips their next TWO draw steps
            tracker.skipNextOccurrence(player1, Step.DRAW);
            tracker.skipNextOccurrence(player1, Step.DRAW);

            // Turn 1: DRAW skipped
            passAll(tracker, player1, player2, player3);
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            completeTurnMultiplayerFromMain(tracker, player1, player2, player3);

            // Turn 2 and 3: other players
            completeTurnMultiplayer(tracker, player2, player3, player1);
            completeTurnMultiplayer(tracker, player3, player1, player2);

            // Turn 4: player1 again, DRAW still skipped
            passAll(tracker, player1, player2, player3);
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            completeTurnMultiplayerFromMain(tracker, player1, player2, player3);

            // Turn 5 and 6: other players
            completeTurnMultiplayer(tracker, player2, player3, player1);
            completeTurnMultiplayer(tracker, player3, player1, player2);

            firedEvents.clear();

            // Turn 7: player1 again, DRAW should NOT be skipped
            passAll(tracker, player1, player2, player3);

            var stepEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(stepEvents.stream().anyMatch(e -> e.step() == Step.DRAW)).isTrue();
        }
    }

    @Nested
    class SkipPhaseNextTurn {

        @Test
        void skipsPhaseOnPlayersNextTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Player2 skips combat on their next turn
            tracker.skipPhaseNextTurn(player2, Phase.COMBAT);

            // Complete turn 1
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Turn 2 is player2's turn
            assertThat(tracker.activePlayer()).isEqualTo(player2);

            firedEvents.clear();

            // Pass through BEGINNING and MAIN 1
            passAll(tracker, player2, player3, player1); // UPKEEP
            passAll(tracker, player2, player3, player1); // DRAW
            passAll(tracker, player2, player3, player1); // MAIN 1

            // Should skip COMBAT and go to MAIN 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(2);
        }

        @Test
        void onlyAffectsNextTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            tracker.skipPhaseNextTurn(player2, Phase.COMBAT);

            // Complete turn 1, turn 2 (player2 with skipped combat), turn 3, turn 4, turn 5
            completeTurnMultiplayer(tracker, player1, player2, player3);
            completeTurnMultiplayerNoCombat(tracker, player2, player3, player1);
            completeTurnMultiplayer(tracker, player3, player1, player2);

            firedEvents.clear();

            // Turn 4 is player1's turn, turn 5 is player2's turn again
            completeTurnMultiplayer(tracker, player1, player2, player3);

            // Player2's next turn should have combat
            passAll(tracker, player2, player3, player1); // UPKEEP
            passAll(tracker, player2, player3, player1); // DRAW
            passAll(tracker, player2, player3, player1); // MAIN 1

            // Should be in COMBAT, not MAIN 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);
        }
    }

    @Nested
    class SkipPhaseThisTurn {

        @Test
        void skipsPhaseInCurrentTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Skip combat this turn
            tracker.skipPhaseThisTurn(Phase.COMBAT);

            // Pass through BEGINNING and MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1

            // Should skip COMBAT and go to MAIN 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(2);
        }

        @Test
        void onlyAffectsCurrentTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            tracker.skipPhaseThisTurn(Phase.COMBAT);

            // Complete turn 1 (combat skipped)
            completeTurnMultiplayerNoCombat(tracker, player1, player2, player3);

            // Turn 2 should have combat
            passAll(tracker, player2, player3, player1); // UPKEEP
            passAll(tracker, player2, player3, player1); // DRAW
            passAll(tracker, player2, player3, player1); // MAIN 1

            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);
        }
    }

    // ========== End Turn Early Tests ==========

    @Nested
    class EndTurnEarly {

        @Test
        void endsCurrentTurnAndStartsNext() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // In the middle of turn 1
            assertThat(tracker.currentTurn().number()).isEqualTo(1);
            assertThat(tracker.activePlayer()).isEqualTo(player1);

            // End turn early
            tracker.endTurnEarly();

            // Should now be turn 2 with player2
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
            assertThat(tracker.activePlayer()).isEqualTo(player2);
        }

        @Test
        void goesToCleanupBeforeEndingTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            firedEvents.clear();

            tracker.endTurnEarly();

            // Should fire cleanup step events
            var stepStartedEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .toList();

            assertThat(stepStartedEvents.stream().anyMatch(e -> e.step() == Step.CLEANUP))
                    .isTrue();
        }

        @Test
        void firesEndedEventsForCurrentPhaseAndStep() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // In UPKEEP step, BEGINNING phase
            assertThat(tracker.currentPhase()).isEqualTo(Phase.BEGINNING);
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);

            firedEvents.clear();

            tracker.endTurnEarly();

            // Should fire StepEndedEvent for UPKEEP and PhaseEndedEvent for BEGINNING
            var stepEndedEvents = firedEvents.stream()
                    .gather(instanceOf(StepEndedEvent.class))
                    .toList();

            var phaseEndedEvents = firedEvents.stream()
                    .gather(instanceOf(PhaseEndedEvent.class))
                    .toList();

            assertThat(stepEndedEvents.stream().anyMatch(e -> e.step() == Step.UPKEEP))
                    .isTrue();
            assertThat(phaseEndedEvents.stream().anyMatch(e -> e.phase() == Phase.BEGINNING))
                    .isTrue();
        }

        @Test
        void firesTurnEndedEvent() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            firedEvents.clear();

            tracker.endTurnEarly();

            var turnEndedEvents = firedEvents.stream()
                    .gather(instanceOf(TurnEndedEvent.class))
                    .toList();

            assertThat(turnEndedEvents).hasSize(1);
            assertThat(turnEndedEvents.getFirst().turnNumber()).isEqualTo(1);
            assertThat(turnEndedEvents.getFirst().activePlayer()).isEqualTo(player1);
        }

        @Test
        void firesTurnStartedEventForNextTurn() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            firedEvents.clear();

            tracker.endTurnEarly();

            var turnStartedEvents = firedEvents.stream()
                    .gather(instanceOf(TurnStartedEvent.class))
                    .toList();

            assertThat(turnStartedEvents).hasSize(1);
            assertThat(turnStartedEvents.getFirst().turnNumber()).isEqualTo(2);
            assertThat(turnStartedEvents.getFirst().activePlayer()).isEqualTo(player2);
        }

        @Test
        void clearsTheStack() {
            var tracker = createTracker(List.of(player1, player2, player3));
            when(stack.isEmpty()).thenReturn(false, false, true); // Two items on stack

            tracker.startGame(player1);
            tracker.endTurnEarly();

            // Stack.pop() should have been called to clear items
            verify(stack, times(2)).pop();
        }

        @Test
        void canBeCalledDuringMainPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);

            firedEvents.clear();

            tracker.endTurnEarly();

            // Should end the turn
            assertThat(tracker.currentTurn().number()).isEqualTo(2);

            // Should have fired PhaseEndedEvent for MAIN
            var phaseEndedEvents = firedEvents.stream()
                    .gather(instanceOf(PhaseEndedEvent.class))
                    .toList();

            assertThat(phaseEndedEvents.stream().anyMatch(e -> e.phase() == Phase.MAIN))
                    .isTrue();
        }

        @Test
        void canBeCalledDuringCombat() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to COMBAT
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1

            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);
            assertThat(tracker.currentStep()).isEqualTo(Step.BEGINNING_OF_COMBAT);

            tracker.endTurnEarly();

            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void skipsRemainingPhasesAndSteps() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            firedEvents.clear();

            tracker.endTurnEarly();

            // After ending early from UPKEEP, we should NOT see any MAIN or COMBAT phases
            // for turn 1 (only ENDING phase for cleanup)
            var phaseStartedEvents = firedEvents.stream()
                    .gather(instanceOf(PhaseStartedEvent.class))
                    .toList();

            // Should only see ENDING (for cleanup) and BEGINNING (for turn 2)
            var turn1Phases = phaseStartedEvents.stream()
                    .takeWhile(e -> {
                        // Find where turn 2 starts
                        var idx = firedEvents.indexOf(e);
                        return firedEvents.subList(0, idx).stream()
                                .noneMatch(ev -> ev instanceof TurnStartedEvent ts && ts.turnNumber() == 2);
                    })
                    .toList();

            assertThat(turn1Phases.stream().noneMatch(e -> e.phase() == Phase.MAIN))
                    .isTrue();
            assertThat(turn1Phases.stream().noneMatch(e -> e.phase() == Phase.COMBAT))
                    .isTrue();
            assertThat(turn1Phases.stream().anyMatch(e -> e.phase() == Phase.ENDING))
                    .isTrue();
        }
    }

    // ========== Stack Resolution Tests ==========

    @Nested
    class StackResolution {

        @Test
        void resolvesTopOfStackWhenAllPlayersPassWithNonEmptyStack() {
            var tracker = createTracker(List.of(player1, player2));
            // Stack has one item, then becomes empty after resolve
            when(stack.isEmpty()).thenReturn(false, true);

            tracker.startGame(player1);

            // In UPKEEP, both players pass
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Stack.resolve() should have been called once
            verify(stack, times(1)).resolve();
        }

        @Test
        void activePlayerGetsPriorityAfterResolution() {
            var tracker = createTracker(List.of(player1, player2));
            // Stack has one item, then becomes empty
            when(stack.isEmpty()).thenReturn(false, true);

            tracker.startGame(player1);

            // Both players pass, stack resolves
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // After resolution, active player should have priority again
            assertThat(tracker.hasPriority(player1)).isTrue();
        }

        @Test
        void resolvesMultipleItemsOneAtATime() {
            var tracker = createTracker(List.of(player1, player2));
            // Stack has three items
            when(stack.isEmpty()).thenReturn(false, false, false, true);

            tracker.startGame(player1);

            // First round of passing - resolves first item
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Second round of passing - resolves second item
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Third round of passing - resolves third item
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Stack.resolve() should have been called three times
            verify(stack, times(3)).resolve();

            // Now stack is empty, fourth pass should advance step
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Should have advanced from UPKEEP
            assertThat(tracker.currentStep()).isNotEqualTo(Step.UPKEEP);
        }
    }

    // ========== SBA Tests ==========

    @Nested
    class StateBasedActions {

        @Test
        void checksStateBasedActionsBeforeGrantingPriority() {
            var sbaChecker = mock(StateBasedAction.class);
            when(sbaChecker.checkAndApply()).thenReturn(false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker));
            tracker.startGame(player1);

            // SBA checker should have been called during startup
            verify(sbaChecker).checkAndApply();
        }

        @Test
        void appliesSbasRepeatedly() {
            var sbaChecker = mock(StateBasedAction.class);
            // Return true twice, then false (SBAs applied twice, then done)
            when(sbaChecker.checkAndApply()).thenReturn(true, true, false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker));
            tracker.startGame(player1);

            // SBA checker should have been called 3 times (true, true, false)
            verify(sbaChecker, times(3)).checkAndApply();
        }

        @Test
        void checksMultipleSbaCheckers() {
            var sbaChecker1 = mock(StateBasedAction.class);
            var sbaChecker2 = mock(StateBasedAction.class);
            when(sbaChecker1.checkAndApply()).thenReturn(false);
            when(sbaChecker2.checkAndApply()).thenReturn(false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker1, sbaChecker2));
            tracker.startGame(player1);

            // Both checkers should be called
            verify(sbaChecker1).checkAndApply();
            verify(sbaChecker2).checkAndApply();
        }

        @Test
        void repeatsSbaLoopWhenAnySbaApplied() {
            var sbaChecker1 = mock(StateBasedAction.class);
            var sbaChecker2 = mock(StateBasedAction.class);
            // Checker1 returns true once, then false
            // Checker2 always returns false
            when(sbaChecker1.checkAndApply()).thenReturn(true, false, false);
            when(sbaChecker2.checkAndApply()).thenReturn(false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker1, sbaChecker2));
            tracker.startGame(player1);

            // First pass: checker1=true, checker2=false -> loop again
            // Second pass: checker1=false, checker2=false -> done
            verify(sbaChecker1, times(2)).checkAndApply();
            verify(sbaChecker2, times(2)).checkAndApply();
        }
    }

    // ========== Cleanup Step Tests ==========

    @Nested
    class CleanupStepBehavior {

        @Test
        void endTurnEarlyRunsCleanupLoopWithSbas() {
            var sbaChecker = mock(StateBasedAction.class);
            // Start with false during startup, then true during endTurnEarly cleanup, then false
            // grantPriority calls checkStateBasedActions() once during startup
            // runCleanupLoop calls checkStateBasedActions() once, then grantPriority() calls it again
            // Then the second cleanup iteration calls checkStateBasedActions() which returns false
            when(sbaChecker.checkAndApply()).thenReturn(false, true, false, false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker));
            tracker.startGame(player1);

            firedEvents.clear();

            // End turn early - this calls runCleanupLoop()
            tracker.endTurnEarly();

            // Should see multiple CLEANUP step events because SBAs were applied
            var cleanupStartEvents = firedEvents.stream()
                    .gather(instanceOf(StepStartedEvent.class))
                    .filter(e -> e.step() == Step.CLEANUP)
                    .toList();

            // At least 2 cleanup steps: one where SBAs applied, one after
            assertThat(cleanupStartEvents.size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void normalCleanupStepChecksForSbas() {
            var sbaChecker = mock(StateBasedAction.class);
            // No SBAs applied
            when(sbaChecker.checkAndApply()).thenReturn(false);

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker));
            tracker.startGame(player1);

            // Go through entire turn to reach cleanup (which has no priority)
            completeTurn(tracker, player1, player2);

            // SBA checker should have been called during cleanup
            verify(sbaChecker, atLeast(10)).checkAndApply();
        }

        @Test
        void normalCleanupGrantsPriorityWhenSbasApplied() {
            var sbaChecker = mock(StateBasedAction.class);
            // Count calls and return true only during normal cleanup step
            // Call sequence: UPKEEP(1), DRAW(2), MAIN1(3), DECLARE_ATTACKERS(4),
            // DECLARE_BLOCKERS(5), COMBAT_DAMAGE(6), END_OF_COMBAT(7), MAIN2(8), END_STEP(9),
            // CLEANUP-handleCleanupStep(10) -> true, then CLEANUP-grantPriority(11) -> false
            var callCount = new int[] {0};
            when(sbaChecker.checkAndApply()).thenAnswer(_ -> {
                callCount[0]++;
                // Return true on call 10 (handleCleanupStep during normal cleanup)
                return callCount[0] == 10;
            });

            var tracker = createTrackerWithSbaCheckers(List.of(player1, player2), List.of(sbaChecker));
            tracker.startGame(player1);
            firedEvents.clear();

            // Go through entire turn up to end step, then pass to enter cleanup
            passAll(tracker, player1, player2); // UPKEEP -> DRAW
            passAll(tracker, player1, player2); // DRAW -> MAIN1
            passAll(tracker, player1, player2); // MAIN1 -> COMBAT (DECLARE_ATTACKERS)
            passAll(tracker, player1, player2); // DECLARE_ATTACKERS -> DECLARE_BLOCKERS
            passAll(tracker, player1, player2); // DECLARE_BLOCKERS -> COMBAT_DAMAGE
            passAll(tracker, player1, player2); // COMBAT_DAMAGE -> END_OF_COMBAT
            passAll(tracker, player1, player2); // END_OF_COMBAT -> MAIN2
            passAll(tracker, player1, player2); // MAIN2 -> ENDING (END_STEP)
            passAll(tracker, player1, player2); // END_STEP -> CLEANUP (SBAs trigger!)

            // After SBAs are applied during cleanup, priority should be granted
            // and we should still be in cleanup
            assertThat(tracker.currentStep()).isEqualTo(Step.CLEANUP);
            assertThat(tracker.hasPriority(player1)).isTrue();
        }
    }

    // ========== Insertion Tests ==========

    @Nested
    class InsertPhaseAfterCurrent {

        @Test
        void insertsPhaseAfterCurrentPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Complete BEGINNING, in MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // Now in MAIN 1
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(1);

            // Insert an extra COMBAT phase after current (MAIN 1)
            tracker.insertPhaseAfterCurrent(Phase.COMBAT);

            firedEvents.clear();

            // Pass through MAIN 1
            passAll(tracker, player1, player2, player3);

            // Should be in the inserted COMBAT phase
            assertThat(tracker.currentPhase()).isEqualTo(Phase.COMBAT);
        }

        @Test
        void insertedPhaseHasAllItsSteps() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // Insert COMBAT after MAIN 1
            tracker.insertPhaseAfterCurrent(Phase.COMBAT);

            firedEvents.clear();

            // Pass through MAIN 1
            passAll(tracker, player1, player2, player3);

            // Should go through all combat steps
            var combatSteps = List.of(
                    Step.BEGINNING_OF_COMBAT,
                    Step.DECLARE_ATTACKERS,
                    Step.DECLARE_BLOCKERS,
                    Step.COMBAT_DAMAGE,
                    Step.END_OF_COMBAT);

            for (var expectedStep : combatSteps) {
                assertThat(tracker.currentStep()).isEqualTo(expectedStep);
                passAll(tracker, player1, player2, player3);
            }
        }

        @Test
        void multipleInsertionsAreProcessedInOrder() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // Insert COMBAT, then another COMBAT
            // Order should be: MAIN1 -> first inserted COMBAT -> second inserted COMBAT -> normal COMBAT
            tracker.insertPhaseAfterCurrent(Phase.COMBAT); // Will be second
            tracker.insertPhaseAfterCurrent(Phase.COMBAT); // Will be first (LIFO)

            firedEvents.clear();

            // Pass through MAIN 1
            passAll(tracker, player1, player2, player3);

            // Count combat phases
            var combatPhaseCount = 0;
            for (var i = 0; i < 20; i++) {
                if (tracker.currentPhase() == Phase.COMBAT) {
                    combatPhaseCount++;
                    // Pass through all combat steps
                    passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
                    passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
                    passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
                    passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
                    passAll(tracker, player1, player2, player3); // END_OF_COMBAT
                } else if (tracker.currentPhase() == Phase.MAIN) {
                    break; // Reached post-combat main
                }
            }

            // Should have 3 combat phases: 2 inserted + 1 normal
            assertThat(combatPhaseCount).isEqualTo(3);
        }
    }

    @Nested
    class InsertStepAfterCurrent {

        @Test
        void insertsStepAfterCurrentStep() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // In UPKEEP
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);

            // Insert another UPKEEP after current
            tracker.insertStepAfterCurrent(Step.UPKEEP);

            firedEvents.clear();

            // Pass through current UPKEEP
            passAll(tracker, player1, player2, player3);

            // Should be in the inserted UPKEEP
            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(2);
        }

        @Test
        void insertedStepHasPriorityIfNormalStepDoes() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Insert an extra DRAW step
            tracker.insertStepAfterCurrent(Step.DRAW);

            // Pass through UPKEEP
            passAll(tracker, player1, player2, player3);

            // Now in DRAW (normal)
            passAll(tracker, player1, player2, player3);

            // Should be in inserted DRAW, active player has priority
            assertThat(tracker.currentStep()).isEqualTo(Step.DRAW);
            assertThat(tracker.hasPriority(player1)).isTrue();
        }

        @Test
        void canInsertCleanupStepToLoopCleanup() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to END step (in ENDING phase)
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW
            passAll(tracker, player1, player2, player3); // MAIN 1
            passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
            passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
            passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
            passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
            passAll(tracker, player1, player2, player3); // END_OF_COMBAT
            passAll(tracker, player1, player2, player3); // MAIN 2

            // Now in END step
            assertThat(tracker.currentStep()).isEqualTo(Step.END);

            // Insert another END step after current
            tracker.insertStepAfterCurrent(Step.END);

            firedEvents.clear();

            // Pass through current END
            passAll(tracker, player1, player2, player3);

            // Should be in inserted END step
            assertThat(tracker.currentStep()).isEqualTo(Step.END);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(2);
        }
    }

    @Nested
    class InsertedPhaseOccurrence {

        @Test
        void tracksOccurrencesOfSamePhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(1);

            // Pass through MAIN 1 and COMBAT
            passAll(tracker, player1, player2, player3); // MAIN 1
            passAll(tracker, player1, player2, player3); // BEGINNING_OF_COMBAT
            passAll(tracker, player1, player2, player3); // DECLARE_ATTACKERS
            passAll(tracker, player1, player2, player3); // DECLARE_BLOCKERS
            passAll(tracker, player1, player2, player3); // COMBAT_DAMAGE
            passAll(tracker, player1, player2, player3); // END_OF_COMBAT

            // Now in MAIN 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(2);
        }

        @Test
        void insertedPhasesIncrementOccurrence() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Go to MAIN 1
            passAll(tracker, player1, player2, player3); // UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // Insert extra MAIN phase
            tracker.insertPhaseAfterCurrent(Phase.MAIN);

            // Pass through MAIN 1
            passAll(tracker, player1, player2, player3);

            // Inserted MAIN should be occurrence 2
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentPhaseOccurrence()).isEqualTo(2);
        }
    }

    @Nested
    class InsertedStepOccurrence {

        @Test
        void tracksOccurrencesOfSameStepInPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1);

            // Insert another UPKEEP
            tracker.insertStepAfterCurrent(Step.UPKEEP);

            passAll(tracker, player1, player2, player3);

            assertThat(tracker.currentStep()).isEqualTo(Step.UPKEEP);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(2);
        }

        @Test
        void occurrenceResetsOnNewPhase() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Insert extra UPKEEP
            tracker.insertStepAfterCurrent(Step.UPKEEP);
            passAll(tracker, player1, player2, player3); // First UPKEEP
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(2);

            passAll(tracker, player1, player2, player3); // Second UPKEEP
            passAll(tracker, player1, player2, player3); // DRAW

            // Now in MAIN phase (step is null for main)
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(0);

            // Go to COMBAT
            passAll(tracker, player1, player2, player3); // MAIN

            // In BEGINNING_OF_COMBAT, occurrence should be 1
            assertThat(tracker.currentStep()).isEqualTo(Step.BEGINNING_OF_COMBAT);
            assertThat(tracker.currentStepOccurrenceInPhase()).isEqualTo(1);
        }
    }

    // ========== Player Leaving Tests ==========

    @Nested
    class PlayerLeavingGame {

        @Test
        void prioritySkipsPlayerWhoLeftTheGame() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player1 has priority
            assertThat(tracker.hasPriority(player1)).isTrue();

            // player2 leaves the game
            playerLeaves(player2);

            // player1 passes priority
            tracker.passPriority(player1);

            // Should skip player2, player3 should have priority
            assertThat(tracker.hasPriority(player3)).isTrue();
            assertThat(tracker.hasPriority(player2)).isFalse();
        }

        @Test
        void allPlayersPassedOnlyCountsPlayersInGame() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player3 leaves the game
            playerLeaves(player3);

            // player1 and player2 pass (player3 has left, so only 2 players need to pass)
            tracker.passPriority(player1);
            tracker.passPriority(player2);

            // Should have advanced (only player1 and player2 need to pass)
            // In 2-player game turn 1 on multiplayer rules, DRAW is not skipped (3 players originally)
            assertThat(tracker.currentStep()).isEqualTo(Step.DRAW);
        }

        @Test
        void turnSkipsPlayerWhoLeftTheGame() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player2 leaves during turn 1 (before their turn comes)
            playerLeaves(player2);

            // Complete turn 1 (only player1 and player3 pass, player2 left)
            passAllInGame(tracker, player1, player3); // UPKEEP
            passAllInGame(tracker, player1, player3); // DRAW
            passAllInGame(tracker, player1, player3); // MAIN 1
            passAllInGame(tracker, player1, player3); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player1, player3); // DECLARE_ATTACKERS
            passAllInGame(tracker, player1, player3); // DECLARE_BLOCKERS
            passAllInGame(tracker, player1, player3); // COMBAT_DAMAGE
            passAllInGame(tracker, player1, player3); // END_OF_COMBAT
            passAllInGame(tracker, player1, player3); // MAIN 2
            passAllInGame(tracker, player1, player3); // END

            // Turn 2 should skip player2 and go to player3
            assertThat(tracker.activePlayer()).isEqualTo(player3);
            assertThat(tracker.currentTurn().number()).isEqualTo(2);
        }

        @Test
        void extraTurnDiscardedIfPlayerLeft() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Add extra turn for player2
            tracker.addExtraTurn(player2);

            // player2 leaves the game
            playerLeaves(player2);

            // Complete turn 1 (only player1 and player3 pass)
            passAllInGame(tracker, player1, player3); // UPKEEP
            passAllInGame(tracker, player1, player3); // DRAW
            passAllInGame(tracker, player1, player3); // MAIN 1
            passAllInGame(tracker, player1, player3); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player1, player3); // DECLARE_ATTACKERS
            passAllInGame(tracker, player1, player3); // DECLARE_BLOCKERS
            passAllInGame(tracker, player1, player3); // COMBAT_DAMAGE
            passAllInGame(tracker, player1, player3); // END_OF_COMBAT
            passAllInGame(tracker, player1, player3); // MAIN 2
            passAllInGame(tracker, player1, player3); // END

            // Turn 2 should skip player2's extra turn and go to normal order
            // Normal order after player1 is player2, but player2 left
            // So it should be player3
            assertThat(tracker.activePlayer()).isEqualTo(player3);
        }

        @Test
        void multipleExtraTurnsSkippedForPlayerWhoLeft() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // Add extra turns for player2, then player3, then player2 again
            tracker.addExtraTurn(player2);
            tracker.addExtraTurn(player3);
            tracker.addExtraTurn(player2);

            // player2 leaves the game
            playerLeaves(player2);

            // Complete turn 1 (only player1 and player3 pass)
            passAllInGame(tracker, player1, player3); // UPKEEP
            passAllInGame(tracker, player1, player3); // DRAW
            passAllInGame(tracker, player1, player3); // MAIN 1
            passAllInGame(tracker, player1, player3); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player1, player3); // DECLARE_ATTACKERS
            passAllInGame(tracker, player1, player3); // DECLARE_BLOCKERS
            passAllInGame(tracker, player1, player3); // COMBAT_DAMAGE
            passAllInGame(tracker, player1, player3); // END_OF_COMBAT
            passAllInGame(tracker, player1, player3); // MAIN 2
            passAllInGame(tracker, player1, player3); // END

            // Turn 2 should be player3's extra turn (player2's extra turn discarded)
            assertThat(tracker.activePlayer()).isEqualTo(player3);

            // Complete player3's extra turn (only player1 and player3 pass)
            passAllInGame(tracker, player3, player1); // UPKEEP
            passAllInGame(tracker, player3, player1); // DRAW
            passAllInGame(tracker, player3, player1); // MAIN 1
            passAllInGame(tracker, player3, player1); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player3, player1); // DECLARE_ATTACKERS
            passAllInGame(tracker, player3, player1); // DECLARE_BLOCKERS
            passAllInGame(tracker, player3, player1); // COMBAT_DAMAGE
            passAllInGame(tracker, player3, player1); // END_OF_COMBAT
            passAllInGame(tracker, player3, player1); // MAIN 2
            passAllInGame(tracker, player3, player1); // END

            // Turn 3: player2's other extra turn should also be skipped
            // Normal turn order resumes: player3 (already had extra), normal next is player2
            // But player2 left, so player3
            assertThat(tracker.activePlayer()).isEqualTo(player3);
        }

        @Test
        void normalTurnOrderSkipsPlayerWhoLeft() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player2 leaves the game
            playerLeaves(player2);

            // Complete turn 1 (only player1 and player3 pass)
            passAllInGame(tracker, player1, player3); // UPKEEP
            passAllInGame(tracker, player1, player3); // DRAW
            passAllInGame(tracker, player1, player3); // MAIN 1
            passAllInGame(tracker, player1, player3); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player1, player3); // DECLARE_ATTACKERS
            passAllInGame(tracker, player1, player3); // DECLARE_BLOCKERS
            passAllInGame(tracker, player1, player3); // COMBAT_DAMAGE
            passAllInGame(tracker, player1, player3); // END_OF_COMBAT
            passAllInGame(tracker, player1, player3); // MAIN 2
            passAllInGame(tracker, player1, player3); // END

            // Turn 2 should be player3 (skipping player2)
            assertThat(tracker.activePlayer()).isEqualTo(player3);

            // Complete turn 2 (player3 is active, APNAP order is player3, player1, skip player2)
            passAllInGame(tracker, player3, player1); // UPKEEP
            passAllInGame(tracker, player3, player1); // DRAW
            passAllInGame(tracker, player3, player1); // MAIN 1
            passAllInGame(tracker, player3, player1); // BEGINNING_OF_COMBAT
            passAllInGame(tracker, player3, player1); // DECLARE_ATTACKERS
            passAllInGame(tracker, player3, player1); // DECLARE_BLOCKERS
            passAllInGame(tracker, player3, player1); // COMBAT_DAMAGE
            passAllInGame(tracker, player3, player1); // END_OF_COMBAT
            passAllInGame(tracker, player3, player1); // MAIN 2
            passAllInGame(tracker, player3, player1); // END

            // Turn 3 should be player1 (wrapping around, skipping player2)
            assertThat(tracker.activePlayer()).isEqualTo(player1);
        }

        @Test
        void priorityAdvancesPastMultiplePlayersWhoLeft() {
            var tracker = createTracker(List.of(player1, player2, player3));
            tracker.startGame(player1);

            // player2 and player3 both leave
            playerLeaves(player2);
            playerLeaves(player3);

            // player1 passes priority
            tracker.passPriority(player1);

            // Should wrap back to player1 (only remaining player)
            assertThat(tracker.hasPriority(player1)).isTrue();
        }

        @Test
        void twoPlayersOneLeftCanStillPassPriority() {
            var tracker = createTracker(List.of(player1, player2));
            tracker.startGame(player1);

            // player2 leaves the game
            playerLeaves(player2);

            // player1 passes priority (and is the only player)
            tracker.passPriority(player1);

            // Should advance (only player1 needs to pass)
            // In 2-player game turn 1, DRAW is skipped, so should go to MAIN
            assertThat(tracker.currentPhase()).isEqualTo(Phase.MAIN);
        }
    }

    // ========== Helper Methods ==========

    private void completeTurn(DefaultTurnTracker tracker, Player active, Player other) {
        if (tracker.currentTurn().number() == 1) {
            passAll(tracker, active, other); // UPKEEP (DRAW skipped in 2-player turn 1)
        } else {
            passAll(tracker, active, other); // UPKEEP
            passAll(tracker, active, other); // DRAW
        }
        passAll(tracker, active, other); // MAIN 1
        passAll(tracker, active, other); // BEGINNING_OF_COMBAT
        passAll(tracker, active, other); // DECLARE_ATTACKERS
        passAll(tracker, active, other); // DECLARE_BLOCKERS
        passAll(tracker, active, other); // COMBAT_DAMAGE
        passAll(tracker, active, other); // END_OF_COMBAT
        passAll(tracker, active, other); // MAIN 2
        passAll(tracker, active, other); // END
    }

    private void completeTurnMultiplayer(DefaultTurnTracker tracker, Player active, Player second, Player third) {
        passAll(tracker, active, second, third); // UPKEEP
        passAll(tracker, active, second, third); // DRAW
        passAll(tracker, active, second, third); // MAIN 1
        passAll(tracker, active, second, third); // BEGINNING_OF_COMBAT
        passAll(tracker, active, second, third); // DECLARE_ATTACKERS
        passAll(tracker, active, second, third); // DECLARE_BLOCKERS
        passAll(tracker, active, second, third); // COMBAT_DAMAGE
        passAll(tracker, active, second, third); // END_OF_COMBAT
        passAll(tracker, active, second, third); // MAIN 2
        passAll(tracker, active, second, third); // END
    }

    private void completeTurnMultiplayerFromMain(
            DefaultTurnTracker tracker, Player active, Player second, Player third) {
        passAll(tracker, active, second, third); // MAIN 1
        passAll(tracker, active, second, third); // BEGINNING_OF_COMBAT
        passAll(tracker, active, second, third); // DECLARE_ATTACKERS
        passAll(tracker, active, second, third); // DECLARE_BLOCKERS
        passAll(tracker, active, second, third); // COMBAT_DAMAGE
        passAll(tracker, active, second, third); // END_OF_COMBAT
        passAll(tracker, active, second, third); // MAIN 2
        passAll(tracker, active, second, third); // END
    }

    private void completeTurnMultiplayerNoCombat(
            DefaultTurnTracker tracker, Player active, Player second, Player third) {
        passAll(tracker, active, second, third); // UPKEEP
        passAll(tracker, active, second, third); // DRAW
        passAll(tracker, active, second, third); // MAIN 1
        // COMBAT skipped
        passAll(tracker, active, second, third); // MAIN 2
        passAll(tracker, active, second, third); // END
    }

    private void passAll(DefaultTurnTracker tracker, Player... players) {
        for (var player : players) {
            tracker.passPriority(player);
        }
    }

    /// Pass priority for the specified players (used when some players have left).
    private void passAllInGame(DefaultTurnTracker tracker, Player... players) {
        for (var player : players) {
            tracker.passPriority(player);
        }
    }
}
