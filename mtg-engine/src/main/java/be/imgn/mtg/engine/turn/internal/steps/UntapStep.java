package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the untap step ({@mtg.rule 502}).
///
/// During the untap step:
/// 1. Phasing occurs ({@mtg.rule 502.1})
/// 2. The day/night cycle is checked ({@mtg.rule 502.2})
/// 3. The active player untaps their permanents ({@mtg.rule 502.3})
///
/// No player receives priority during this step ({@mtg.rule 502.4}).
public final class UntapStep extends AbstractStep {

    public UntapStep(int occurrence) {
        super(StepType.UNTAP, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Phasing (Rule 502.1)
        // Permanents with phasing phase out or in

        // TODO: Day/Night check (Rule 502.2)
        // If it's day and the active player didn't cast spells last turn, it becomes night
        // If it's night and the active player cast two or more spells last turn, it becomes day

        // TODO: Untap (Rule 502.3)
        // Active player untaps all permanents they control
        // Respects "doesn't untap" restrictions
    }

    @Override
    public boolean hasPriority() {
        // No player receives priority during the untap step
        return false;
    }
}
