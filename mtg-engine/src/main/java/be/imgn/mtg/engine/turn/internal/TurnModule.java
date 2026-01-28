package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.action.internal.ActionModule;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.turn.internal.sba.SbaModule;
import be.imgn.mtg.engine.zone.Stack;

/// Guice module for turn system bindings.
///
/// Provides:
/// - [TurnTracker] - main turn orchestrator (includes priority, phase/step progression, SBAs)
public final class TurnModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ActionModule());
        install(new SbaModule());
    }

    @Provides
    @Singleton
    TurnTracker provideTurnTracker(
            List<Player> players, Stack stack, EventBus eventBus, List<StateBasedAction> sbaCheckers) {
        return new DefaultTurnTracker(players, stack, eventBus, sbaCheckers);
    }
}
