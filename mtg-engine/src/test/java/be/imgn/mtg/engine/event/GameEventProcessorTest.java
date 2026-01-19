package be.imgn.mtg.engine.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.internal.DefaultGameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.replacement.ReplacementEffect;
import be.imgn.mtg.engine.replacement.ReplacementEffectRegistry;
import be.imgn.mtg.engine.replacement.ReplacementEffectRegistry.ApplicableReplacement;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerCondition;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggerQueue;
import be.imgn.mtg.engine.trigger.TriggeredAbility;
import be.imgn.mtg.engine.trigger.TriggeredAbilityInstance;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.ZoneType;

class GameEventProcessorTest {

    private GameEventProcessor processor;
    private ReplacementEffectRegistry replacements;
    private TriggerDetector triggerDetector;
    private TriggerQueue triggerQueue;
    private EventBus eventBus;
    private GameState gameState;
    private TestResolver testResolver;
    private Player player;
    private Card card;

    @BeforeEach
    void setUp() {
        replacements = mock(ReplacementEffectRegistry.class);
        triggerDetector = mock(TriggerDetector.class);
        triggerQueue = mock(TriggerQueue.class);
        eventBus = mock(EventBus.class);
        gameState = mock(GameState.class);
        testResolver = new TestResolver();
        player = mock(Player.class);
        card = mock(Card.class);

        when(replacements.findApplicable(any())).thenReturn(List.of());
        when(triggerDetector.detect(any(), any())).thenReturn(List.of());

        processor = new DefaultGameEventProcessor(
                replacements,
                triggerDetector,
                triggerQueue,
                Map.of(DrawEvent.class, testResolver),
                eventBus,
                gameState);
    }

    @Nested
    class ProcessSingleEvent {

        @Test
        void resolvesEvent() {
            var event = new DrawEvent(card, player);

            processor.process(event);

            assertThat(testResolver.resolvedEvents).containsExactly(event);
        }

        @Test
        void detectsTriggers() {
            var event = new DrawEvent(card, player);

            processor.process(event);

            verify(triggerDetector).detect(event, gameState);
        }

        @Test
        void addsDetectedTriggersToQueue() {
            var event = new DrawEvent(card, player);
            var trigger = createTriggerInstance(event);
            when(triggerDetector.detect(event, gameState)).thenReturn(List.of(trigger));

            processor.process(event);

            verify(triggerQueue).addAll(List.of(trigger));
        }

        @Test
        void postsEventToEventBus() {
            var event = new DrawEvent(card, player);

            processor.process(event);

            verify(eventBus).post(event);
        }

        @Test
        void executesAllPhasesInOrder() {
            var event = new DrawEvent(card, player);
            var callOrder = new ArrayList<String>();

            // Track call order through side effects
            when(replacements.findApplicable(any())).thenAnswer(inv -> {
                callOrder.add("replacements");
                return List.of();
            });

            testResolver = new TestResolver() {
                @Override
                public void resolve(DrawEvent event, GameState state) {
                    callOrder.add("resolve");
                    super.resolve(event, state);
                }
            };

            when(triggerDetector.detect(any(), any())).thenAnswer(inv -> {
                callOrder.add("triggers");
                return List.of();
            });

            doAnswer(inv -> {
                        callOrder.add("eventBus");
                        return null;
                    })
                    .when(eventBus)
                    .post(any());

            processor = new DefaultGameEventProcessor(
                    replacements,
                    triggerDetector,
                    triggerQueue,
                    Map.of(DrawEvent.class, testResolver),
                    eventBus,
                    gameState);

            processor.process(event);

            assertThat(callOrder).containsExactly("replacements", "resolve", "triggers", "eventBus");
        }
    }

    @Nested
    class ReplacementEffects {

        @Test
        void appliesReplacementEffect() {
            var originalEvent = new DrawEvent(card, player);
            var modifiedCard = mock(Card.class);
            var modifiedEvent = new DrawEvent(modifiedCard, player);

            var replacement = new ModifyingReplacement(modifiedEvent);
            var applicable = new ApplicableReplacement(replacement, ObjectId.create(), player);

            when(replacements.findApplicable(originalEvent)).thenReturn(List.of(applicable));
            when(replacements.findApplicable(modifiedEvent)).thenReturn(List.of());

            processor.process(originalEvent);

            // Modified event should be resolved, not original
            assertThat(testResolver.resolvedEvents).containsExactly(modifiedEvent);
        }

        @Test
        void preventedEventNotResolved() {
            var event = new DrawEvent(card, player);

            var replacement = new PreventingReplacement();
            var applicable = new ApplicableReplacement(replacement, ObjectId.create(), player);

            when(replacements.findApplicable(event)).thenReturn(List.of(applicable));

            processor.process(event);

            assertThat(testResolver.resolvedEvents).isEmpty();
            verify(eventBus, never()).post(any());
        }

        @Test
        void multipleEventsFromReplacement() {
            var originalEvent = new DrawEvent(card, player);
            var card1 = mock(Card.class);
            var card2 = mock(Card.class);
            var event1 = new DrawEvent(card1, player);
            var event2 = new DrawEvent(card2, player);

            var replacement = new SplittingReplacement(List.of(event1, event2));
            var applicable = new ApplicableReplacement(replacement, ObjectId.create(), player);

            when(replacements.findApplicable(originalEvent)).thenReturn(List.of(applicable));
            when(replacements.findApplicable(event1)).thenReturn(List.of());
            when(replacements.findApplicable(event2)).thenReturn(List.of());

            processor.process(originalEvent);

            assertThat(testResolver.resolvedEvents).containsExactly(event1, event2);
            verify(eventBus, times(2)).post(any());
        }

        @Test
        void loopsUntilStable() {
            var card1 = mock(Card.class);
            var card2 = mock(Card.class);
            var card3 = mock(Card.class);
            var event1 = new DrawEvent(card1, player);
            var event2 = new DrawEvent(card2, player);
            var event3 = new DrawEvent(card3, player);

            var replacement1 = new ModifyingReplacement(event2);
            var replacement2 = new ModifyingReplacement(event3);

            var applicable1 = new ApplicableReplacement(replacement1, ObjectId.create(), player);
            var applicable2 = new ApplicableReplacement(replacement2, ObjectId.create(), player);

            when(replacements.findApplicable(event1)).thenReturn(List.of(applicable1));
            when(replacements.findApplicable(event2)).thenReturn(List.of(applicable2));
            when(replacements.findApplicable(event3)).thenReturn(List.of());

            processor.process(event1);

            // Final event after two replacement iterations
            assertThat(testResolver.resolvedEvents).containsExactly(event3);
        }
    }

    @Nested
    class BatchProcessing {

        @Test
        void processesBatchWithinEventBusBatch() {
            var card1 = mock(Card.class);
            var card2 = mock(Card.class);
            var event1 = new DrawEvent(card1, player);
            var event2 = new DrawEvent(card2, player);

            var batchContext = mock(EventBus.Batch.class);
            when(eventBus.batch()).thenReturn(batchContext);

            processor.processBatch(List.of(event1, event2));

            verify(eventBus).batch();
            verify(batchContext).close();
            assertThat(testResolver.resolvedEvents).containsExactly(event1, event2);
        }

        @Test
        void emptyBatchDoesNothing() {
            var batchContext = mock(EventBus.Batch.class);
            when(eventBus.batch()).thenReturn(batchContext);

            processor.processBatch(List.of());

            verify(batchContext).close();
            assertThat(testResolver.resolvedEvents).isEmpty();
        }
    }

    // Helper methods

    private TriggeredAbilityInstance createTriggerInstance(GameEvent event) {
        var ability = new TestTriggeredAbility();
        var sourceId = ObjectId.create();
        return new TriggeredAbilityInstance(ability, sourceId, player, event);
    }

    // Test implementations

    static class TestResolver implements EventResolver<DrawEvent> {
        final List<DrawEvent> resolvedEvents = new ArrayList<>();

        @Override
        public Class<DrawEvent> eventType() {
            return DrawEvent.class;
        }

        @Override
        public void resolve(DrawEvent event, GameState state) {
            resolvedEvents.add(event);
        }
    }

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

    static class ModifyingReplacement implements ReplacementEffect {
        private final GameEvent replacement;

        ModifyingReplacement(GameEvent replacement) {
            this.replacement = replacement;
        }

        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(replacement);
        }
    }

    static class PreventingReplacement implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Prevented();
        }
    }

    static class SplittingReplacement implements ReplacementEffect {
        private final List<GameEvent> events;

        SplittingReplacement(List<GameEvent> events) {
            this.events = events;
        }

        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Multiple(events);
        }
    }
}
