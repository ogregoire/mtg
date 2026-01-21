package be.imgn.mtg.engine.action.internal.turnbased;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;

/// Handles phasing at the beginning of the untap step ({@mtg.rule 502.1}).
///
/// During the untap step, phasing occurs in this order ({@mtg.rule 702.26d}):
/// 1. All phased-out permanents controlled by the active player simultaneously phase in
/// 2. All permanents with phasing controlled by the active player simultaneously phase out
///
/// Phasing in/out doesn't trigger enters/leaves battlefield abilities ({@mtg.rule 702.26e}).
/// Phased-out permanents are treated as though they don't exist ({@mtg.rule 702.26b}).
///
/// Note: This implementation handles basic phasing. More complex cases (indirectly phased-out
/// permanents, phasing and Auras/Equipment) require additional infrastructure.
public final class PhasingAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.UNTAP_STEP_PHASING;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        var activePlayer = state.activePlayer();
        var battlefield = state.battlefield();

        // Gather all permanents that will phase in (phased-out permanents controlled by active player)
        List<Permanent> toPhaseIn = new ArrayList<>();
        for (var permanent : battlefield.controlledBy(activePlayer)) {
            if (permanent.isPhasedOut() && permanent.canPhaseIn()) {
                toPhaseIn.add(permanent);
            }
        }

        // Gather all permanents that will phase out (permanents with phasing controlled by active player)
        // TODO: Check for "phasing" ability - for now we assume no permanents have phasing
        // When ability system is implemented, we would check:
        // if (permanent.hasAbility(StaticAbility.PHASING))
        List<Permanent> toPhaseOut = new ArrayList<>();
        // for (var permanent : battlefield.controlledBy(activePlayer)) {
        //     if (permanent.isPhasedIn() && permanent.hasPhasing() && permanent.canPhaseOut()) {
        //         toPhaseOut.add(permanent);
        //     }
        // }

        // Now perform all phasing simultaneously (no events triggered)
        // Phase in first, then phase out (Rule 702.26d)
        for (var permanent : toPhaseIn) {
            permanent.phaseIn();
        }
        for (var permanent : toPhaseOut) {
            permanent.phaseOut();
        }
    }
}
