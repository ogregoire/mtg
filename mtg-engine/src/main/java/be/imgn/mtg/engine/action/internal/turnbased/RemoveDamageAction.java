package be.imgn.mtg.engine.action.internal.turnbased;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Removes all damage from permanents ({@mtg.rule 514.2}).
///
/// As a turn-based action during the cleanup step, simultaneously with
/// "until end of turn" effects ending, all damage marked on permanents
/// is removed.
///
/// Note: This is a partial implementation. Full implementation requires:
/// - Permanent damage tracking
public final class RemoveDamageAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.CLEANUP_REMOVE_DAMAGE;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        // TODO: Implement damage removal
        // 1. Get all permanents on the battlefield
        // 2. Remove all damage marked on each permanent
        //
        // for (var permanent : state.battlefield().permanents()) {
        //     permanent.clearDamage();
        // }
        //
        // Note: This doesn't produce events because it's not a state change
        // that other game mechanics care about (no triggers on damage removal)
    }
}
