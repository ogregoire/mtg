package be.imgn.mtg.engine.event.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;

/// Guice module for the event system.
public final class EventModule extends AbstractModule {

    @Provides
    @Singleton
    EventBus eventBus() {
        return new DefaultEventBus();
    }

    @Provides
    @Singleton
    EventTracker eventTracker(EventBus eventBus) {
        return new DefaultEventTracker(eventBus);
    }
}
