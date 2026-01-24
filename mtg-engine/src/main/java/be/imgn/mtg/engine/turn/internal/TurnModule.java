package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.action.internal.ActionModule;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.turn.internal.sba.LethalDamageSBA;
import be.imgn.mtg.engine.turn.internal.sba.ZeroLifeSBA;
import be.imgn.mtg.engine.turn.internal.sba.ZeroToughnessSBA;

/// Guice module for turn system bindings.
///
/// Provides all turn-related interfaces:
/// - [TurnTracker] - main turn orchestrator (includes TurnState, occurrence, and skip tracking)
/// - [PrioritySystem] - priority passing and APNAP ordering
/// - [DurationTracker] - effect duration tracking
public final class TurnModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ActionModule());
    }

    @Provides
    @Singleton
    PrioritySystem providePrioritySystem(GameState gameState) {
        return new DefaultPrioritySystem(gameState);
    }

    @Provides
    @Singleton
    List<StateBasedAction> provideStateBasedActions() {
        return List.of(new ZeroLifeSBA(), new LethalDamageSBA(), new ZeroToughnessSBA());
    }

    @Provides
    @Singleton
    DurationTracker provideDurationTracker() {
        return new DefaultDurationTracker();
    }

    @Provides
    @Singleton
    TurnTracker provideTurnTracker(
            GameState gameState,
            EventBus eventBus,
            PrioritySystem prioritySystem,
            List<StateBasedAction> stateBasedActions,
            DurationTracker durationTracker,
            TurnBasedActionRegistry turnBasedActionRegistry,
            GameEventProcessor gameEventProcessor) {
        return new DefaultTurnTracker(
                gameState,
                eventBus,
                prioritySystem,
                stateBasedActions,
                durationTracker,
                turnBasedActionRegistry,
                gameEventProcessor);
    }
}
