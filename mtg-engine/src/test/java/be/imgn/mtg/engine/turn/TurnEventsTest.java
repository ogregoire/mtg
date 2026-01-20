package be.imgn.mtg.engine.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

class TurnEventsTest {

    private final Player mockPlayer = mock(Player.class);

    @Nested
    class TurnStartedEventTests {

        @Test
        void implementsEvent() {
            var event = new TurnStartedEvent(1, mockPlayer);
            assertThat(event).isInstanceOf(Event.class);
        }

        @Test
        void storesTurnNumber() {
            var event = new TurnStartedEvent(5, mockPlayer);
            assertThat(event.turnNumber()).isEqualTo(5);
        }

        @Test
        void storesActivePlayer() {
            var event = new TurnStartedEvent(1, mockPlayer);
            assertThat(event.activePlayer()).isEqualTo(mockPlayer);
        }

        @Test
        void equalityBasedOnFields() {
            var event1 = new TurnStartedEvent(1, mockPlayer);
            var event2 = new TurnStartedEvent(1, mockPlayer);
            assertThat(event1).isEqualTo(event2);
        }
    }

    @Nested
    class TurnEndedEventTests {

        @Test
        void implementsEvent() {
            var event = new TurnEndedEvent(1, mockPlayer);
            assertThat(event).isInstanceOf(Event.class);
        }

        @Test
        void storesTurnNumber() {
            var event = new TurnEndedEvent(3, mockPlayer);
            assertThat(event.turnNumber()).isEqualTo(3);
        }

        @Test
        void storesActivePlayer() {
            var event = new TurnEndedEvent(1, mockPlayer);
            assertThat(event.activePlayer()).isEqualTo(mockPlayer);
        }
    }

    @Nested
    class PhaseEventsTest {

        @Test
        void phaseStartedStoresType() {
            var event = new PhaseStartedEvent(PhaseType.COMBAT, 1);
            assertThat(event.phase()).isEqualTo(PhaseType.COMBAT);
        }

        @Test
        void phaseStartedStoresOccurrence() {
            var event = new PhaseStartedEvent(PhaseType.MAIN, 2);
            assertThat(event.occurrence()).isEqualTo(2);
        }

        @Test
        void phaseEndedStoresType() {
            var event = new PhaseEndedEvent(PhaseType.BEGINNING, 1);
            assertThat(event.phase()).isEqualTo(PhaseType.BEGINNING);
        }

        @Test
        void phaseEndedStoresOccurrence() {
            var event = new PhaseEndedEvent(PhaseType.MAIN, 2);
            assertThat(event.occurrence()).isEqualTo(2);
        }
    }

    @Nested
    class StepEventsTest {

        @Test
        void stepStartedStoresType() {
            var event = new StepStartedEvent(StepType.UPKEEP, 1);
            assertThat(event.step()).isEqualTo(StepType.UPKEEP);
        }

        @Test
        void stepStartedStoresOccurrence() {
            var event = new StepStartedEvent(StepType.COMBAT_DAMAGE, 2);
            assertThat(event.occurrence()).isEqualTo(2);
        }

        @Test
        void stepEndedStoresType() {
            var event = new StepEndedEvent(StepType.DRAW, 1);
            assertThat(event.step()).isEqualTo(StepType.DRAW);
        }

        @Test
        void stepEndedStoresOccurrence() {
            var event = new StepEndedEvent(StepType.CLEANUP, 3);
            assertThat(event.occurrence()).isEqualTo(3);
        }
    }
}
