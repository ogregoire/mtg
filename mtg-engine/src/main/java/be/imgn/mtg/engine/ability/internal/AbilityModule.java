package be.imgn.mtg.engine.ability.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Stack;

/// Guice module providing ability system bindings.
///
/// Provides:
/// - [AbilityManager] - Central coordinator for ability activation and registration
/// - Internal handlers for mana, loyalty, and regular activated abilities
public final class AbilityModule extends AbstractModule {

    @Override
    protected void configure() {
        // All bindings via @Provides methods
    }

    @Provides
    @Singleton
    AbilityManager provideAbilityManager(
            TurnTracker turnTracker,
            EventTracker eventTracker,
            EventBus eventBus,
            TriggerDetector triggerDetector,
            Stack stack) {

        var activatedHandler = new ActivatedAbilityHandler(turnTracker, eventTracker, eventBus);
        var manaHandler = new ManaAbilityHandler(eventBus);
        var loyaltyHandler = new LoyaltyAbilityHandler(turnTracker, eventTracker, eventBus);
        var scanner = new StaticAbilityScanner(triggerDetector);

        return new DefaultAbilityManager(activatedHandler, manaHandler, loyaltyHandler, scanner, stack);
    }
}
