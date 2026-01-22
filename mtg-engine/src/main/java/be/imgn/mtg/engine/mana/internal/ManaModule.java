package be.imgn.mtg.engine.mana.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.mana.ManaPool;

/// Guice module for mana system bindings.
///
/// Provides per-player mana pool.
public class ManaModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bindings configured via @Provides methods
    }

    @Provides
    @Singleton
    ManaPool provideManaPool() {
        return new DefaultManaPool();
    }
}
