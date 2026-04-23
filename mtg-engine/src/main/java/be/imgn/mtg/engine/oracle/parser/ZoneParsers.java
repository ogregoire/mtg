package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Zone;

/// Parsers for zones, zone destinations, and zone sources in oracle text.
final class ZoneParsers {
    private ZoneParsers() {}

    // ── Zone ───────────────────────────────────────────────────────────

    public static final Parser<Zone> ZONE = Parser.anyOf(
            phrase("the battlefield").thenReturn(Zone.battlefield()),
            phrase("Exile").thenReturn(Zone.exile()),
            sequence(
                    anyOf(
                            phrase("its owner's"),
                            phrase("their owner's"),
                            phrase("their owners'"),
                            phrase("an opponent's"),
                            word("your"),
                            word("their"),
                            word("its")),
                    SelectorParsers.ZONE_NAME,
                    Zone::named),
            phrase("the").then(SelectorParsers.ZONE_NAME).map(Zone.Named::new),
            SelectorParsers.ZONE_NAME.map(Zone.Named::new));

    // ── Zone destination ───────────────────────────────────────────────

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD =
            phrase("onto the battlefield").thenReturn(Zone.Destination.ontoBattlefield(false, null));

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD_TAPPED =
            phrase("onto the battlefield tapped").thenReturn(Zone.Destination.ontoBattlefield(true, null));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD_TAPPED =
            phrase("to the battlefield tapped").thenReturn(Zone.Destination.ontoBattlefield(true, null));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD =
            phrase("to the battlefield").thenReturn(Zone.Destination.ontoBattlefield(false, null));

    /// Library-owner possessive — matches either a pronoun ("your", "their",
    /// "its") or the possessive phrase "its owner's" / "their owners'"
    /// (e.g., Uproot: "Put target land on top of its owner's library.";
    /// Harmonic Convergence: "Put all enchantments on top of their owners'
    /// libraries.").
    private static final Parser<String> LIBRARY_POSSESSIVE =
            anyOf(phrase("their owners'"), phrase("its owner's"), word("your"), word("their"), word("its"));

    private static final Parser<Zone.Destination> TOP_OF_LIBRARY = phrase("on top of")
            .then(LIBRARY_POSSESSIVE)
            .followedBy(phrase("[libraries|library]"))
            .map(Zone.Destination::topOfLibrary);

    private static final Parser<Zone.Destination> BOTTOM_OF_LIBRARY = phrase("on the bottom of")
            .then(LIBRARY_POSSESSIVE)
            .followedBy(phrase("[libraries|library]"))
            .map(Zone.Destination::bottomOfLibrary);

    private static final Parser<Zone.Destination> TO_HAND = anyOf(
            phrase("to their owners' hands").thenReturn(Zone.Destination.toHand("their owners'")),
            phrase("to its owner's hand").thenReturn(Zone.Destination.toHand("its owner's")),
            phrase("to their owner's hand").thenReturn(Zone.Destination.toHand("their owner's")),
            phrase("to your hand").thenReturn(Zone.Destination.toHand("your")),
            phrase("to their hand").thenReturn(Zone.Destination.toHand("their")));

    /// Possessives that can prefix an "into [X] [zone]" destination —
    /// pronouns or "its owner's" / "their owners'" phrases (Pull from
    /// Eternity: "into its owner's graveyard").
    private static final Parser<String> INTO_ZONE_POSSESSIVE =
            anyOf(phrase("their owners'"), phrase("its owner's"), word("your"), word("their"), word("its"));

    /// Optional ordinal-from-the-top/bottom tail on an "into library"
    /// destination (Chronostutter: "into its owner's library second from
    /// the top."). Consumed as flavor since [Zone.Destination.IntoZone]
    /// only carries the zone identity for now.
    private static final Parser<String> INTO_ZONE_POSITION =
            phrase("[First|Second|Third|Fourth] from the [top|bottom]");

    private static final Parser<Zone.Destination> INTO_ZONE = phrase("into")
            .then(INTO_ZONE_POSSESSIVE)
            .then(SelectorParsers.ZONE_NAME)
            .map(name -> Zone.Destination.intoZone(null, name))
            .optionallyFollowedBy(INTO_ZONE_POSITION, (z, _) -> z);

    public static final Parser<Zone.Destination> ZONE_DESTINATION = anyOf(
            ONTO_BATTLEFIELD_TAPPED,
            TO_BATTLEFIELD_TAPPED, // must precede TO_BATTLEFIELD
            ONTO_BATTLEFIELD,
            TO_BATTLEFIELD,
            TOP_OF_LIBRARY,
            BOTTOM_OF_LIBRARY,
            TO_HAND,
            INTO_ZONE);

    // ── Zone source ────────────────────────────────────────────────────

    private static final Parser<Zone.Source> FROM_ZONE =
            phrase("from").then(ZONE).map(Zone.Source::fromZone);

    /// "from [plural-zone]" — bulk-zone source (Faerie Macabre: "Exile
    /// up to two target cards from graveyards."). Captured as a
    /// possessive-less named zone.
    private static final Parser<Zone.Source> FROM_PLURAL_ZONE = phrase("from")
            .then(SelectorParsers.PLURAL_ZONE_NAME)
            .map(z -> Zone.Source.fromZone(new Zone.Named(null, z)));

    private static final Parser<Zone.Source> FROM_AMONG =
            phrase("from among").thenReturn(Zone.Source.fromAmong("from among"));

    public static final Parser<Zone.Source> ZONE_SOURCE = anyOf(FROM_AMONG, FROM_PLURAL_ZONE, FROM_ZONE);
}
