package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;

/// "The Ring tempts (player)." ({@mtg.rule 701.54}) — `player` picks
/// a creature they control to become their Ring-bearer, and the
/// emblem named "The Ring" tracks their tempt count to unlock
/// abilities. Birthday Escape ("Draw a card. The Ring tempts you.").
/// The subject of the verb is always "The Ring" (a fixed game-state
/// concept), so it isn't represented in the record.
public record RingTemptsEffect(PlayerSelector player) implements Effect {
    public RingTemptsEffect {
        requireNonNull(player);
    }
}
