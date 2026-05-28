package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "(who) take(s) an extra turn after this one." ({@mtg.rule 500.7})
/// — `who` takes an additional turn immediately after the currently-
/// active turn ends. Time Walk, Temporal Manipulation, Capture of
/// Jingzhou. The bare imperative "Take an extra turn after this
/// one." has no explicit subject; the parser fills `who` with the
/// implicit "you" in that case.
public record TakeExtraTurnEffect(Selector who) implements Effect {
    public TakeExtraTurnEffect {
        requireNonNull(who);
    }
}
