package be.imgn.mtg.engine.turn;

import java.util.List;

/// A phase within a turn ({@mtg.rule 500.1}).
///
/// A turn consists of phases, and most phases contain steps. The main phase
/// is special in that it has no steps - it's a single period where players
/// can cast sorcery-speed spells.
///
/// @see PhaseType
/// @see Step
public interface Phase {

    /// Returns the type of this phase.
    ///
    /// @return the phase type
    PhaseType type();

    /// Returns the occurrence number of this phase in the current turn.
    ///
    /// Most phases occur once per turn, but the main phase occurs twice
    /// (pre-combat = 1, post-combat = 2). Extra phases created by effects
    /// increment this counter.
    ///
    /// @return the 1-indexed occurrence number
    int occurrence();

    /// Returns the steps that make up this phase.
    ///
    /// The main phase returns an empty list as it has no steps.
    ///
    /// @return an unmodifiable list of steps in this phase
    List<Step> steps();
}
