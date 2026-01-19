package be.imgn.mtg.engine.game.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.event.internal.EventModule;
import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.Game;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;
import be.imgn.mtg.engine.replacement.internal.ReplacementModule;
import be.imgn.mtg.engine.resolver.internal.ResolverModule;
import be.imgn.mtg.engine.state.internal.GameStateModule;
import be.imgn.mtg.engine.trigger.internal.TriggerModule;
import be.imgn.mtg.engine.zone.internal.SharedZonesModule;

/// Guice module that configures a specific game instance.
///
/// This module is created by [GameModule] for each new game and provides:
/// - The [Format] for this game
/// - The [Game] instance
/// - The list of [Player]s participating in the game
/// - Shared zones (battlefield, stack, exile, command zone)
/// - Game state management
/// - Event bus and event processing
/// - Replacement effects, triggers, and resolvers
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
    protected void configure() {
        install(new SharedZonesModule());
        install(new GameStateModule());
        install(new EventModule());
        install(new ReplacementModule());
        install(new TriggerModule());
        install(new ResolverModule());
    }

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
