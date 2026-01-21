package be.imgn.mtg.engine.action.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Default implementation of the turn-based action registry.
///
/// Stores actions in a map indexed by timing, ensuring efficient lookup
/// when executing actions for a specific timing point.
final class DefaultTurnBasedActionRegistry implements TurnBasedActionRegistry {

    private final Map<TurnBasedTiming, List<TurnBasedAction>> actionsByTiming;

    DefaultTurnBasedActionRegistry(List<TurnBasedAction> actions) {
        this.actionsByTiming = new EnumMap<>(TurnBasedTiming.class);

        // Initialize empty lists for all timings
        for (var timing : TurnBasedTiming.values()) {
            this.actionsByTiming.put(timing, new ArrayList<>());
        }

        // Register each action under its timing
        for (var action : actions) {
            actionsByTiming
                    .computeIfAbsent(action.timing(), k -> new ArrayList<>())
                    .add(action);
        }
    }

    @Override
    public List<TurnBasedAction> getActionsFor(TurnBasedTiming timing) {
        return List.copyOf(actionsByTiming.getOrDefault(timing, List.of()));
    }

    @Override
    public void executeAll(TurnBasedTiming timing, GameState state, GameEventProcessor processor) {
        var actions = actionsByTiming.getOrDefault(timing, List.of());
        for (var action : actions) {
            action.execute(state, processor);
        }
    }
}
