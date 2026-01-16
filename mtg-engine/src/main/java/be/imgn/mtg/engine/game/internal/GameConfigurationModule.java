package be.imgn.mtg.engine.game.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.Game;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;

/// Guice module that configures a specific game instance.
///
/// This module is created by [GameModule] for each new game and provides:
/// - The [Format] for this game
/// - The [Game] instance
/// - The list of [Player]s participating in the game
///
/// Each player is created in its own child injector via [PlayerModule] to allow
/// per-player scoped bindings.
public final class GameConfigurationModule extends AbstractModule {

    private final Format format;
    private final List<PlayerData> playersData;

    public GameConfigurationModule(Format format, List<PlayerData> playersData) {
        this.format = format;
        this.playersData = playersData;
    }

    @Override
    protected void configure() {}

    @Provides
    @Singleton
    Format provideFormat() {
        return format;
    }

    @Provides
    @Singleton
    Game provideGame(List<Player> players) {
        return new GameImpl(players);
    }

    @Provides
    @Singleton
    List<Player> providePlayers(Injector injector) {
        return playersData.stream()
                .map(data -> {
                    var playerInjector = injector.createChildInjector(new PlayerModule(data));
                    return playerInjector.getInstance(Player.class);
                })
                .toList();
    }
}
