package be.imgn.mtg.engine.oracle2.domain;

/// Duration of a continuous effect ({@mtg.rule 611.2}). Sealed at the
/// fixed-vocabulary [Fixed] enum today; parameterised arms (per-step
/// bounds, per-player turn bounds, "for as long as" conditions) land
/// here as new permits when cards demand them.
///
/// Every effect that can carry a duration treats this as a mandatory
/// field with [Fixed#PERMANENT] as the no-end-specified default —
/// `@Nullable Duration` is intentionally not used. PERMANENT
/// represents the rules-default "applies indefinitely while the
/// source is on the battlefield" semantics ({@mtg.rule 604}); other
/// values bound the effect to a temporal scope.
public sealed interface Duration {

    /// Fixed, parameterless temporal scopes. Each constant is a self-
    /// contained scope that needs no further data. PERMANENT is the
    /// no-duration default; the others map to specific oracle-text
    /// phrases parsed by
    /// [be.imgn.mtg.engine.oracle2.parser.DurationParser].
    enum Fixed implements Duration {
        /// No oracle phrase — the absent case. An effect with this
        /// duration applies as long as the source remains on the
        /// battlefield ({@mtg.rule 604}).
        PERMANENT,
        /// "this turn" — scoped to the currently-active turn. Chaos:
        /// "Creatures can't block this turn.".
        THIS_TURN
    }
}
