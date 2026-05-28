package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X mills N cards." ({@mtg.rule 701.13}) — `who` puts the top N
/// cards of their library into their graveyard. Bare "Mill N cards."
/// has no explicit subject; the parser fills `who` with the implicit
/// "you" in that case.
public record MillEffect(Selector who, Amount amount) implements Effect {
    public MillEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
