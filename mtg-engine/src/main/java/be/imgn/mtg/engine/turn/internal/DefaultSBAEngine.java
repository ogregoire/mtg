package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.SBAEngine;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Default implementation of [SBAEngine].
///
/// Checks and applies all registered state-based actions.
final class DefaultSBAEngine implements SBAEngine {

    private final List<StateBasedAction> stateBasedActions;

    DefaultSBAEngine(List<StateBasedAction> stateBasedActions) {
        this.stateBasedActions = List.copyOf(stateBasedActions);
    }

    @Override
    public boolean checkAndApply(GameState gameState) {
        boolean anyApplied = false;
        boolean appliedThisPass;

        // Keep checking until no SBAs apply in a complete pass
        do {
            appliedThisPass = false;

            for (var sba : stateBasedActions) {
                if (sba.appliesTo(gameState)) {
                    sba.apply(gameState);
                    appliedThisPass = true;
                    anyApplied = true;
                }
            }
        } while (appliedThisPass);

        return anyApplied;
    }

    @Override
    public boolean wouldPerformActions(GameState gameState) {
        for (var sba : stateBasedActions) {
            if (sba.appliesTo(gameState)) {
                return true;
            }
        }
        return false;
    }
}
