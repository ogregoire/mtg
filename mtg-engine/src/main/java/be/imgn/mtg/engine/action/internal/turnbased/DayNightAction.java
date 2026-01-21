package be.imgn.mtg.engine.action.internal.turnbased;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Handles day/night transitions at the beginning of the untap step ({@mtg.rule 502.2}).
///
/// The day/night cycle is tracked by the game and can change based on the number
/// of spells cast during the previous turn:
/// - If it's day and the active player cast no spells during their previous turn,
///   it becomes night.
/// - If it's night and the active player cast two or more spells during their
///   previous turn, it becomes day.
///
/// Note: This is a stub implementation. Day/night requires:
/// - Game-wide day/night state tracking
/// - Per-turn spell counting
/// - DayNightEvent for the GameEventProcessor
public final class DayNightAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.UNTAP_STEP_DAY_NIGHT;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        // TODO: Implement day/night tracking
        // 1. Check if the game is tracking day/night (it only starts once a
        //    daybound/nightbound card exists)
        // 2. Get the number of spells the active player cast during their
        //    previous turn
        // 3. If it's day and they cast 0 spells, transition to night
        // 4. If it's night and they cast 2+ spells, transition to day
        // 5. Process any day/night change events
    }
}
