package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X sacrifices Y." ({@mtg.rule 701.16}). `what` is a battlefield
/// noun phrase the controller chooses among.
public record SacrificeEffect(Selector who, Selector what) implements Effect {
    public SacrificeEffect {
        requireNonNull(who);
        requireNonNull(what);
    }
}
