package be.imgn.mtg.engine.oracle.domain;

import be.imgn.mtg.engine.turn.Step;

/// When a delayed triggered ability (rule 603.7) fires. Delayed
/// timings reference a *future* occurrence — distinct from the
/// always-on `at the beginning of …` triggers captured by
/// [TriggerEvent.OwnerScoped] — so they need their own shapes.
public sealed interface DelayedTiming {

    /// "the beginning of \[scope\] \[step\]" — the most common delayed
    /// shape (Blessed Wine: "at the beginning of the next turn's
    /// upkeep."; Mystic Remora: "at the beginning of your next
    /// upkeep.").
    record BeginningOfStep(Scope scope, Step step) implements DelayedTiming {}

    /// "end of turn" / "the end of turn" — the cleanup-adjacent
    /// delayed window (common on "until end of turn"-style effects
    /// that also need a scheduled cleanup).
    enum EndOfTurn implements DelayedTiming {
        END_OF_TURN
    }

    /// "end of combat" — scheduled at the end of the current combat
    /// phase.
    enum EndOfCombat implements DelayedTiming {
        END_OF_COMBAT
    }

    /// Whose next occurrence of the step counts. `NEXT_TURN` is the
    /// special "the next turn's \[step\]" phrasing where the owner is
    /// whoever has the upcoming turn; the concrete-owner variants
    /// (`YOUR_NEXT`, …) scope to that player's next turn only.
    enum Scope {
        /// "your next \[step\]".
        YOUR_NEXT,
        /// "the next turn's \[step\]" — owner is the active player of
        /// the next turn.
        NEXT_TURN,
        /// "an opponent's next \[step\]".
        OPPONENT_NEXT,
        /// "each \[step\]" — recurring across every occurrence.
        EACH
    }
}
