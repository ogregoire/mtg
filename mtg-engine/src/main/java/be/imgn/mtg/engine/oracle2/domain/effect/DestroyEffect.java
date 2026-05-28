package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Destroy X." ({@mtg.rule 701.7}). `target` typically wraps the
/// chosen permanent in
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector.Target]
/// when oracle text reads "destroy target …".
public record DestroyEffect(Selector selector) implements Effect {
    public DestroyEffect {
        requireNonNull(selector);
    }
}
