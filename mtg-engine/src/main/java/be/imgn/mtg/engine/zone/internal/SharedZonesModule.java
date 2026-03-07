package be.imgn.mtg.engine.zone.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Stack;

/// Guice module providing shared zone implementations.
///
/// Provides singleton instances of zones that are shared by all players:
/// - [Battlefield] - where permanents exist
/// - [Stack] - where spells and abilities wait to resolve
/// - [Exile] - where exiled cards go
/// - [CommandZone] - for commanders and emblems
///
/// This module should be installed by the game configuration module.
public final class SharedZonesModule extends AbstractModule {

    @Override
    protected void configure() {}

    @Provides
    @Singleton
    Battlefield provideBattlefield(ObjectStore store, GameEventProcessor eventProcessor) {
        return new DefaultBattlefield(store, eventProcessor);
    }

    @Provides
    @Singleton
    Stack provideStack(ObjectStore store, GameEventProcessor eventProcessor, GameState gameState) {
        return new DefaultStack(store, eventProcessor, gameState);
    }

    @Provides
    @Singleton
    Exile provideExile(ObjectStore store) {
        return new DefaultExile(store);
    }

    @Provides
    @Singleton
    CommandZone provideCommandZone(ObjectStore store) {
        return new DefaultCommandZone(store);
    }
}
