package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Untap X." ({@mtg.rule 701.20}). `target` typically wraps the
/// chosen permanent in
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector.Target]
/// when oracle text reads "untap target …" (Twiddle, Early Harvest).
public record UntapEffect(Selector target) implements Effect {
    public UntapEffect {
        requireNonNull(target);
    }
}
