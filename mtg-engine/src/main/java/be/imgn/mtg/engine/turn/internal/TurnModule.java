package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.APNAPOrder;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.SBAEngine;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.StateBasedAction;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.turn.internal.sba.LethalDamageSBA;
import be.imgn.mtg.engine.turn.internal.sba.ZeroLifeSBA;
import be.imgn.mtg.engine.turn.internal.sba.ZeroToughnessSBA;

/// Guice module for turn system bindings.
///
/// Provides all turn-related interfaces:
/// - [TurnTracker] - main turn orchestrator
/// - [OccurrenceTracker] - phase/step occurrence counting
/// - [PrioritySystem] - priority passing
/// - [APNAPOrder] - player ordering
/// - [SBAEngine] - state-based actions
/// - [DurationTracker] - effect duration tracking
/// - [SkipTracker] - phase/step/turn skip tracking
public final class TurnModule extends AbstractModule {

    @Override
    protected void configure() {
        // All bindings are provided via @Provides methods
    }

    @Provides
    @Singleton
    OccurrenceTracker provideOccurrenceTracker() {
        return new DefaultOccurrenceTracker();
    }

    @Provides
    @Singleton
    APNAPOrder provideAPNAPOrder() {
        return new DefaultAPNAPOrder();
    }

    @Provides
    @Singleton
    PrioritySystem providePrioritySystem(GameState gameState, APNAPOrder apnapOrder) {
        return new DefaultPrioritySystem(gameState, apnapOrder);
    }

    @Provides
    @Singleton
    List<StateBasedAction> provideStateBasedActions() {
        return List.of(new ZeroLifeSBA(), new LethalDamageSBA(), new ZeroToughnessSBA());
    }

    @Provides
    @Singleton
    SBAEngine provideSBAEngine(List<StateBasedAction> stateBasedActions) {
        return new DefaultSBAEngine(stateBasedActions);
    }

    @Provides
    @Singleton
    DurationTracker provideDurationTracker() {
        return new DefaultDurationTracker();
    }

    @Provides
    @Singleton
    SkipTracker provideSkipTracker() {
        return new DefaultSkipTracker();
    }

    @Provides
    @Singleton
    TurnTracker provideTurnTracker(
            GameState gameState,
            EventBus eventBus,
            OccurrenceTracker occurrenceTracker,
            PrioritySystem prioritySystem,
            SBAEngine sbaEngine,
            DurationTracker durationTracker,
            SkipTracker skipTracker) {
        return new DefaultTurnTracker(
                gameState, eventBus, occurrenceTracker, prioritySystem, sbaEngine, durationTracker, skipTracker);
    }
}
