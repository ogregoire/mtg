package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Duration;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Attack-side combat restriction ({@mtg.rule 508.1}, {@mtg.rule 802.1}).
/// Umbrella for the two related restriction shapes — the absolute
/// "can't attack (X)?" form and the conditional "can't attack
/// alone" form. Both share `subject` and `duration` accessors so
/// downstream code can read them uniformly; case-match for per-
/// shape resolution semantics.
public sealed interface CantAttackEffect extends Effect {

    /// The creatures the restriction applies to.
    Selector subject();

    /// Temporal scope. [Duration.Fixed#PERMANENT] for the default
    /// continuous form; bounded scopes for "this turn"-style cards.
    Duration duration();

    /// "(subject) can't attack (defender)? (duration)." The
    /// absolute / partially-restricted form. When `defender` is
    /// `null` the subject can't attack at all; when set the
    /// restriction is scoped to a specific player (Blazing Archon:
    /// "Creatures can't attack you." — `defender` = YOU).
    record Of(Selector subject, @Nullable PlayerSelector defender, Duration duration) implements CantAttackEffect {
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

    /// "(subject) can't attack alone (duration)." ({@mtg.rule 802.1})
    /// — the subject may only be declared as an attacker if at
    /// least one other creature is also declared as an attacker
    /// that turn. Conditional, not absolute. Bonded Construct /
    /// Raging Kronch: "This creature can't attack alone.".
    record Alone(Selector subject, Duration duration) implements CantAttackEffect {
        public Alone {
            requireNonNull(subject);
            requireNonNull(duration);
        }

        public Alone(Selector subject) {
            this(subject, Duration.Fixed.PERMANENT);
        }
    }
}
