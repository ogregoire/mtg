package be.imgn.mtg.engine.event.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.event.ReplacementEffectRegistry;
import be.imgn.mtg.engine.event.ReplacementResult;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggerQueue;

/// Default implementation of the game event processor.
///
/// Orchestrates the event processing pipeline: replacement → resolution → triggers → notification.
public final class DefaultGameEventProcessor implements GameEventProcessor {

    private static final int MAX_REPLACEMENT_ITERATIONS = 1000;

    private final ReplacementEffectRegistry replacements;
    private final TriggerDetector triggerDetector;
    private final TriggerQueue triggerQueue;
    private final Map<Class<? extends GameEvent>, EventResolver<?>> resolvers;
    private final EventBus eventBus;
    private final GameState gameState;

    public DefaultGameEventProcessor(
            ReplacementEffectRegistry replacements,
            TriggerDetector triggerDetector,
            TriggerQueue triggerQueue,
            Map<Class<? extends GameEvent>, EventResolver<?>> resolvers,
            EventBus eventBus,
            GameState gameState) {
        this.replacements = replacements;
        this.triggerDetector = triggerDetector;
        this.triggerQueue = triggerQueue;
        this.resolvers = resolvers;
        this.eventBus = eventBus;
        this.gameState = gameState;
    }

    @Override
    public void process(GameEvent event) {
        // Phase 1: Apply replacement effects
        var finalEvents = applyReplacements(event);

        // Process each resulting event (usually just one unless replacements split it)
        for (var finalEvent : finalEvents) {
            // Phase 2: Resolve the event
            resolve(finalEvent);

            // Phase 3: Detect triggers
            var triggers = triggerDetector.detect(finalEvent, gameState);
            triggerQueue.addAll(triggers);

            // Phase 4: Post to event bus
            eventBus.post(finalEvent);
        }
    }

    @Override
    public void processBatch(List<GameEvent> events) {
        try (var ignored = eventBus.batch()) {
            for (var event : events) {
                process(event);
            }
        }
    }

    /// Applies replacement effects until no more apply or the event is prevented.
    ///
    /// Returns an empty list if the event was prevented, or the final form(s) of the event.
    private List<GameEvent> applyReplacements(GameEvent event) {
        var current = List.of(event);
        var iterations = 0;

        while (iterations < MAX_REPLACEMENT_ITERATIONS) {
            var next = new ArrayList<GameEvent>();
            var anyReplaced = false;

            for (var evt : current) {
                var applicable = replacements.findApplicable(evt);

                if (applicable.isEmpty()) {
                    // No replacements apply, keep this event as-is
                    next.add(evt);
                } else {
                    // Apply the first applicable replacement
                    // In a full implementation, the affected player would choose if multiple apply
                    var chosen = applicable.getFirst();
                    var result = chosen.effect().replace(evt, gameState);

                    switch (result) {
                        case ReplacementResult.Modified(var modified) -> {
                            next.add(modified);
                            anyReplaced = true;
                        }
                        case ReplacementResult.Prevented() -> {
                            // Event prevented, don't add anything
                            anyReplaced = true;
                        }
                        case ReplacementResult.Multiple(var multiple) -> {
                            next.addAll(multiple);
                            anyReplaced = true;
                        }
                    }
                }
            }

            if (!anyReplaced) {
                // No more replacements applied, we're stable
                return next;
            }

            current = next;
            iterations++;
        }

        // Safety: too many iterations, return what we have
        return current;
    }

    /// Resolves an event by dispatching to the appropriate resolver.
    @SuppressWarnings("unchecked")
    private void resolve(GameEvent event) {
        // Find a resolver for this event type or its supertypes
        EventResolver<GameEvent> resolver = null;

        for (var entry : resolvers.entrySet()) {
            if (entry.getKey().isAssignableFrom(event.getClass())) {
                resolver = (EventResolver<GameEvent>) entry.getValue();
                break;
            }
        }

        if (resolver != null) {
            resolver.resolve(event, gameState);
        }
    }
}
