package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Flip (selector)." ({@mtg.rule 110.5b}) — turn a flip card to its
/// alternate face. Student of Elements: "When this creature has
/// flying, flip it." `selector` is typically a self-reference
/// (Bound.OBJECT in trigger context).
public record FlipEffect(Selector selector) implements Effect {
    public FlipEffect {
        requireNonNull(selector);
    }
}
