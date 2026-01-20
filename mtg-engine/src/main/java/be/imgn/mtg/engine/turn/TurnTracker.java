package be.imgn.mtg.engine.turn;

/// Main orchestrator for the turn system ({@mtg.rule 500}).
///
/// The TurnTracker manages the flow of the game through turns, phases, and steps.
/// It handles:
/// - Running turns in sequence
/// - Processing phases and steps within each turn
/// - Priority rounds and stack resolution
/// - State-based action checks
/// - Duration expiration
///
/// The GameState is injected via dependency injection, not passed to run().
///
/// @see TurnState
/// @see Phase
/// @see Step
public interface TurnTracker {

    /// Starts running the game, processing turns until the game ends.
    ///
    /// This method runs the main game loop, processing turns, phases, and steps.
    /// It continues until the game ends (a player wins, all players lose, etc.).
    void run();

    /// Ends the current turn immediately ({@mtg.rule 720.4}).
    ///
    /// This is used by effects like "end the turn" (Time Stop, Sundial of the Infinite).
    /// The current turn ends immediately:
    /// 1. All spells and abilities on the stack are exiled
    /// 2. The current phase/step ends
    /// 3. The cleanup step begins (can repeat if state-based actions apply)
    /// 4. The turn ends
    void endTurnEarly();

    /// Returns the current turn state.
    ///
    /// @return the turn state for accessing turn number, active player, etc.
    TurnState turnState();
}
