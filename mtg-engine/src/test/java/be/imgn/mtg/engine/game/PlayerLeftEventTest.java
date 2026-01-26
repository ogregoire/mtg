package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerLeftEventTest {

    @Nested
    class Construction {

        @Test
        void createsEventWithPlayerAndReason() {
            var player = mock(Player.class);
            var event = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.WON);

            assertThat(event.player()).isEqualTo(player);
            assertThat(event.reason()).isEqualTo(PlayerLeftEvent.Reason.WON);
        }
    }

    @Nested
    class ReasonEnum {

        @Test
        void hasWonReason() {
            assertThat(PlayerLeftEvent.Reason.WON).isNotNull();
        }

        @Test
        void hasLostReason() {
            assertThat(PlayerLeftEvent.Reason.LOST).isNotNull();
        }

        @Test
        void hasDrawReason() {
            assertThat(PlayerLeftEvent.Reason.DRAW).isNotNull();
        }

        @Test
        void hasConcededReason() {
            assertThat(PlayerLeftEvent.Reason.CONCEDED).isNotNull();
        }

        @Test
        void hasExactlyFourReasons() {
            var reasons = PlayerLeftEvent.Reason.values();

            assertThat(reasons).hasSize(4);
        }
    }

    @Nested
    class RecordEquality {

        @Test
        void equalEventsAreEqual() {
            var player = mock(Player.class);
            var event1 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.WON);
            var event2 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.WON);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void differentReasonsAreNotEqual() {
            var player = mock(Player.class);
            var event1 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.WON);
            var event2 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.LOST);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentPlayersAreNotEqual() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);
            var event1 = new PlayerLeftEvent(player1, PlayerLeftEvent.Reason.CONCEDED);
            var event2 = new PlayerLeftEvent(player2, PlayerLeftEvent.Reason.CONCEDED);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void hashCodeIsConsistent() {
            var player = mock(Player.class);
            var event1 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.DRAW);
            var event2 = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.DRAW);

            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }
    }

    @Nested
    class ToString {

        @Test
        void hasDescriptiveToString() {
            var player = mock(Player.class);
            var event = new PlayerLeftEvent(player, PlayerLeftEvent.Reason.WON);

            var str = event.toString();

            assertThat(str).contains("PlayerLeftEvent");
            assertThat(str).contains("WON");
        }
    }
}
