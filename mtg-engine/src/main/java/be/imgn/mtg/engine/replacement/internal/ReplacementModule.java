package be.imgn.mtg.engine.replacement.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.replacement.ReplacementEffectRegistry;
import be.imgn.mtg.engine.state.GameState;

/// Guice module providing replacement effect system bindings.
public final class ReplacementModule extends AbstractModule {

    @Provides
    @Singleton
    ReplacementEffectRegistry provideReplacementEffectRegistry(GameState gameState) {
        return new DefaultReplacementEffectRegistry(gameState);
    }
}
