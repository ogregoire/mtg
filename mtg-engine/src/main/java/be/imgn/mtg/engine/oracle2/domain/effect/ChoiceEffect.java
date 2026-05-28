package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import java.util.List;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Choose one: \<effect\>; \<effect\>; …" — the resolving controller
/// picks exactly one of the alternatives at resolution time
/// ({@mtg.rule 700.2a}). Used today by the "tap or untap \<X\>" /
/// "destroy or exile \<X\>" family, where the verb is chosen but the
/// subject is shared across alternatives.
///
/// Structural invariant: there is **exactly one** `subject`
/// expression in the AST. Each `alternative` references it via the
/// matching per-axis sentinel
/// ([be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector.Bound#PLAYER]
/// or [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector.Bound#OBJECT])
/// in its own target slot — so no engine code can mistakenly latch
/// two different targets (CR 115.1: a target chosen once is one
/// target). Mirrors [SharedSubjectEffect] on the OR-composition
/// axis.
///
/// The slot is named `subject` (not `target`) because the captured
/// selector isn't always a target — "Tap or untap all creatures."
/// has no `target` word but is structurally identical to "Tap or
/// untap target creature." (only the quantifier wrapper differs).
public record ChoiceEffect(Selector subject, List<Effect> alternatives) implements Effect {
    public ChoiceEffect {
        requireNonNull(subject);
        alternatives = List.copyOf(alternatives);
        if (alternatives.size() < 2) {
            throw new IllegalArgumentException(
                    "ChoiceEffect needs at least 2 alternatives, got " + alternatives.size());
        }
    }
}
