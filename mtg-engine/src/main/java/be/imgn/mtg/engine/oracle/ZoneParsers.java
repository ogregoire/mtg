package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

/// Parsers for zones, zone destinations, and zone sources in oracle text.
final class ZoneParsers {
    private ZoneParsers() {}

    // ── Zone ───────────────────────────────────────────────────────────

    public static final Parser<Zone> ZONE = anyOf(
            ciWords("the battlefield").thenReturn(Zone.battlefield()),
            w("exile").thenReturn(Zone.exile()),
            sequence(
                    anyOf(
                            ciWords("its owner's"),
                            ciWords("their owner's"),
                            ciWords("their owners'"),
                            ciWords("an opponent's"),
                            anyCiWord("your", "their", "its")),
                    SelectorParsers.ZONE_NAME,
                    Zone::named),
            w("the").then(SelectorParsers.ZONE_NAME).map(Zone.Named::new),
            SelectorParsers.ZONE_NAME.map(Zone.Named::new));

    // ── Zone destination ───────────────────────────────────────────────

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD =
            ciWords("onto the battlefield").thenReturn(Zone.Destination.ontoBattlefield(false, null));

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD_TAPPED =
            ciWords("onto the battlefield tapped").thenReturn(Zone.Destination.ontoBattlefield(true, null));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD =
            ciWords("to the battlefield").thenReturn(Zone.Destination.ontoBattlefield(false, null));

    /// Library-owner possessive — matches either a pronoun ("your", "their",
    /// "its") or the possessive phrase "its owner's" / "their owners'"
    /// (e.g., Uproot: "Put target land on top of its owner's library.";
    /// Harmonic Convergence: "Put all enchantments on top of their owners'
    /// libraries.").
    private static final Parser<String> LIBRARY_POSSESSIVE =
            anyOf(ciWords("their owners'"), ciWords("its owner's"), anyCiWord("your", "their", "its"));

    private static final Parser<Zone.Destination> TOP_OF_LIBRARY = ciWords("on top of")
            .then(LIBRARY_POSSESSIVE)
            .followedBy(anyCiWord("libraries", "library"))
            .map(Zone.Destination::topOfLibrary);

    private static final Parser<Zone.Destination> BOTTOM_OF_LIBRARY = ciWords("on the bottom of")
            .then(LIBRARY_POSSESSIVE)
            .followedBy(anyCiWord("libraries", "library"))
            .map(Zone.Destination::bottomOfLibrary);

    private static final Parser<Zone.Destination> TO_HAND = anyOf(
            ciWords("to their owners' hands").thenReturn(Zone.Destination.toHand("their owners'")),
            ciWords("to its owner's hand").thenReturn(Zone.Destination.toHand("its owner's")),
            ciWords("to their owner's hand").thenReturn(Zone.Destination.toHand("their owner's")),
            ciWords("to your hand").thenReturn(Zone.Destination.toHand("your")),
            ciWords("to their hand").thenReturn(Zone.Destination.toHand("their")));

    /// Possessives that can prefix an "into [X] [zone]" destination —
    /// pronouns or "its owner's" / "their owners'" phrases (Pull from
    /// Eternity: "into its owner's graveyard").
    private static final Parser<String> INTO_ZONE_POSSESSIVE =
            anyOf(ciWords("their owners'"), ciWords("its owner's"), anyCiWord("your", "their", "its"));

    /// Optional ordinal-from-the-top/bottom tail on an "into library"
    /// destination (Chronostutter: "into its owner's library second from
    /// the top."). Consumed as flavor since {@link Zone.Destination.IntoZone}
    /// only carries the zone identity for now.
    private static final Parser<String> INTO_ZONE_POSITION = anyCiWord("first", "second", "third", "fourth")
            .then(ciWords("from the"))
            .then(anyCiWord("top", "bottom"));

    private static final Parser<Zone.Destination> INTO_ZONE = w("into")
            .then(INTO_ZONE_POSSESSIVE)
            .then(SelectorParsers.ZONE_NAME)
            .map(name -> Zone.Destination.intoZone(null, name))
            .optionallyFollowedBy(INTO_ZONE_POSITION, (z, _) -> z);

    public static final Parser<Zone.Destination> ZONE_DESTINATION = anyOf(
            ONTO_BATTLEFIELD_TAPPED,
            ONTO_BATTLEFIELD,
            TO_BATTLEFIELD,
            TOP_OF_LIBRARY,
            BOTTOM_OF_LIBRARY,
            TO_HAND,
            INTO_ZONE);

    // ── Zone source ────────────────────────────────────────────────────

    private static final Parser<Zone.Source> FROM_ZONE = w("from").then(ZONE).map(Zone.Source::fromZone);

    private static final Parser<Zone.Source> FROM_AMONG =
            ciWords("from among").thenReturn(Zone.Source.fromAmong("from among"));

    public static final Parser<Zone.Source> ZONE_SOURCE = anyOf(FROM_ZONE, FROM_AMONG);
}
