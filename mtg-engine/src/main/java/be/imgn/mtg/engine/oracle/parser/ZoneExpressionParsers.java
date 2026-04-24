package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PLURAL_ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Zone;

/// Shared zone prepositional phrases — the "in \[zone\]" / "from \[zone\]"
/// suffixes that appear on many effects (zone-changes, count-of
/// expressions, play-lands-from). Produces [Zone.Named] / [Zone.Source]
/// values consumed by effect parsers.
final class ZoneExpressionParsers {
    private ZoneExpressionParsers() {}

    // Matches: "in <zone>"
    /// "in [possessive] [zone]" suffix — used by count-of expressions such
    /// as "for each card in your hand". Possessive is flavor.
    static final Parser<Zone.Named> IN_ZONE =
            phrase("in [your|their|its|a|any]").then(ZONE_NAME).map(Zone.Named::new);

    // Matches: "from <zone>" (single-card or bulk-plural)
    /// "from [possessive] [single]? [zone]" or "from [zone]" suffix — e.g.,
    /// "play lands from your graveyard", "cast this card from exile", "exile
    /// X target cards from a single graveyard". Possessive and "single" are
    /// flavor.
    static final Parser<Zone.Named> IN_ZONE_FROM = phrase("from")
            .then(anyOf(
                    phrase("[your|their|its|a|any] single?").then(ZONE_NAME).map(Zone.Named::new),
                    ZONE_NAME.map(Zone.Named::new),
                    // "from all graveyards" — explicit bulk-zone form
                    // (Rise of the Dark Realms). "all" is flavor since
                    // the plural-zone reading already implies every
                    // matching zone.
                    word("all").then(PLURAL_ZONE_NAME).map(Zone.Named::new),
                    // "from graveyards" / "from libraries" — bulk-zone
                    // source (Faerie Macabre: "Exile up to two target
                    // cards from graveyards.").
                    PLURAL_ZONE_NAME.map(Zone.Named::new)));

    /// "from [player-ref]'s [zone] and [zone]" — combined two-zone source
    /// (e.g., Identity Crisis: "from target player's hand and graveyard").
    /// The parsed [Zone.Multi] keeps the shared possessive and the
    /// list of named zones.
    static final Parser<Zone.Source> MULTI_ZONE_FROM = sequence(
            phrase("from").then(SubjectParsers.PLAYER_REF).followedBy(string("'s")),
            sequence(ZONE_NAME.followedBy(word("and")), ZONE_NAME, (a, b) -> List.of(a, b)),
            (ref, zones) -> Zone.Source.fromZone(new Zone.Multi(ref.name().toLowerCase() + "'s", zones)));

    /// "from [player-ref]'s [zone]" — single-zone source keyed on a player
    /// reference (Leonin of the Lost Pride: "from an opponent's graveyard").
    /// Parallel to [#MULTI_ZONE_FROM] with exactly one zone.
    static final Parser<Zone.Source> PLAYER_ZONE_FROM = sequence(
            phrase("from").then(SubjectParsers.PLAYER_REF).followedBy(string("'s")),
            ZONE_NAME,
            (ref, zone) -> Zone.Source.fromZone(new Zone.Named(ref.name().toLowerCase() + "'s", zone)));
}
