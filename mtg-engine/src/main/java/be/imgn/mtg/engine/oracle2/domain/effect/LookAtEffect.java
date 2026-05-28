package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

/// "(who) look(s) at (zone)." ({@mtg.rule 701.10}) — `who` looks at
/// a hidden zone, then it's hidden again from `who` afterward
/// unless the effect specifies otherwise. Glasses of Urza: "Look at
/// target player's hand.". `zone` is a [ZoneSelector] identifying
/// the looked-at zone (hand, library top N, …).
public record LookAtEffect(Selector who, ZoneSelector zone) implements Effect {
    public LookAtEffect {
        requireNonNull(who);
        requireNonNull(zone);
    }
}
