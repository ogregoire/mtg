package be.imgn.mtg.engine.resolver.internal;

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.characteristics.CounterEvent;
import be.imgn.mtg.engine.combat.DamageEvent;
import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.LifeChangeEvent;
import be.imgn.mtg.engine.object.TapEvent;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.zone.ZoneChangeEvent;

/// Guice module providing event resolver bindings.
public final class ResolverModule extends AbstractModule {

    @Provides
    @Singleton
    Map<Class<? extends GameEvent>, EventResolver<?>> provideResolvers(LastKnownInformation lki) {
        return Map.of(
                ZoneChangeEvent.class, new ZoneChangeResolver(lki),
                DamageEvent.class, new DamageResolver(),
                LifeChangeEvent.class, new LifeChangeResolver(),
                CounterEvent.class, new CounterResolver(),
                TapEvent.class, new TapResolver());
    }

    @Provides
    @Singleton
    EffectExecutor provideEffectExecutor() {
        return new DefaultEffectExecutor();
    }
}
