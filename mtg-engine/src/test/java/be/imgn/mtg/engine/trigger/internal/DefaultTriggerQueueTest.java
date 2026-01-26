package be.imgn.mtg.engine.trigger.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerCondition;
import be.imgn.mtg.engine.trigger.TriggeredAbility;
import be.imgn.mtg.engine.trigger.TriggeredAbilityInstance;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

class DefaultTriggerQueueTest {

    DefaultTriggerQueue queue;
    Stack stack;
    GameState gameState;
    Player activePlayer;
    Player otherPlayer;

    @BeforeEach
    void setUp() {
        queue = new DefaultTriggerQueue();
        stack = mock(Stack.class);
        gameState = mock(GameState.class);
        activePlayer = mock(Player.class);
        otherPlayer = mock(Player.class);
    }

    @Nested
    class BasicOperations {

        @Test
        void initiallyEmpty() {
            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void addMakesPending() {
            var instance = createInstance(activePlayer, new ObjectId());

            queue.add(instance);

            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void addAllMakesPending() {
            var instance1 = createInstance(activePlayer, new ObjectId());
            var instance2 = createInstance(activePlayer, new ObjectId());

            queue.addAll(List.of(instance1, instance2));

            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void clearRemovesPending() {
            var instance = createInstance(activePlayer, new ObjectId());
            queue.add(instance);

            queue.clear();

            assertThat(queue.hasPending()).isFalse();
        }
    }

    @Nested
    class FlushToStack {

        @Test
        void flushEmptyQueueDoesNothing() {
            queue.flushToStack(stack, gameState, activePlayer);

            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void flushClearsPendingTriggers() {
            var sourceId = new ObjectId();
            var instance = createInstance(activePlayer, sourceId);
            queue.add(instance);

            // Source not found
            when(gameState.findObject(sourceId)).thenReturn(Optional.empty());

            queue.flushToStack(stack, gameState, activePlayer);

            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void flushWithSourceNotFound() {
            var sourceId = new ObjectId();
            var instance = createInstance(activePlayer, sourceId);
            queue.add(instance);

            when(gameState.findObject(sourceId)).thenReturn(Optional.empty());

            // Should not throw
            queue.flushToStack(stack, gameState, activePlayer);
        }

        @Test
        void flushWithSourceFound() {
            var sourceId = new ObjectId();
            var source = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("Test Card")
                    .build();
            var instance = createInstance(activePlayer, sourceId);
            queue.add(instance);

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));

            // Should not throw
            queue.flushToStack(stack, gameState, activePlayer);
        }

        @Test
        void flushWithSourceWithoutName() {
            var sourceId = new ObjectId();
            var source = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("")
                    .build();
            var instance = createInstance(activePlayer, sourceId);
            queue.add(instance);

            when(gameState.findObject(sourceId)).thenReturn(Optional.of(source));

            // Should not throw
            queue.flushToStack(stack, gameState, activePlayer);
        }

        @Test
        void flushGroupsByController() {
            var activeSourceId = new ObjectId();
            var otherSourceId = new ObjectId();

            var activeInstance = createInstance(activePlayer, activeSourceId);
            var otherInstance = createInstance(otherPlayer, otherSourceId);

            queue.add(activeInstance);
            queue.add(otherInstance);

            // Both sources not found (simplified test)
            when(gameState.findObject(activeSourceId)).thenReturn(Optional.empty());
            when(gameState.findObject(otherSourceId)).thenReturn(Optional.empty());

            queue.flushToStack(stack, gameState, activePlayer);

            assertThat(queue.hasPending()).isFalse();
        }
    }

    private TriggeredAbilityInstance createInstance(Player controller, ObjectId sourceId) {
        var ability = new TestTriggeredAbility();
        var card = Card.builder()
                .owner(controller)
                .controller(controller)
                .name("Test")
                .build();
        var event = new DrawEvent(card, controller);
        return new TriggeredAbilityInstance(ability, sourceId, controller, event);
    }

    static class TestTriggeredAbility implements TriggeredAbility {
        private final AbilityId id = new AbilityId();

        @Override
        public AbilityId id() {
            return id;
        }

        @Override
        public String oracleText() {
            return "Test triggered ability";
        }

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

        @Override
        public List<Effect> effects() {
            return List.of();
        }

        @Override
        public boolean isManaAbility() {
            return false;
        }
    }
}
