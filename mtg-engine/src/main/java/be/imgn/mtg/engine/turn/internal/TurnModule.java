package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.action.internal.ActionModule;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Stack;

/// Guice module for turn system bindings.
///
/// Provides:
/// - [TurnTracker] - main turn orchestrator (includes priority, phase/step progression, SBAs)
public final class TurnModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ActionModule());
    }

    @Provides
    @Singleton
    TurnTracker provideTurnTracker(List<Player> players, Stack stack, EventBus eventBus) {
        // TODO: Wire state-based action checkers from StateBasedAction implementations
        return new DefaultTurnTracker(players, stack, eventBus, List.of());
    }
}
