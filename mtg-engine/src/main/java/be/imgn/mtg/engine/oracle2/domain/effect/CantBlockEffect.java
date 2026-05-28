package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Block-side combat restriction ({@mtg.rule 509.1}, {@mtg.rule 802.1}).
/// Umbrella for the two related restriction shapes — the absolute
/// "can't block (X)?" form and the conditional "can't block alone"
/// form. Both share `subject` and `duration` accessors so downstream
/// code can read them uniformly; case-match for per-shape
/// resolution semantics.
public sealed interface CantBlockEffect extends Effect {

    /// The creatures the restriction applies to.
    Selector subject();

    /// Temporal scope. [Duration.Fixed#PERMANENT] for the default
    /// continuous form; bounded scopes for "this turn"-style cards
    /// (Chaos).
    Duration duration();

    /// "(subject) can't block (what)? (duration)." The absolute /
    /// partially-restricted form. When `what` is `null` the subject
    /// can't block any creature (Frenetic Raptor: "Beasts can't
    /// block."); when set the restriction is partial (Hunted Ghoul:
    /// "This creature can't block Humans." — Hunted Ghoul can still
    /// block non-Humans).
    record Of(Selector subject, @Nullable Selector what, Duration duration) implements CantBlockEffect {
        public Of {
            requireNonNull(subject);
            requireNonNull(duration);
        }

        public Of(Selector subject) {
            this(subject, null, Duration.Fixed.PERMANENT);
        }

        public Of(Selector subject, Duration duration) {
            this(subject, null, duration);
        }
    }

    /// "(subject) can't block alone (duration)." ({@mtg.rule 802.1})
    /// — the subject may only be declared as a blocker if at least
    /// one other creature is also declared as a blocker that combat.
    /// Conditional, not absolute. Craven Hulk: "This creature can't
    /// block alone.".
    record Alone(Selector subject, Duration duration) implements CantBlockEffect {
        public Alone {
            requireNonNull(subject);
            requireNonNull(duration);
        }

        public Alone(Selector subject) {
            this(subject, Duration.Fixed.PERMANENT);
        }
    }
}
