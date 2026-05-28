package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X shuffles Y's library." ({@mtg.rule 701.20}). The `who` slot is
/// the shuffling player; the `owner` slot identifies whose library
/// is shuffled. Bare "Shuffle your library." has no explicit subject;
/// the parser fills `who` with the implicit "you" in that case (the
/// resolving controller does the shuffling).
public record ShuffleEffect(Selector who, PlayerSelector owner) implements Effect {
    public ShuffleEffect {
        requireNonNull(who);
        requireNonNull(owner);
    }
}
