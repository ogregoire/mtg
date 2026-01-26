package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LifeChangeEventTest {

    @Nested
    class Construction {

        @Test
        void createsEventWithPlayerAmountAndCause() {
            var player = mock(Player.class);
            var cause = new LifeChangeEvent.LifeChangeCause.LifeGain();
            var event = new LifeChangeEvent(player, 5, cause);

            assertThat(event.player()).isEqualTo(player);
            assertThat(event.amount()).isEqualTo(5);
            assertThat(event.cause()).isEqualTo(cause);
        }

        @Test
        void affectedPlayerReturnsPlayer() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, 3, new LifeChangeEvent.LifeChangeCause.LifeGain());

            assertThat(event.affectedPlayer()).isEqualTo(player);
        }
    }

    @Nested
    class IsGain {

        @Test
        void returnsTrueForPositiveAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, 10, new LifeChangeEvent.LifeChangeCause.LifeGain());

            assertThat(event.isGain()).isTrue();
        }

        @Test
        void returnsFalseForNegativeAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, -5, new LifeChangeEvent.LifeChangeCause.LifeLoss());

            assertThat(event.isGain()).isFalse();
        }

        @Test
        void returnsFalseForZeroAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, 0, new LifeChangeEvent.LifeChangeCause.LifeSet());

            assertThat(event.isGain()).isFalse();
        }
    }

    @Nested
    class IsLoss {

        @Test
        void returnsTrueForNegativeAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, -8, new LifeChangeEvent.LifeChangeCause.Damage());

            assertThat(event.isLoss()).isTrue();
        }

        @Test
        void returnsFalseForPositiveAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, 4, new LifeChangeEvent.LifeChangeCause.LifeGain());

            assertThat(event.isLoss()).isFalse();
        }

        @Test
        void returnsFalseForZeroAmount() {
            var player = mock(Player.class);
            var event = new LifeChangeEvent(player, 0, new LifeChangeEvent.LifeChangeCause.LifeSet());

            assertThat(event.isLoss()).isFalse();
        }
    }

    @Nested
    class LifeGainCause {

        @Test
        void createsLifeGainCause() {
            var cause = new LifeChangeEvent.LifeChangeCause.LifeGain();

            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(LifeChangeEvent.LifeChangeCause.class);
        }

        @Test
        void equalInstancesAreEqual() {
            var cause1 = new LifeChangeEvent.LifeChangeCause.LifeGain();
            var cause2 = new LifeChangeEvent.LifeChangeCause.LifeGain();

            assertThat(cause1).isEqualTo(cause2);
        }
    }

    @Nested
    class LifeLossCause {

        @Test
        void createsLifeLossCause() {
            var cause = new LifeChangeEvent.LifeChangeCause.LifeLoss();

            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(LifeChangeEvent.LifeChangeCause.class);
        }

        @Test
        void equalInstancesAreEqual() {
            var cause1 = new LifeChangeEvent.LifeChangeCause.LifeLoss();
            var cause2 = new LifeChangeEvent.LifeChangeCause.LifeLoss();

            assertThat(cause1).isEqualTo(cause2);
        }

        @Test
        void notEqualToLifeGain() {
            var lifeLoss = new LifeChangeEvent.LifeChangeCause.LifeLoss();
            var lifeGain = new LifeChangeEvent.LifeChangeCause.LifeGain();

            assertThat(lifeLoss).isNotEqualTo(lifeGain);
        }
    }

    @Nested
    class LifePaidCause {

        @Test
        void createsLifePaidCause() {
            var cause = new LifeChangeEvent.LifeChangeCause.LifePaid();

            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(LifeChangeEvent.LifeChangeCause.class);
        }

        @Test
        void equalInstancesAreEqual() {
            var cause1 = new LifeChangeEvent.LifeChangeCause.LifePaid();
            var cause2 = new LifeChangeEvent.LifeChangeCause.LifePaid();

            assertThat(cause1).isEqualTo(cause2);
        }
    }

    @Nested
    class DamageCause {

        @Test
        void createsDamageCause() {
            var cause = new LifeChangeEvent.LifeChangeCause.Damage();

            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(LifeChangeEvent.LifeChangeCause.class);
        }

        @Test
        void equalInstancesAreEqual() {
            var cause1 = new LifeChangeEvent.LifeChangeCause.Damage();
            var cause2 = new LifeChangeEvent.LifeChangeCause.Damage();

            assertThat(cause1).isEqualTo(cause2);
        }
    }

    @Nested
    class LifeSetCause {

        @Test
        void createsLifeSetCause() {
            var cause = new LifeChangeEvent.LifeChangeCause.LifeSet();

            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(LifeChangeEvent.LifeChangeCause.class);
        }

        @Test
        void equalInstancesAreEqual() {
            var cause1 = new LifeChangeEvent.LifeChangeCause.LifeSet();
            var cause2 = new LifeChangeEvent.LifeChangeCause.LifeSet();

            assertThat(cause1).isEqualTo(cause2);
        }
    }

    @Nested
    class RecordEquality {

        @Test
        void equalEventsAreEqual() {
            var player = mock(Player.class);
            var cause = new LifeChangeEvent.LifeChangeCause.LifeGain();
            var event1 = new LifeChangeEvent(player, 5, cause);
            var event2 = new LifeChangeEvent(player, 5, cause);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void differentAmountsAreNotEqual() {
            var player = mock(Player.class);
            var cause = new LifeChangeEvent.LifeChangeCause.LifeGain();
            var event1 = new LifeChangeEvent(player, 5, cause);
            var event2 = new LifeChangeEvent(player, 10, cause);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void differentCausesAreNotEqual() {
            var player = mock(Player.class);
            var event1 = new LifeChangeEvent(player, 5, new LifeChangeEvent.LifeChangeCause.LifeGain());
            var event2 = new LifeChangeEvent(player, 5, new LifeChangeEvent.LifeChangeCause.Damage());

            assertThat(event1).isNotEqualTo(event2);
        }
    }
}
