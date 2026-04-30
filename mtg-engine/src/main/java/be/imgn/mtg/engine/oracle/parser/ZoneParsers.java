package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.AmountParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PLURAL_ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.PlayerRef;
import be.imgn.mtg.engine.oracle.domain.PronounType;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;
import be.imgn.mtg.engine.oracle.domain.ZoneName;

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
                            phrase("a player's"),
                            word("your"),
                            word("their"),
                            word("its")),
                    ZONE_NAME,
                    Zone::named),
            phrase("the").then(ZONE_NAME).map(Zone.Named::new),
            // "a/an <zone>" — indefinite zone reference, used in
            // zone-agnostic triggers like Planar Void's "put into a
            // graveyard from anywhere". No possessive is recorded.
            phrase("[a|an]").then(ZONE_NAME).map(Zone.Named::new),
            ZONE_NAME.map(Zone.Named::new));

    // ── Zone destination ───────────────────────────────────────────────

    /// Optional "under [your|their|its owner's] control" tail on an
    /// "onto the battlefield" destination (Restore: "Put target land
    /// card from a graveyard onto the battlefield under your
    /// control."). Sets the [Zone.OntoBattlefield#controller] so
    /// downstream resolution knows which player gains control.
    private static final Parser<String> UNDER_CONTROL = phrase("under")
            .then(anyOf(
                    phrase("their owners'"),
                    phrase("its owner's"),
                    phrase("their owner's"),
                    word("your"),
                    word("their")))
            .followedBy(word("control"));

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD = phrase("onto the battlefield")
            .thenReturn(Zone.Destination.ontoBattlefield(false, null))
            .optionallyFollowedBy(UNDER_CONTROL, (d, c) -> Zone.Destination.ontoBattlefield(false, c));

    private static final Parser<Zone.Destination> ONTO_BATTLEFIELD_TAPPED = phrase("onto the battlefield tapped")
            .thenReturn(Zone.Destination.ontoBattlefield(true, null))
            .optionallyFollowedBy(UNDER_CONTROL, (d, c) -> Zone.Destination.ontoBattlefield(true, c));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD_TAPPED = phrase("to the battlefield tapped")
            .thenReturn(Zone.Destination.ontoBattlefield(true, null))
            .optionallyFollowedBy(UNDER_CONTROL, (d, c) -> Zone.Destination.ontoBattlefield(true, c));

    private static final Parser<Zone.Destination> TO_BATTLEFIELD = phrase("to the battlefield")
            .thenReturn(Zone.Destination.ontoBattlefield(false, null))
            .optionallyFollowedBy(UNDER_CONTROL, (d, c) -> Zone.Destination.ontoBattlefield(false, c));

    /// Library-owner possessive — matches either a pronoun ("your", "their",
    /// "its") or the possessive phrase "its owner's" / "their owners'" /
    /// "that player's" (e.g., Uproot: "Put target land on top of its
    /// owner's library."; Painful Memories: "Put that card on top of
    /// that player's library.").
    private static final Parser<String> LIBRARY_POSSESSIVE = anyOf(
            phrase("their owners'"),
            phrase("its owner's"),
            phrase("that player's"),
            word("your"),
            word("their"),
            word("its"));

    private static final Parser<Zone.Destination> TOP_OF_LIBRARY = anyOf(
            phrase("on top of")
                    .then(LIBRARY_POSSESSIVE)
                    .followedBy(phrase("[libraries|library]"))
                    .map(Zone.Destination::topOfLibrary),
            // "on top" — shorthand for "on top of [library]" when the
            // library is implicit from a preceding clause (Cruel Tutor /
            // Imperial Seal / Vampiric Tutor: "Search your library for a
            // card, then shuffle and put that card on top."). Defaults
            // the possessive to null so the engine can resolve it against
            // the just-searched library.
            phrase("on top").thenReturn(Zone.Destination.topOfLibrary(null)));

    private static final Parser<Zone.Destination> BOTTOM_OF_LIBRARY = phrase("on the bottom of")
            .then(LIBRARY_POSSESSIVE)
            .followedBy(phrase("[libraries|library]"))
            .map(Zone.Destination::bottomOfLibrary);

    /// "on \[chooser\]'s choice of the top or bottom of \[possessive\] library"
    /// — chooser-picks-end destination (Misleading Motes).
    private static final Parser<Zone.Destination> CHOICE_OF_TOP_OR_BOTTOM_OF_LIBRARY = Parser.sequence(
            phrase("on")
                    .then(anyOf(
                            word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                            anyOf(word("their"), word("his"), word("her"))
                                    .thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
                            word("its").thenReturn(Subject.pronoun(PronounType.IT))))
                    .followedBy(phrase("choice of the top or bottom of")),
            LIBRARY_POSSESSIVE.followedBy(phrase("[libraries|library]")),
            Zone.Destination.ChoiceOfTopOrBottomOfLibrary::new);

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

    /// "into \[possessive\] library just beneath the top N cards of that
    /// library" — variable-depth library insertion (Unexpectedly Absent).
    /// Must precede [#INTO_ZONE] in [#ZONE_DESTINATION] since it shares
    /// the "into" prefix and is strictly more specific.
    private static final Parser<Zone.Destination> INTO_LIBRARY_BENEATH_TOP = sequence(
            phrase("into").then(INTO_ZONE_POSSESSIVE).followedBy(phrase("library just beneath the top")),
            AMOUNT.followedBy(phrase("cards of that library")),
            (possessive, depth) -> Zone.Destination.beneathTopCards(depth, possessive));

    /// "into \[possessive\] \[zone\] or \[zone\]" — player-chosen destination
    /// between two zones sharing the same possessive (Dina's Guidance:
    /// "put it into your hand or graveyard"). Must precede [#INTO_ZONE]
    /// since it shares the "into" prefix and is strictly more specific.
    private static final Parser<Zone.Destination.ChoiceOfZones> INTO_CHOICE_OF_ZONES = sequence(
            phrase("into").then(INTO_ZONE_POSSESSIVE),
            ZONE_NAME.followedBy(word("or")),
            ZONE_NAME,
            Zone.Destination.ChoiceOfZones::new);

    private static final Parser<Zone.Destination> INTO_ZONE = phrase("into")
            .then(INTO_ZONE_POSSESSIVE)
            .then(ZONE_NAME)
            .map(name -> Zone.Destination.intoZone(null, name))
            .optionallyFollowedBy(INTO_ZONE_POSITION, (z, _) -> z);

    /// "\[first|second|third|fourth\] from the \[top|bottom\]" — ordinal
    /// library destination (Long-Term Plans: "put that card third
    /// from the top."). Implicit possessive is the controller's own
    /// library.
    private static final Parser<Zone.Destination> NTH_FROM_LIBRARY_END = sequence(
            anyOf(
                    phrase("First").thenReturn(1),
                    phrase("Second").thenReturn(2),
                    phrase("Third").thenReturn(3),
                    phrase("Fourth").thenReturn(4)),
            phrase("from the")
                    .then(anyOf(
                            word("top").thenReturn(Zone.Destination.NthFromLibraryEnd.End.TOP),
                            word("bottom").thenReturn(Zone.Destination.NthFromLibraryEnd.End.BOTTOM))),
            Zone.Destination.NthFromLibraryEnd::new);

    /// "to the command zone" — commander-specific destination. Used by
    /// effects that explicitly move commanders to the command zone
    /// (Leadership Vacuum: "Target player returns each commander they
    /// control from the battlefield to the command zone.").
    private static final Parser<Zone.Destination> TO_COMMAND_ZONE =
            phrase("to the command zone").thenReturn(Zone.Destination.intoZone(null, ZoneName.COMMAND));

    public static final Parser<Zone.Destination> ZONE_DESTINATION = anyOf(
            ONTO_BATTLEFIELD_TAPPED,
            TO_BATTLEFIELD_TAPPED, // must precede TO_BATTLEFIELD
            ONTO_BATTLEFIELD,
            TO_BATTLEFIELD,
            CHOICE_OF_TOP_OR_BOTTOM_OF_LIBRARY, // must precede TOP_OF_LIBRARY ("on …" shared prefix)
            TOP_OF_LIBRARY,
            BOTTOM_OF_LIBRARY,
            NTH_FROM_LIBRARY_END,
            TO_HAND,
            TO_COMMAND_ZONE,
            INTO_LIBRARY_BENEATH_TOP, // must precede INTO_ZONE (shares "into" prefix)
            INTO_CHOICE_OF_ZONES, // must precede INTO_ZONE (shares "into" prefix)
            INTO_ZONE);

    // ── Zone source ────────────────────────────────────────────────────

    private static final Parser<Zone.Source> FROM_ZONE =
            phrase("from").then(ZONE).map(Zone.Source::fromZone);

    /// "from \[all\]? [plural-zone]" — bulk-zone source (Faerie Macabre:
    /// "Exile up to two target cards from graveyards."; Rise of the
    /// Dark Realms: "from all graveyards"). Captured as a
    /// possessive-less named zone; the "all" is flavor since the
    /// bulk-zone form already implies every matching zone.
    private static final Parser<Zone.Source> FROM_PLURAL_ZONE = anyOf(
                    phrase("from all").then(PLURAL_ZONE_NAME), phrase("from").then(PLURAL_ZONE_NAME))
            .map(z -> Zone.Source.fromZone(new Zone.Named(null, z)));

    private static final Parser<Zone.Source> FROM_AMONG =
            phrase("from among").thenReturn(Zone.Source.fromAmong("from among"));

    /// "from anywhere \[other than \[zone\]\]?" — zone-agnostic source
    /// (Planar Void: "Whenever another card is put into a graveyard
    /// from anywhere"). With an exclusion (Vega, the Watcher:
    /// "cast a spell from anywhere other than your hand") captured
    /// structurally as [Zone.Source.FromAnywhereExcept].
    private static final Parser<Zone.Source> FROM_ANYWHERE = phrase("from anywhere")
            .<Zone.Source>thenReturn(Zone.Source.fromAmong("anywhere"))
            .optionallyFollowedBy(phrase("other than").then(ZONE), (_, ex) -> new Zone.Source.FromAnywhereExcept(ex));

    public static final Parser<Zone.Source> ZONE_SOURCE = anyOf(FROM_AMONG, FROM_ANYWHERE, FROM_PLURAL_ZONE, FROM_ZONE);
}
