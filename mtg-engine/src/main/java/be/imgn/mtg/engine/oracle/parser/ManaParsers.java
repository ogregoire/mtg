package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

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

/// Parsers for [Effect.AddMana] and the [Restriction.SpendOnly]
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
    /// Reflecting Pool's `Palette.ProducedBy` to refer to the source
    /// of the producible mana palette.
    private static Subject landsSelector(PlayerRef.Pronoun controller) {
        return Subject.select(new Selector(
                        Selector.Quantifier.one(),
                        List.of(new Selector.Qualifier.Types(TypeMatcher.LAND)),
                        GameObjectType.PERMANENT)
                .withController(
                        Selector.ControllerClause.does(new Selector.ControllerClause.Body.Controls(controller))));
    }

    /// "Basic land you control" — used by Star Compass's `Palette.ProducedBy`
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

    static final Parser<Mana> MANA = Parser.<Mana>anyOf(
            // "one mana of any color in your commander's color identity" —
            // Command Tower / Arcane Signet. Commander color identity is
            // flavor in the current model; the palette is still the five
            // basic colors.
            phrase("One mana of any color in your commander's color identity")
                    .thenReturn(new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.Explicit(BASIC_COLORS))),
            // "one mana of any color that a basic land you control could
            // produce" — Star Compass. The palette is whatever basic
            // lands you control can produce, captured as [Palette.ProducedBy].
            sequence(
                            phrase("One mana of any [color|type] that a basic land"),
                            anyOf(
                                    phrase("you control").thenReturn(basicLandsSelector(PlayerRef.Pronoun.YOU)),
                                    phrase("an opponent controls")
                                            .thenReturn(basicLandsSelector(PlayerRef.Pronoun.AN_OPPONENT))),
                            (_, source) -> new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.ProducedBy(source)))
                    .followedBy(phrase("could produce")),
            // "one mana of any [color|type] that a land you control could
            // produce" — Reflecting Pool / Naga Vitalist / Harvester
            // Druid. The palette is whatever those lands actually
            // produce, captured as [Palette.ProducedBy].
            sequence(
                            phrase("One mana of any [color|type] that a land"),
                            anyOf(
                                    phrase("you control").thenReturn(landsSelector(PlayerRef.Pronoun.YOU)),
                                    phrase("an opponent controls")
                                            .thenReturn(landsSelector(PlayerRef.Pronoun.AN_OPPONENT))),
                            (_, source) -> new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.ProducedBy(source)))
                    .followedBy(phrase("could produce")),
            // "one mana of any type the sacrificed land could produce"
            // — Squandered Resources. Palette comes from the
            // just-sacrificed land's mana ability.
            phrase("One mana of any [color|type] the sacrificed land could produce")
                    .thenReturn(new Mana.OfOneColor(
                            Amount.exact(1),
                            new Mana.Palette.ProducedBy(Subject.demonstrative("the sacrificed", "land")))),
            // "one mana of any type that land could produce" — Benthic
            // Explorers; "one mana of any type that land produced" —
            // Heartbeat of Spring (past-tense variant). Both
            // back-reference the same land.
            phrase("One mana of any")
                    .then(phrase("[color|type]"))
                    .followedBy(anyOf(phrase("that land could produce"), phrase("that land produced")))
                    .thenReturn(new Mana.OfOneColor(
                            Amount.exact(1), new Mana.Palette.ProducedBy(Subject.demonstrative("that", "land")))),
            // "one mana of any color among [subject]" — color palette
            // restricted to colors *appearing on* the referenced set
            // (Mox Amber). Must precede the bare "any color" arm so
            // the "among …" tail wins.
            phrase("One mana of any color among")
                    .then(SubjectParsers.SUBJECT)
                    .map(among -> new Mana.OfOneColor(Amount.exact(1), new Mana.Palette.AmongColorsOf(among))),
            // "one mana of any color" — unambiguous shorthand for one
            // of any basic color.
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
            // "<amount> mana in any combination of <symbol> and/or <symbol>..."
            // — restricted-palette combination (Orcish Lumberjack:
            // "three mana in any combination of {R} and/or {G}").
            sequence(
                    AMOUNT.followedBy(phrase("mana in any combination of")),
                    EffectParsers.MANA_SYMBOL.atLeastOnceDelimitedBy(
                            anyOf(word("and/or"), word("and"), word("or")), Collectors.toUnmodifiableList()),
                    (amt, palette) -> new Mana.Mixed(amt, new Mana.Palette.Explicit(palette))),
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

    static final Parser<Effect.AddMana> ADD_MANA = Parser.<Effect.AddMana>anyOf(
                    // "[player] adds …" — player-actor form (Tangleroot:
                    // "that player adds {G}.").
                    sequence(
                            SubjectParsers.PLAYER_SUBJECTS
                                    .followedBy(phrase("add(s)"))
                                    .optionallyFollowedBy(phrase("an additional"), (s, _) -> s),
                            MANA,
                            (actor, mana) -> new Effect.AddMana(mana).withPlayer(actor)),
                    phrase("Add")
                            .optionallyFollowedBy(phrase("an additional"), (s, _) -> s)
                            .then(MANA)
                            .map(Effect.AddMana::new))
            // Optional trailing "where X is …" — binds the X in a
            // variable-mana expression (Mona Lisa). Consumed as flavor
            // for now since {@link Effect.AddMana} has no X slot.
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, (am, _) -> am)
            // Trailing "\[they|you\] choose" — flavor restating the
            // chooser (Spectral Searchlight). Consumed as flavor.
            .optionallyFollowedBy(phrase("[they|you] choose"), (am, _) -> am);

    /// "Spend this mana only to [restriction]." — Omen Hawker.
    /// "Spend this mana only on [restriction]." — Rosheen Meanderer.
    /// Fallback for restriction sentences that don't immediately
    /// follow an [Effect.AddMana] (e.g., Piracy: "Until end of turn,
    /// you may tap lands you don't control for mana. Spend this mana
    /// only to cast spells."). When the restriction *does* follow an
    /// AddMana, [#ADD_MANA] absorbs it into [Mana.Restricted] via
    /// [OracleParser]'s post-process fold.
    static final Parser<Effect.SpendThisManaOnly> SPEND_THIS_MANA_ONLY =
            SPEND_ONLY_RESTRICTION.map(Effect.SpendThisManaOnly::new);
}
