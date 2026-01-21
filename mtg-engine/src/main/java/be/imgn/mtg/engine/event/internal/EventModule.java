package be.imgn.mtg.engine.event.internal;

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.event.ReplacementEffectRegistry;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggerQueue;

/// Guice module for the event system.
public final class EventModule extends AbstractModule {

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return new DefaultEventBus();
    }

    @Provides
    @Singleton
    EventTracker provideEventTracker(EventBus eventBus) {
        return new DefaultEventTracker(eventBus);
    }

    @Provides
    @Singleton
    GameEventProcessor provideGameEventProcessor(
            ReplacementEffectRegistry replacements,
            TriggerDetector triggerDetector,
            TriggerQueue triggerQueue,
            Map<Class<? extends GameEvent>, EventResolver<?>> resolvers,
            EventBus eventBus,
            GameState gameState) {
        return new DefaultGameEventProcessor(
                replacements, triggerDetector, triggerQueue, resolvers, eventBus, gameState);
    }

    @Provides
    @Singleton
    ReplacementEffectRegistry provideReplacementEffectRegistry(GameState gameState) {
        return new DefaultReplacementEffectRegistry(gameState);
    }
}
