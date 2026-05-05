package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X discards Y." ({@mtg.rule 701.8}). The `what` slot describes the
/// card(s) discarded — typically a card noun phrase wrapped in
/// [be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector.Hand].
/// `atRandom` flags the "at random" variant ({@mtg.rule 701.8d}) —
/// the game (not the discarding player) chooses the card.
public record DiscardEffect(Selector who, Selector what, boolean atRandom) implements Effect {
    public DiscardEffect {
        requireNonNull(who);
        requireNonNull(what);
    }

    /// Convenience: non-random discard. Used by parser plumbing
    /// (`DiscardEffect::new` as a `BiFunction<Selector, Selector,
    /// DiscardEffect>` for [be.imgn.mtg.engine.oracle2.parser.effect.EffectParser#subjectVerb]).
    public DiscardEffect(Selector who, Selector what) {
        this(who, what, false);
    }
}
