package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.MayPlayLandFromZoneEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parser for [MayPlayLandFromZoneEffect]. Surface form "(player)
/// may play lands from (zone)." Crucible of Worlds / Ramunap
/// Excavator ("You may play lands from your graveyard."). The
/// alternative-zone slot is currently limited to graveyard; exile /
/// library forms get new arms here as cards demand them. The
/// possessive-owner-zone reference ("(owner)'s graveyard") is
/// parsed inline because [ZoneParser#ZONE_SELECTOR]'s graveyard arm
/// expects an `<object-type> in <owner> graveyard` shape, not a
/// bare zone.
public final class MayPlayLandFromZoneEffectParser {
    private MayPlayLandFromZoneEffectParser() {}

    private static final ObjectTypeSelector.Card ANY_CARD =
            new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING);

    /// "(owner) graveyard" — bare graveyard zone reference. Reuses
    /// [ZoneParser#POSSESSIVE_OWNER] for the owner (your / their /
    /// target player's / etc.).
    private static final Parser<ZoneSelector> GRAVEYARD = ZoneParser.POSSESSIVE_OWNER
            .followedBy(phrase("graveyard"))
            .map(owner -> new ZoneSelector.Graveyard(owner, ANY_CARD));

    /// Source zone — extend with `EXILE`, `LIBRARY`, etc. when
    /// cards demand them.
    private static final Parser<ZoneSelector> SOURCE_ZONE = anyOf(GRAVEYARD);

    /// "(player) may play lands from (zone)" — full sentence; period
    /// consumed at the [EffectParser] level.
    public static final Parser<MayPlayLandFromZoneEffect> MAY_PLAY_LAND_FROM_ZONE = sequence(
            PlayerSelectorParser.PLAYER_SELECTOR.followedBy(phrase("may play lands from")),
            SOURCE_ZONE,
            MayPlayLandFromZoneEffect::new);
}
