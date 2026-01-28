package be.imgn.mtg.engine.turn.internal;

/// A single state-based action ({@mtg.rule 704}).
///
/// State-based actions are automatic game actions that happen whenever
/// certain conditions are met. They don't use the stack and no player
/// receives priority while they're being processed.
///
/// Each implementation receives `GameState` via its constructor and
/// combines the check and apply logic into a single method.
public interface StateBasedAction {

    /// Checks if this state-based action currently applies and, if so, applies it.
    ///
    /// @return true if this SBA's condition was met and it was applied
    boolean checkAndApply();
}
