package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Counter X (if (condition))?." ({@mtg.rule 701.5}). `selector`
/// typically wraps the targeted spell in
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector.Target]
/// when oracle text reads "counter target …". `condition`, when
/// non-null, gates the counter at resolution time (Ertai's
/// Trickery: "Counter target spell if it was kicked." —
/// `condition = WasKicked(Bound.OBJECT)`).
public record CounterEffect(Selector selector, @Nullable Condition condition) implements Effect {
    public CounterEffect {
        requireNonNull(selector);
    }

    public CounterEffect(Selector selector) {
        this(selector, null);
    }

    public CounterEffect withCondition(Condition condition) {
        return new CounterEffect(selector, condition);
    }
}
