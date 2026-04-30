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

import be.imgn.mtg.engine.oracle.domain.PlayerRef;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;

/// Shared zone prepositional phrases — the "in \[zone\]" / "from \[zone\]"
/// suffixes that appear on many effects (zone-changes, count-of
/// expressions, play-lands-from). Produces [Zone.Owned] / [Zone.Source]
/// values consumed by effect parsers.
final class ZoneExpressionParsers {
    private ZoneExpressionParsers() {}

    // Matches: "in <zone>"
    /// "in [possessive] [zone]" suffix — used by count-of expressions such
    /// as "for each card in your hand". Possessive is flavor.
    static final Parser<Zone.Owned> IN_ZONE = anyOf(
            phrase("in [your|their|its|a|any]").then(ZONE_NAME).map(z -> ZoneParsers.ownedZone("your", z)),
            // "in each <zone>" — distributive every-zone scope
            // (Life Burst: "for each card named Life Burst in each
            // graveyard.").
            phrase("in each").then(ZONE_NAME).map(z -> ZoneParsers.ownedZone("each", z)));

    /// "from all [zone-plural] and [zone-plural]" — two-zone bulk source with
    /// universal scope (Worldfire: "from all hands and graveyards").
    /// Produces a [Zone.Source.FromZones] with [PlayerRef.Pronoun#EACH_PLAYER]
    /// owner so the engine reads it as every player's copy of both zones.
    static final Parser<Zone.Source> ALL_ZONES_FROM = phrase("from all")
            .then(sequence(
                    PLURAL_ZONE_NAME.followedBy(word("and")),
                    PLURAL_ZONE_NAME,
                    (a, b) -> Zone.Source.fromZones(List.of(
                            (Zone) ZoneParsers.ownedZone("each", a), (Zone) ZoneParsers.ownedZone("each", b)))));

    // Matches: "from <zone>" (single-card or bulk-plural)
    /// "from [possessive] [single]? [zone]" or "from [zone]" suffix — e.g.,
    /// "play lands from your graveyard", "cast this card from exile", "exile
    /// X target cards from a single graveyard". Possessive and "single" are
    /// flavor.
    static final Parser<Zone.Owned> IN_ZONE_FROM = phrase("from")
            .then(anyOf(
                    phrase("[your|their|its|a|an|any|the] single?")
                            .then(ZONE_NAME)
                            .map(z -> ZoneParsers.ownedZone("your", z)),
                    ZONE_NAME.map(z -> ZoneParsers.ownedZone("your", z)),
                    // "from all graveyards" — explicit bulk-zone form
                    // (Rise of the Dark Realms). "all" is flavor since
                    // the plural-zone reading already implies every
                    // matching zone.
                    word("all").then(PLURAL_ZONE_NAME).map(z -> ZoneParsers.ownedZone("each", z)),
                    // "from graveyards" / "from libraries" — bulk-zone
                    // source (Faerie Macabre: "Exile up to two target
                    // cards from graveyards.").
                    PLURAL_ZONE_NAME.map(z -> ZoneParsers.ownedZone("each", z))));

    /// "from [player-ref]'s [zone] and [zone]" — combined two-zone source
    /// (e.g., Identity Crisis: "from target player's hand and graveyard").
    /// The parsed [Zone.Source.FromZones] keeps the typed owner per
    /// element.
    static final Parser<Zone.Source> MULTI_ZONE_FROM = sequence(
            phrase("from").then(SubjectParsers.PLAYER_REF).followedBy(string("'s")),
            sequence(ZONE_NAME.followedBy(word("and")), ZONE_NAME, (a, b) -> List.of(a, b)),
            (PlayerRef ref, List<Zone.Name> zones) -> {
                var owner = Subject.player(ref);
                return Zone.Source.fromZones(List.of((Zone) ZoneParsers.ownedZone(owner, zones.get(0)), (Zone)
                        ZoneParsers.ownedZone(owner, zones.get(1))));
            });

    /// "from [player-ref]'s [zone]" — single-zone source keyed on a player
    /// reference (Leonin of the Lost Pride: "from an opponent's graveyard").
    /// Parallel to [#MULTI_ZONE_FROM] with exactly one zone.
    static final Parser<Zone.Source> PLAYER_ZONE_FROM = sequence(
            phrase("from").then(SubjectParsers.PLAYER_REF).followedBy(string("'s")),
            ZONE_NAME,
            (PlayerRef ref, Zone.Name zone) -> Zone.Source.fromZone(ZoneParsers.ownedZone(Subject.player(ref), zone)));
}
