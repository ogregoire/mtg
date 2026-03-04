package be.imgn.mtg.engine.action;

import java.util.ArrayList;
import java.util.List;

/// Result of validating a player action.
///
/// Action validation determines if an action can be legally taken
/// in the current game state.
public sealed interface ValidationResult {

    /// The action is legal and can be executed.
    record Legal() implements ValidationResult {}

    /// A single validation error with a reason and type.
    ///
    /// @param reason a human-readable description of why the action is illegal
    /// @param type the type of illegality for programmatic handling
    record ValidationError(String reason, IllegalActionType type) {}

    /// The action is illegal and cannot be executed.
    ///
    /// @param errors the list of validation errors
    record Illegal(List<ValidationError> errors) implements ValidationResult {}

    /// Merges multiple validation results, collecting all errors.
    ///
    /// Returns [Legal] if all results are legal, or [Illegal] with
    /// all accumulated errors otherwise.
    static ValidationResult merge(ValidationResult... results) {
        var errors = new ArrayList<ValidationError>();
        for (var result : results) {
            if (result instanceof Illegal illegal) {
                errors.addAll(illegal.errors());
            }
        }
        return errors.isEmpty() ? new Legal() : new Illegal(List.copyOf(errors));
    }
}
