package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;

/// "(player)'s life total becomes (amount)." ({@mtg.rule 119.6}) —
/// hard-sets a player's life total to a specific value (the engine
/// adjusts by the delta, not by a damage/lifegain path). Blessed
/// Wind: "Target player's life total becomes 20.".
public record SetLifeTotalEffect(PlayerSelector who, Amount amount) implements Effect {
    public SetLifeTotalEffect {
        requireNonNull(who);
        requireNonNull(amount);
    }
}
