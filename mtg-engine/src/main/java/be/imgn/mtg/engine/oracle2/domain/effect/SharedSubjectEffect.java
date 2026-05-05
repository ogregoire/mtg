package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import java.util.List;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Multiple effect clauses sharing one subject. The `subject` is
/// captured exactly once; each `clause` references it via the
/// matching per-axis sentinel
/// ([be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector.SharedSubject]
/// or
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector.SharedSubject]).
/// Produced by the effect parser when oracle text fans a single
/// subject across multiple verbs: "Target player draws two cards and
/// loses 2 life." (Unscrupulous Contractor).
///
/// Structural invariant: there is **exactly one** `subject`
/// expression in the AST. Clauses do not carry their own copy of
/// it, so no engine code can mistakenly latch a second target
/// (CR 115.1: a target chosen once is one target).
public record SharedSubjectEffect(Selector subject, List<Effect> clauses) implements Effect {
    public SharedSubjectEffect {
        requireNonNull(subject);
        requireNonNull(clauses);
        clauses = List.copyOf(clauses);
        if (clauses.size() < 2) {
            throw new IllegalArgumentException("SharedSubjectEffect needs at least 2 clauses, got " + clauses.size());
        }
    }
}
