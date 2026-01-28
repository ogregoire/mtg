package be.imgn.mtg.engine.action.internal.turnbased;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Ends "until end of turn" and "this turn" effects ({@mtg.rule 514.2}).
///
/// As a turn-based action during the cleanup step, simultaneously with
/// damage removal, effects that last "until end of turn" or "this turn" expire.
public final class EndDurationEffectsAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.CLEANUP_REMOVE_DAMAGE;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        // TODO: Implement duration effect expiration via continuous effect layer system
    }
}
