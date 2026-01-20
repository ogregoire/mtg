package be.imgn.mtg.engine.turn;

/// Tracks how many times each phase and step has occurred in the current turn.
///
/// This is used to distinguish between the first and second main phase,
/// multiple combat damage steps (first strike), extra phases, etc.
///
/// @see PhaseType
/// @see StepType
public interface OccurrenceTracker {

    /// Increments the occurrence count for a phase and returns the new count.
    ///
    /// @param phase the phase type
    /// @return the occurrence number (1 for first occurrence, 2 for second, etc.)
    int increment(PhaseType phase);

    /// Increments the occurrence count for a step and returns the new count.
    ///
    /// @param step the step type
    /// @return the occurrence number (1 for first occurrence, 2 for second, etc.)
    int increment(StepType step);

    /// Returns the current occurrence count for a phase.
    ///
    /// @param phase the phase type
    /// @return the current count (0 if the phase hasn't occurred yet this turn)
    int count(PhaseType phase);

    /// Returns the current occurrence count for a step.
    ///
    /// @param step the step type
    /// @return the current count (0 if the step hasn't occurred yet this turn)
    int count(StepType step);

    /// Resets all occurrence counts to zero.
    ///
    /// This is called at the start of each turn.
    void reset();
}
