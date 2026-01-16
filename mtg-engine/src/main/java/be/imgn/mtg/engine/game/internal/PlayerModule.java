package be.imgn.mtg.engine.game.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;

/// Guice module that configures a single player.
///
/// Each player gets their own child injector with this module installed,
/// allowing per-player scoped dependencies.
class PlayerModule extends AbstractModule {

    private final PlayerData playerData;

    PlayerModule(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Provides
    @Singleton
    Player providePlayer() {
        return new PlayerImpl(playerData);
    }
}
