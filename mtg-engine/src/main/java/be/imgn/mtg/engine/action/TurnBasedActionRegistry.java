package be.imgn.mtg.engine.action;

import java.util.List;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Registry of turn-based actions indexed by their timing.
///
/// The registry stores all turn-based actions and provides methods to
/// retrieve and execute actions for specific timing points.
///
/// @see TurnBasedAction
/// @see TurnBasedTiming
public interface TurnBasedActionRegistry {

    /// Returns all actions that execute at the given timing.
    ///
    /// @param timing the timing point to query
    /// @return the list of actions for this timing, may be empty
    List<TurnBasedAction> getActionsFor(TurnBasedTiming timing);

    /// Executes all actions for the given timing.
    ///
    /// Actions are executed in registration order.
    ///
    /// @param timing the timing point to execute
    /// @param state the current game state
    /// @param processor the event processor for game state changes
    void executeAll(TurnBasedTiming timing, GameState state, GameEventProcessor processor);
}
