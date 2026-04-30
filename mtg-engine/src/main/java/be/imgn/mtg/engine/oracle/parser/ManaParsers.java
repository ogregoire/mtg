package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Map;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.oracle.domain.AddManaEffect;
import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.GameObjectType;
import be.imgn.mtg.engine.oracle.domain.Mana;
import be.imgn.mtg.engine.oracle.domain.ManaSymbol;
import be.imgn.mtg.engine.oracle.domain.PlayerRef;
import be.imgn.mtg.engine.oracle.domain.Restriction;
import be.imgn.mtg.engine.oracle.domain.Selector;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.TypeMatcher;

/// Parsers for [AddManaEffect] and the [Restriction.SpendOnly]
/// fallback. The mana-payload grammar (`{G}`, `one mana of any
/// color`, `mana of any one color`, `mana in any combination`,
/// "could produce" palettes, etc.) is encoded in [#MANA]. The
/// imperative "Add …" / "[player] adds …" wrapping is in
/// [#ADD_MANA]. The "Spend this mana only …" sentence parses to
/// either a wrapped [Mana.Restricted] (when adjacent to ADD_MANA,
/// folded by [OracleParser]) or to [Effect.SpendThisManaOnly] as
/// a fallback (Piracy and similar).
final class ManaParsers {
    private ManaParsers() {}

    private static final List<ManaSymbol> BASIC_COLORS = List.of(
            new ManaSymbol("{W}"),
            new ManaSymbol("{U}"),
            new ManaSymbol("{B}"),
            new ManaSymbol("{R}"),
            new ManaSymbol("{G}"));

    /// "Lands you control" / "lands an opponent controls" — used by
    /// Reflecting Pool's `Palette.CouldProduce` to refer to the source
    /// of the producible mana palette.
    private static Subject landsSelector(PlayerRef.Pronoun controller) {
        return Subject.select(new Selector(
                        Selector.Quantifier.one(),
                        List.of(new Selector.Qualifier.Types(TypeMatcher.LAND)),
                        GameObjectType.PERMANENT)
                .withController(
                        Selector.ControllerClause.does(new Selector.ControllerClause.Body.Controls(controller))));
    }

    /// "Basic land you control" — used by Star Compass's `Palette.CouldProduce`
    /// to refer specifically to basic lands controlled by the player.
    private static Subject basicLandsSelector(PlayerRef.Pronoun controller) {
        return Subject.select(new Selector(
                        Selector.Quantifier.one(),
                        List.of(new Selector.Qualifier.Types(
                                new TypeMatcher.All(List.of(TypeMatcher.BASIC, TypeMatcher.LAND)))),
                        GameObjectType.PERMANENT)
                .withController(
                        Selector.ControllerClause.does(new Selector.ControllerClause.Body.Controls(controller))));
    }

    /// Or-list collapse: an oracle "Add A or B" with multiple literal
    /// arms becomes [Mana.AnyOf]; a singleton arm collapses to a bare
    /// [Mana.Exact].
    private static Mana orListToMana(List<Mana> exacts) {
        return exacts.size() == 1 ? exacts.getFirst() : new Mana.AnyOf(exacts);
    }

    /// Common trunk for "one mana of any [color|type] …" arms.
    /// Captures the [color|type] discriminator (rule 106.7: "color"
    /// excludes {C}, "type" includes it) so the tail can plumb it
    /// into dynamic palettes.
    private static final Parser<Mana.Palette.Filter> ANY_COLOR_OR_TYPE = phrase("One mana of any")
            .then(anyOf(
                    word("color").thenReturn(Mana.Palette.Filter.COLOR),
                    word("type").thenReturn(Mana.Palette.Filter.TYPE)));

    /// Tail of "that <land selector> [could produce|produced]". The
    /// `<could produce | produced>` choice maps to [Palette.CouldProduce]
    /// (potential) vs [Palette.Produced] (past actual).
    private static Parser<Mana.Palette> producedPalette(Subject source, Mana.Palette.Filter filter) {
        return anyOf(
                phrase("could produce").thenReturn(new Mana.Palette.CouldProduce(source, filter)),
                phrase("produced").thenReturn(new Mana.Palette.Produced(source, filter)));
    }

    /// "that <land selector>" — the source of a dynamic produce-palette.
    /// Covers "a basic land you control", "a land an opponent controls",
    /// the bare back-reference "land" (Subject.demonstrative("that",
    /// "land")), and selector forms via [SubjectParsers#SUBJECT].
    private static final Parser<Subject> THAT_LAND_SOURCE = phrase("that")
            .then(anyOf(
                    phrase("a basic land you control").thenReturn(basicLandsSelector(PlayerRef.Pronoun.YOU)),
                    phrase("a basic land an opponent controls")
                            .thenReturn(basicLandsSelector(PlayerRef.Pronoun.AN_OPPONENT)),
                    phrase("a land you control").thenReturn(landsSelector(PlayerRef.Pronoun.YOU)),
                    phrase("a land an opponent controls").thenReturn(landsSelector(PlayerRef.Pronoun.AN_OPPONENT)),
                    phrase("land").thenReturn(Subject.demonstrative("that", "land"))));

    static final Parser<Mana> MANA = Parser.<Mana>anyOf(
            // "One mana of any [color|type] …" — common-trunk arms. The
            // trunk captures [color|type] as the COLOR/TYPE filter; the
            // tail dispatches on the trailing structure.
            sequence(
                    ANY_COLOR_OR_TYPE,
                    Parser.<Mana.Palette>anyOf(
                            // "in your commander's color identity" — Command
                            // Tower / Arcane Signet. Color-identity is flavor;
                            // palette is the five basic colors. Always paired
                            // with "color" in oracle text.
                            phrase("in your commander's color identity")
                                    .thenReturn(new Mana.Palette.Explicit(BASIC_COLORS)),
                            // "among <subject>" — Mox Amber. Always paired
                            // with "color" (printed colors of objects).
                            phrase("among").then(SubjectParsers.SUBJECT).map(Mana.Palette.AmongColorsOf::new)),
                    (_, palette) -> new Mana.OfOneColor(Amount.exact(1), palette)),
            // "One mana of any [color|type] that <land-source>
            // [could produce|produced]" — Reflecting Pool / Star
            // Compass / Benthic Explorers (CouldProduce); Mirari's
            // Wake / Sisay / Heartbeat of Spring (Produced). Filter
            // is the COLOR/TYPE bit captured by the trunk.
            sequence(ANY_COLOR_OR_TYPE, THAT_LAND_SOURCE, (filter, source) -> Map.entry(filter, source))
                    .flatMap(fs -> producedPalette(fs.getValue(), fs.getKey()))
                    .map(palette -> new Mana.OfOneColor(Amount.exact(1), palette)),
            // "One mana of any [color|type] the sacrificed land could
            // produce" — Squandered Resources. The "the sacrificed
            // land" form doesn't share the "that <X>" prefix.
            sequence(
                    ANY_COLOR_OR_TYPE,
                    phrase("the sacrificed land could produce")
                            .thenReturn(Subject.demonstrative("the sacrificed", "land")),
                    (filter, source) ->
                            new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.CouldProduce(source, filter))),
            // "one mana of any color" — unambiguous shorthand for one
            // of any basic color (Spectral Searchlight, Rainbow Vale).
            // Bare "any type" doesn't appear in oracle text so no
            // matching arm.
            phrase("One mana of any color")
                    .thenReturn(new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.Explicit(BASIC_COLORS))),
            // "<amount> mana of [that|the chosen] color" — back-
            // reference to a color named earlier in the same resolution
            // (Meteor Crater, Sol Grail).
            AMOUNT.followedBy(phrase("mana of [that|the chosen] color")).map(Mana.OfThatColor::new),
            // "<amount> mana of any one color" — N copies of one
            // chosen basic color. Subsumes the prior 5-fold expansion
            // into one [OfOneColor].
            AMOUNT.followedBy(phrase("mana of any one color"))
                    .map(amt -> new Mana.OfOneColor(amt, new Mana.Palette.Explicit(BASIC_COLORS))),
            // "<amount> mana of different colors" — N distinct colors
            // (Firemind Vessel). [OfDistinctColors] preserves the
            // distinctness constraint that the engine can honor when
            // wiring is in place.
            AMOUNT.followedBy(phrase("mana of different colors"))
                    .map(amt -> new Mana.OfDistinctColors(amt, new Mana.Palette.Explicit(BASIC_COLORS))),
            // "<amount> mana in any combination of colors" — each of
            // N mana chosen independently from the five basic colors
            // (Manamorphose).
            AMOUNT.followedBy(phrase("mana in any combination of colors"))
                    .map(amt -> new Mana.Mixed(amt, new Mana.Palette.Explicit(BASIC_COLORS))),
            // "<amount> mana in any combination of <symbol> [and/or|and|or] <symbol>…"
            // — restricted-palette combination (Orcish Lumberjack:
            // "three mana in any combination of {R} and/or {G}").
            // Uses [MtgParsers#joinedList] so the parser accepts the
            // historical permissive set of connectors and preserves
            // which connector the oracle used on [Mana.Mixed].
            sequence(
                    AMOUNT.followedBy(phrase("mana in any combination of")),
                    MtgParsers.joinedList(EffectParsers.MANA_SYMBOL),
                    (amt, joined) ->
                            new Mana.Mixed(amt, new Mana.Palette.Explicit(joined.items()), joined.connector())),
            // "<symbol(s)> for each X" — `count` copies of the literal
            // symbol bundle. Mana Seism's "add that much {C}" takes
            // the next arm; this one handles patterns like "{C}{C} for
            // each card revealed this way".
            sequence(
                    EffectParsers.MANA_SYMBOL.atLeastOnce(),
                    CountOfParsers.FOR_EACH,
                    (syms, count) -> new Mana.Repeated(count, syms)),
            // "<amount> <symbol>" — amount-scaled single symbol
            // (Mana Seism: "add that much {C}").
            sequence(AMOUNT, EffectParsers.MANA_SYMBOL, (amt, sym) -> new Mana.Repeated(amt, List.of(sym))),
            // "an amount of <symbol> equal to <property>" — Viridian
            // Joiner: "Add an amount of {G} equal to this creature's
            // power.".
            sequence(
                    phrase("an amount of").then(EffectParsers.MANA_SYMBOL),
                    phrase("equal to").then(CountOfParsers.PROPERTY_OF_AMOUNT),
                    (sym, amt) -> new Mana.Repeated(amt, List.of(sym))),
            // Fallback: an or-list of fixed symbol groups
            // ({G}; {G}{G}; {2}{B}; or "{B} or {R}"; or "{U} or {C}{U}").
            // Singleton collapses to a bare [Exact]; multiple → [AnyOf].
            MtgParsers.orList(EffectParsers.MANA_SYMBOL.atLeastOnce().<Mana>map(Mana.Exact::new))
                    .map(ManaParsers::orListToMana));

    /// Token parser for the free-text tail of "Spend this mana only…":
    /// accepts contraction-like words plus mana-symbol braces so forms
    /// like "on costs that contain {X}" (Rosheen Meanderer) round-trip.
    private static final Parser<String> SPEND_MANA_TOKEN = consecutive(
            CharacterSet.charsIn("[A-Za-z0-9'-]").or(CharPredicate.is('{')).or(CharPredicate.is('}')),
            "spend-mana token");

    /// "Spend this mana only \[to|on\] <body>" — restriction body
    /// shared by [#ADD_MANA]'s absorption arm and the orphan-effect
    /// fallback [#SPEND_THIS_MANA_ONLY].
    static final Parser<Restriction> SPEND_ONLY_RESTRICTION = phrase("Spend this mana only")
            .then(phrase("[to|on]"))
            .then(SPEND_MANA_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Restriction.SpendOnly::new);

    static final Parser<AddManaEffect> ADD_MANA = Parser.<AddManaEffect>anyOf(
                    // "[player] adds …" — player-actor form (Tangleroot:
                    // "that player adds {G}.").
                    sequence(
                            SubjectParsers.PLAYER_SUBJECTS
                                    .followedBy(phrase("add(s)"))
                                    .optionallyFollowedBy(phrase("an additional"), (s, _) -> s),
                            MANA,
                            (actor, mana) -> new AddManaEffect(mana).withPlayer(actor)),
                    phrase("Add")
                            .optionallyFollowedBy(phrase("an additional"), (s, _) -> s)
                            .then(MANA)
                            .map(AddManaEffect::new))
            // Optional trailing "where X is …" — binds the X in a
            // variable-mana expression (Mona Lisa). Consumed as flavor
            // for now since {@link AddManaEffect} has no X slot.
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, (am, _) -> am)
            // Trailing "\[they|you\] choose" — flavor restating the
            // chooser (Spectral Searchlight). Consumed as flavor.
            .optionallyFollowedBy(phrase("[they|you] choose"), (am, _) -> am);

    /// "Spend this mana only to [restriction]." — Omen Hawker.
    /// "Spend this mana only on [restriction]." — Rosheen Meanderer.
    /// Fallback for restriction sentences that don't immediately
    /// follow an [AddManaEffect] (e.g., Piracy: "Until end of turn,
    /// you may tap lands you don't control for mana. Spend this mana
    /// only to cast spells."). When the restriction *does* follow an
    /// AddMana, [#ADD_MANA] absorbs it into [Mana.Restricted] via
    /// [OracleParser]'s post-process fold.
    static final Parser<Effect.SpendThisManaOnly> SPEND_THIS_MANA_ONLY =
            SPEND_ONLY_RESTRICTION.map(Effect.SpendThisManaOnly::new);
}
