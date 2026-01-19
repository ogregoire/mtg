package be.imgn.mtg.engine.trigger.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggerQueue;

/// Guice module providing trigger system bindings.
public final class TriggerModule extends AbstractModule {

    @Provides
    @Singleton
    TriggerDetector provideTriggerDetector() {
        return new DefaultTriggerDetector();
    }

    @Provides
    @Singleton
    TriggerQueue provideTriggerQueue() {
        return new DefaultTriggerQueue();
    }
}
