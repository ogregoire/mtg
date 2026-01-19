package be.imgn.mtg.engine.game.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.internal.PlayerZonesModule;

/// Guice module that configures a single player.
///
/// Each player gets their own child injector with this module installed,
/// allowing per-player scoped dependencies. Includes player-specific zones
/// (library, hand, graveyard).
class PlayerModule extends AbstractModule {

    private final PlayerData playerData;

    PlayerModule(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Override
    protected void configure() {
        install(new PlayerZonesModule());
    }

    @Provides
    @Singleton
    Player providePlayer(Library library, Hand hand, Graveyard graveyard) {
        return new PlayerImpl(playerData, library, hand, graveyard);
    }
}
