package be.imgn.mtg.engine.trigger.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
            var instance = createInstance(activePlayer, mock(Card.class));

            queue.add(instance);

            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void addAllMakesPending() {
            var instance1 = createInstance(activePlayer, mock(Card.class));
            var instance2 = createInstance(activePlayer, mock(Card.class));

            queue.addAll(List.of(instance1, instance2));

            assertThat(queue.hasPending()).isTrue();
        }

        @Test
        void clearRemovesPending() {
            var instance = createInstance(activePlayer, mock(Card.class));
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
            var source = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("Test Card")
                    .build();
            var instance = createInstance(activePlayer, source);
            queue.add(instance);

            queue.flushToStack(stack, gameState, activePlayer);

            assertThat(queue.hasPending()).isFalse();
        }

        @Test
        void flushWithSourceFound() {
            var source = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("Test Card")
                    .build();
            var instance = createInstance(activePlayer, source);
            queue.add(instance);

            // Should not throw
            queue.flushToStack(stack, gameState, activePlayer);
        }

        @Test
        void flushWithSourceWithoutName() {
            var source = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("")
                    .build();
            var instance = createInstance(activePlayer, source);
            queue.add(instance);

            // Should not throw
            queue.flushToStack(stack, gameState, activePlayer);
        }

        @Test
        void flushGroupsByController() {
            var activeSource = Card.builder()
                    .owner(activePlayer)
                    .controller(activePlayer)
                    .name("Active Source")
                    .build();
            var otherSource = Card.builder()
                    .owner(otherPlayer)
                    .controller(otherPlayer)
                    .name("Other Source")
                    .build();

            var activeInstance = createInstance(activePlayer, activeSource);
            var otherInstance = createInstance(otherPlayer, otherSource);

            queue.add(activeInstance);
            queue.add(otherInstance);

            queue.flushToStack(stack, gameState, activePlayer);

            assertThat(queue.hasPending()).isFalse();
        }
    }

    private TriggeredAbilityInstance createInstance(Player controller, Card source) {
        var ability = new TestTriggeredAbility();
        var card = Card.builder()
                .owner(controller)
                .controller(controller)
                .name("Test")
                .build();
        var event = new DrawEvent(card, controller);
        return new TriggeredAbilityInstance(ability, source, controller, event);
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
