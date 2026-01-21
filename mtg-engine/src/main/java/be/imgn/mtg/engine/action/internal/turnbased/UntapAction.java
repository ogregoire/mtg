package be.imgn.mtg.engine.action.internal.turnbased;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;

/// Untaps the active player's permanents ({@mtg.rule 502.3}).
///
/// As a turn-based action, the active player untaps all permanents they control
/// that normally untap during the untap step. Permanents with "doesn't untap
/// during your untap step" restrictions are not untapped ({@mtg.rule 502.3}).
///
/// The untap happens simultaneously for all permanents ({@mtg.rule 502.3}).
///
/// Note: Full "doesn't untap" restriction checking requires the ability system
/// to track continuous effects. For now, we use canUntap() which may be overridden.
public final class UntapAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.UNTAP_STEP_UNTAP;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        var activePlayer = state.activePlayer();
        var battlefield = state.battlefield();

        // Find all tapped permanents controlled by active player that can untap
        List<Permanent> toUntap = new ArrayList<>();
        for (var permanent : battlefield.controlledBy(activePlayer)) {
            if (permanent.isTapped() && permanent.canUntap()) {
                toUntap.add(permanent);
            }
        }

        // Untap all simultaneously
        // Note: In a full implementation, this would generate UntapEvents
        // and process them through the GameEventProcessor to handle
        // replacement effects and triggers. For now, we directly untap.
        for (var permanent : toUntap) {
            permanent.untap();
        }
    }
}
