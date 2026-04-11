package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

/// Parsers for zones, zone destinations, and zone sources in oracle text.
final class ZoneParsers {
    private ZoneParsers() {}

    // ── Zone ───────────────────────────────────────────────────────────

    public static final Parser<Zone> ZONE = anyOf(
            w("the").then(w("battlefield")).thenReturn(Zone.battlefield()),
            w("exile").thenReturn(Zone.exile()),
            sequence(
                    anyOf(w("your"), w("their"), w("its"), w("an").then(w("opponent's"))),
                    SelectorParsers.ZONE_NAME,
                    Zone::named),
            w("the").then(SelectorParsers.ZONE_NAME).map(Zone.Named::new),
            SelectorParsers.ZONE_NAME.map(Zone.Named::new));

    // ── Zone destination ───────────────────────────────────────────────

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD =
            w("onto").then(w("the").then(w("battlefield"))).thenReturn(Zone.Destination.ontoBattlefield(false, null));

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD_TAPPED = w("onto")
            .then(w("the").then(w("battlefield")).then(w("tapped")))
            .thenReturn(Zone.Destination.ontoBattlefield(true, null));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD =
            w("to").then(w("the").then(w("battlefield"))).thenReturn(Zone.Destination.ontoBattlefield(false, null));

    private static final Parser<Zone.Destination> TOP_OF_LIBRARY = w("on").then(w("top").then(w("of")))
            .then(anyOf(w("your"), w("their"), w("its")))
            .followedBy(w("library"))
            .map(Zone.Destination::topOfLibrary);

    private static final Parser<Zone.Destination> BOTTOM_OF_LIBRARY = w("on").then(
                    w("the").then(w("bottom")).then(w("of")))
            .then(anyOf(w("your"), w("their"), w("its")))
            .followedBy(w("library"))
            .map(Zone.Destination::bottomOfLibrary);

    private static final Parser<Zone.Destination> TO_HAND = anyOf(
            w("to").then(w("its").then(w("owner's")).then(w("hand")))
                    .thenReturn(Zone.Destination.toHand("its owner's")),
            w("to").then(w("your").then(w("hand"))).thenReturn(Zone.Destination.toHand("your")),
            w("to").then(w("their").then(w("hand"))).thenReturn(Zone.Destination.toHand("their")));

    private static final Parser<Zone.Destination> INTO_ZONE = w("into")
            .then(anyOf(w("your"), w("their"), w("its")))
            .then(SelectorParsers.ZONE_NAME)
            .map(name -> Zone.Destination.intoZone(null, name));

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
            w("from").then(w("among")).thenReturn(Zone.Source.fromAmong("from among"));

    public static final Parser<Zone.Source> ZONE_SOURCE = anyOf(FROM_ZONE, FROM_AMONG);
}
