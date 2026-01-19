package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Represents how long an effect lasts.
public sealed interface Duration {

    /// Until the end of the current turn.
    record UntilEndOfTurn() implements Duration {}

    /// Until the beginning of your next turn.
    record UntilYourNextTurn() implements Duration {}

    /// The effect is permanent (no duration specified).
    record Permanent() implements Duration {}
}
