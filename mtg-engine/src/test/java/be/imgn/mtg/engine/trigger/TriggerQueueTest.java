package be.imgn.mtg.engine.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.internal.DefaultTriggerQueue;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.ZoneType;

class TriggerQueueTest {

    private TriggerQueue queue;
    private Player player;
    private Card card;

    @BeforeEach
    void setUp() {
        queue = new DefaultTriggerQueue();
        player = mock(Player.class);
        card = mock(Card.class);
    }

    @Nested
    class AddTriggers {

        @Test
        void addSingleTrigger() {
            var instance = createTriggerInstance();
            queue.add(instance);

            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void addAllTriggers() {
            var instance1 = createTriggerInstance();
            var instance2 = createTriggerInstance();
            queue.addAll(List.of(instance1, instance2));

            assertThat(queue.hasPending()).isTrue();
        }
    }

    @Nested
    class HasPending {

        @Test
        void emptyQueueHasNoPending() {
            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void queueWithTriggersHasPending() {
            queue.add(createTriggerInstance());
            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void clearedQueueHasNoPending() {
            queue.add(createTriggerInstance());
            queue.clear();

            assertThat(queue.hasPending()).isFalse();
        }
    }

    @Nested
    class Clear {

        @Test
        void clearRemovesAllTriggers() {
            queue.add(createTriggerInstance());
            queue.add(createTriggerInstance());
            queue.add(createTriggerInstance());

            queue.clear();

            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void clearOnEmptyQueueDoesNotThrow() {
            queue.clear();
            // Should not throw
            assertThat(queue.hasPending()).isFalse();
        }
    }

    // Helper methods

    private TriggeredAbilityInstance createTriggerInstance() {
        var ability = new TestTriggeredAbility();
        var sourceId = ObjectId.create();
        var event = new DrawEvent(card, player);
        return new TriggeredAbilityInstance(ability, sourceId, player, event);
    }

    // Test implementations

    static class TestTriggeredAbility implements TriggeredAbility {
        @Override
        public TriggerCondition condition() {
            return (event, state) -> true;
        }

        @Override
        public Optional<Predicate<GameState>> interveningIf() {
            return Optional.empty();
        }

        @Override
        public Set<ZoneType> triggersFrom() {
            return Set.of(ZoneType.BATTLEFIELD);
        }
    }
}
