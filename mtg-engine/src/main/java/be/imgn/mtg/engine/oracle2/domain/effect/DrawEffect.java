package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X draws N cards." ({@mtg.rule 121}). Bare "Draw N cards." has no
/// explicit subject; the parser fills `who` with the implicit "you"
/// in that case.
public record DrawEffect(Selector who, Amount amount) implements Effect {
    public DrawEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
