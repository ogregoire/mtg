package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

/// "(who) may play lands from (zone)." ({@mtg.rule 305} land play,
/// {@mtg.rule 718.2} alternative-zone play permission). `zone`
/// captures the non-default source of the land cards — typically a
/// graveyard or exile zone, with its `owner` slot referencing the
/// relevant player. Crucible of Worlds / Ramunap Excavator ("You
/// may play lands from your graveyard." — `who` = YOU, `zone` =
/// `Graveyard(YOU, …)`).
public record MayPlayLandFromZoneEffect(PlayerSelector who, ZoneSelector zone) implements Effect {
    public MayPlayLandFromZoneEffect {
        requireNonNull(who);
        requireNonNull(zone);
    }
}
