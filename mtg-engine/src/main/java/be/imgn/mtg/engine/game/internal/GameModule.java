package be.imgn.mtg.engine.game.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;

import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.ChoiceHandler;
import be.imgn.mtg.engine.game.Game;
import be.imgn.mtg.engine.game.GameFactory;
import be.imgn.mtg.engine.game.PlayerData;

/// Top-level Guice module for the MTG engine.
///
/// Install this module in your application's injector to get access to the
/// [GameFactory], which can then be used to create game instances.
///
/// Example usage:
/// ```java
/// var injector = Guice.createInjector(new GameModule());
/// var factory = injector.getInstance(GameFactory.class);
/// var game = factory.create(format, playersData);
/// ```
///
/// To override the default [ChoiceHandler], use [OptionalBinder.setBinding()]:
/// ```java
/// var injector = Guice.createInjector(new GameModule(), new AbstractModule() {
///     @Override protected void configure() {
///         OptionalBinder.newOptionalBinder(binder(), ChoiceHandler.class)
///             .setBinding().to(MyChoiceHandler.class);
///     }
/// });
/// ```
public final class GameModule extends AbstractModule {

    @Override
    protected void configure() {
        // Default ChoiceHandler, can be overridden by binding ChoiceHandler in a sibling module
        OptionalBinder.newOptionalBinder(binder(), ChoiceHandler.class)
                .setDefault()
                .toInstance(FirstOptionChoiceHandler.INSTANCE);
    }

    @Provides
    @Singleton
    GameFactory provideGameFactory(Injector parentInjector) {
        return new GameFactoryImpl(parentInjector);
    }

    private record GameFactoryImpl(Injector parentInjector) implements GameFactory {
        @Override
        public Game createGame(Format format, List<PlayerData> playersData) {
            format.checkPlayers(playersData);
            var gameModule = new GameConfigurationModule(format, playersData);
            var gameInjector = parentInjector.createChildInjector(gameModule);
            return gameInjector.getInstance(Game.class);
        }
    }
}
