package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Exile X." ({@mtg.rule 701.20}).
public record ExileEffect(Selector target) implements Effect {
    public ExileEffect {
        requireNonNull(target);
    }
}
