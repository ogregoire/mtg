package be.imgn.mtg.engine.action.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.action.internal.turnbased.DayNightAction;
import be.imgn.mtg.engine.action.internal.turnbased.DiscardToHandSizeAction;
import be.imgn.mtg.engine.action.internal.turnbased.DrawForTurnAction;
import be.imgn.mtg.engine.action.internal.turnbased.EndDurationEffectsAction;
import be.imgn.mtg.engine.action.internal.turnbased.PhasingAction;
import be.imgn.mtg.engine.action.internal.turnbased.RemoveDamageAction;
import be.imgn.mtg.engine.action.internal.turnbased.UntapAction;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Guice module for action system bindings.
///
/// Provides:
/// - [TurnBasedActionRegistry] - registry for turn-based actions
/// - [ActionValidator] - validates player actions
/// - [ActionExecutor] - executes player actions
/// - [SpecialActionHandler] - handles Rule 116 special actions
public final class ActionModule extends AbstractModule {

    @Override
    protected void configure() {
        // All bindings are provided via @Provides methods
    }

    @Provides
    @Singleton
    List<TurnBasedAction> provideTurnBasedActions(DurationTracker durationTracker) {
        return List.of(
                // Untap step
                new PhasingAction(),
                new DayNightAction(),
                new UntapAction(),
                // Draw step
                new DrawForTurnAction(),
                // Cleanup step
                new DiscardToHandSizeAction(),
                new RemoveDamageAction(),
                new EndDurationEffectsAction(durationTracker));
    }

    @Provides
    @Singleton
    TurnBasedActionRegistry provideTurnBasedActionRegistry(List<TurnBasedAction> actions) {
        return new DefaultTurnBasedActionRegistry(actions);
    }

    @Provides
    @Singleton
    ActionValidator provideActionValidator(PrioritySystem prioritySystem) {
        return new DefaultActionValidator(prioritySystem);
    }

    @Provides
    @Singleton
    SpecialActionHandler provideSpecialActionHandler(GameEventProcessor eventProcessor) {
        return new DefaultSpecialActionHandler(eventProcessor);
    }

    @Provides
    @Singleton
    ActionExecutor provideActionExecutor(PrioritySystem prioritySystem, SpecialActionHandler specialActionHandler) {
        return new DefaultActionExecutor(prioritySystem, specialActionHandler);
    }
}
