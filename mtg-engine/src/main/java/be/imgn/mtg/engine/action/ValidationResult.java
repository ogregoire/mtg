package be.imgn.mtg.engine.action;

/// Result of validating a player action.
///
/// Action validation determines if an action can be legally taken
/// in the current game state.
public sealed interface ValidationResult {

    /// The action is legal and can be executed.
    record Legal() implements ValidationResult {}

    /// The action is illegal and cannot be executed.
    ///
    /// @param reason a human-readable description of why the action is illegal
    /// @param type the type of illegality for programmatic handling
    record Illegal(String reason, IllegalActionType type) implements ValidationResult {}
}
