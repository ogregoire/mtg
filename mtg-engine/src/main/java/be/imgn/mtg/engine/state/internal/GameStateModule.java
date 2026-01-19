package be.imgn.mtg.engine.state.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Stack;

/// Guice module providing game state and related services.
///
/// Provides:
/// - [LastKnownInformation] - tracks object state before zone changes
/// - [GameState] - aggregates all zones and provides lookup
///
/// This module should be installed by the game configuration module.
public final class GameStateModule extends AbstractModule {

    @Override
    protected void configure() {}

    @Provides
    @Singleton
    LastKnownInformation provideLastKnownInformation() {
        return new DefaultLastKnownInformation();
    }

    @Provides
    @Singleton
    GameState provideGameState(
            Battlefield battlefield,
            Stack stack,
            Exile exile,
            CommandZone commandZone,
            LastKnownInformation lki,
            List<Player> players) {
        return new DefaultGameState(battlefield, stack, exile, commandZone, lki, players);
    }
}
