package be.imgn.mtg.engine.object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;

class TapEventTest {

    @Nested
    class TappingEvent {

        @Test
        void isTappingReturnsTrueForTapEvent() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, true);

            assertThat(event.isTapping()).isTrue();
            assertThat(event.isUntapping()).isFalse();
        }

        @Test
        void recordsCorrectPermanent() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, true);

            assertThat(event.permanent()).isSameAs(permanent);
        }

        @Test
        void affectedPlayerIsPermanentController() {
            var player = mock(Player.class);
            var permanent = mock(Permanent.class);
            when(permanent.controller()).thenReturn(player);

            var event = new TapEvent(permanent, true);

            assertThat(event.affectedPlayer()).isSameAs(player);
        }

        @Test
        void tappingFieldIsTrue() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, true);

            assertThat(event.tapping()).isTrue();
        }
    }

    @Nested
    class UntappingEvent {

        @Test
        void isUntappingReturnsTrueForUntapEvent() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, false);

            assertThat(event.isUntapping()).isTrue();
            assertThat(event.isTapping()).isFalse();
        }

        @Test
        void recordsCorrectPermanent() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, false);

            assertThat(event.permanent()).isSameAs(permanent);
        }

        @Test
        void affectedPlayerIsPermanentController() {
            var player = mock(Player.class);
            var permanent = mock(Permanent.class);
            when(permanent.controller()).thenReturn(player);

            var event = new TapEvent(permanent, false);

            assertThat(event.affectedPlayer()).isSameAs(player);
        }

        @Test
        void tappingFieldIsFalse() {
            var permanent = mock(Permanent.class);
            var event = new TapEvent(permanent, false);

            assertThat(event.tapping()).isFalse();
        }
    }

    @Nested
    class Equality {

        @Test
        void equalWhenSamePermanentAndSameTappingState() {
            var permanent = mock(Permanent.class);
            var event1 = new TapEvent(permanent, true);
            var event2 = new TapEvent(permanent, true);

            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        void notEqualWhenDifferentTappingState() {
            var permanent = mock(Permanent.class);
            var tapEvent = new TapEvent(permanent, true);
            var untapEvent = new TapEvent(permanent, false);

            assertThat(tapEvent).isNotEqualTo(untapEvent);
        }

        @Test
        void notEqualWhenDifferentPermanent() {
            var permanent1 = mock(Permanent.class);
            var permanent2 = mock(Permanent.class);
            var event1 = new TapEvent(permanent1, true);
            var event2 = new TapEvent(permanent2, true);

            assertThat(event1).isNotEqualTo(event2);
        }
    }
}
