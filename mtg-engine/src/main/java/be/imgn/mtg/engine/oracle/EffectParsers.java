package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Parsers for effect productions in oracle text.
final class EffectParsers {
    private EffectParsers() {}

    // ── Shared primitives ──────────────────────────────────────────────

    static final Parser<ManaSymbol> MANA_SYMBOL = string("{")
            .then(consecutive(CharPredicate.noneOf(" {}"), "mana content"))
            .followedBy(string("}"))
            .map(s -> new ManaSymbol("{" + s + "}"));

    private static final Parser<Integer> MOD_SIGN =
            anyOf(string("+").thenReturn(1), string("-").thenReturn(-1));

    /// One side of a P/T modifier — either `±X` (a
    /// {@link PtModifier.Component.Variable}) or a signed integer (a
    /// {@link PtModifier.Component.Fixed}). The `±X` branch is tried first
    /// so `+X` isn't misread as a numeric amount.
    private static final Parser<PtModifier.Component> PT_COMPONENT = anyOf(
            sequence(MOD_SIGN, word("X"), (sign, _) -> (PtModifier.Component) new PtModifier.Component.Variable(sign)),
            sequence(MOD_SIGN, SelectorParsers.INTEGER, (sign, value) ->
                    (PtModifier.Component) new PtModifier.Component.Fixed(sign * value)));

    static final Parser<PtModifier> PT_MODIFIER =
            sequence(PT_COMPONENT, string("/").then(PT_COMPONENT), PtModifier::new);

    /// Word-or-contraction token (e.g., "it's") used when capturing free
    /// predicate text — broader than {@link Parser#word()} so English
    /// contractions like "it's" and "isn't" round-trip.
    private static final Parser<String> WORD_OR_CONTRACTION =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'-]"), "word");

    /// "as long as [condition]" — {@link Duration.ForAsLongAs} captured as
    /// free text (allows English contractions such as "it's").
    private static final Parser<Duration> AS_LONG_AS = ciWords("as long as")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Duration::forAsLongAs);

    static final Parser<Duration> DURATION = anyOf(
            ciWords("until end of turn").thenReturn(Duration.untilEndOfTurn()),
            ciWords("until your next turn").thenReturn(Duration.untilYourNextTurn()),
            ciWords("until end of combat").thenReturn(Duration.untilEndOfCombat()),
            ciWords("this turn").thenReturn(Duration.thisTurn()),
            ciWords("on each of your turns").thenReturn(Duration.eachYourTurn()),
            AS_LONG_AS);

    private static final Parser<String> KEYWORD_NAME = anyOf(
            ciWords("first strike"),
            ciWords("double strike"),
            ciWords("death touch").thenReturn("deathtouch"),
            w("flying"),
            w("trample"),
            w("haste"),
            w("vigilance"),
            w("lifelink"),
            w("deathtouch"),
            w("hexproof"),
            w("indestructible"),
            w("menace"),
            w("reach"),
            w("defender"),
            w("flash"),
            w("fear"),
            w("intimidate"),
            w("shroud"),
            w("wither"),
            w("infect"),
            w("prowess"),
            word().suchThat(k -> k.length() > 2 && Character.isLowerCase(k.charAt(0)), "keyword name"));

    private static final Parser<List<String>> KEYWORD_LIST = KEYWORD_NAME.atLeastOnceDelimitedBy(",");

    // ── Predefined tokens ──────────────────────────────────────────────

    private static final Parser<TokenDescription> PREDEFINED_TOKEN = anyOf(
                    w("Treasure"),
                    w("Food"),
                    w("Gold"),
                    w("Clue"),
                    w("Blood"),
                    w("Powerstone"),
                    w("Map"),
                    w("Incubator"))
            .followedBy(anyWord("tokens", "token"))
            .map(TokenDescription::predefined);

    /// Subtype(s) + card type(s) preceding "token(s)". Returned as a pair
    /// (subtypes, cardTypes) so the CUSTOM_TOKEN parser can assemble them.
    private static final Parser<Map.Entry<List<String>, List<CardType>>> TOKEN_TAIL = anyOf(
            sequence(
                    SelectorParsers.SUBTYPE_NAME.atLeastOnce(),
                    SelectorParsers.CARD_TYPE.atLeastOnce().followedBy(anyWord("tokens", "token")),
                    Map::entry),
            SelectorParsers.CARD_TYPE
                    .atLeastOnce()
                    .followedBy(anyWord("tokens", "token"))
                    .map(types -> Map.entry(List.<String>of(), types)));

    /// Color list preceding a token body — either `colorless` (empty list)
    /// or one-or-more basic colors joined by `and` (e.g., "white and black").
    private static final Parser<List<Color>> TOKEN_COLORS =
            anyOf(w("colorless").thenReturn(List.<Color>of()), SelectorParsers.COLOR.atLeastOnceDelimitedBy("and"));

    /// Optional `with [keyword list]` suffix on a custom token (e.g., Advent
    /// of the Wurm: "Create a 5/5 green Wurm creature token with trample.").
    /// Emits the list of ability names so consumers can reconstruct the
    /// token's printed text — the grammar doesn't yet try to resolve each
    /// keyword to its structured {@link Ability} form in this context.
    private static final Parser<List<String>> TOKEN_ABILITIES = w("with")
            .then(KeywordParsers.KEYWORD.atLeastOnceDelimitedBy(
                    anyOf(string(","), w("and")), Collectors.toUnmodifiableList()))
            .map(list -> list.stream().map(a -> a.getClass().getSimpleName()).toList());

    private static final Parser<TokenDescription.Custom> CUSTOM_TOKEN_BARE = sequence(
            SelectorParsers.PT_VALUE,
            TOKEN_COLORS,
            TOKEN_TAIL,
            (pt, colors, tail) ->
                    new TokenDescription.Custom(pt, colors, List.of(), tail.getValue(), tail.getKey(), List.of()));

    private static final Parser<TokenDescription> CUSTOM_TOKEN = CUSTOM_TOKEN_BARE
            .<TokenDescription>map(c -> c)
            .optionallyFollowedBy(
                    TOKEN_ABILITIES,
                    (t, abilities) -> new TokenDescription.Custom(
                            ((TokenDescription.Custom) t).pt(),
                            ((TokenDescription.Custom) t).colors(),
                            ((TokenDescription.Custom) t).supertypes(),
                            ((TokenDescription.Custom) t).types(),
                            ((TokenDescription.Custom) t).subtypes(),
                            abilities));

    /// "a token that's a copy of [source]" — e.g., Myr Propagator:
    /// "Create a token that's a copy of this creature.".
    private static final Parser<TokenDescription> COPY_TOKEN = ciWords("token that's a copy of")
            .then(SubjectParsers.SUBJECT)
            .<TokenDescription>map(TokenDescription.CopyOf::new);

    private static final Parser<TokenDescription> TOKEN_DESCRIPTION = anyOf(PREDEFINED_TOKEN, COPY_TOKEN, CUSTOM_TOKEN);

    // ── Zone prepositional phrases (shared by several effects) ────────

    /// "in [possessive] [zone]" suffix — used by count-of expressions such as
    /// "for each card in your hand".
    private static final Parser<Zone.Named> IN_ZONE = sequence(
            w("in").then(anyCiWord("your", "their", "its", "a", "any")), SelectorParsers.ZONE_NAME, Zone.Named::new);

    /// "from [possessive] [single]? [zone]" or "from [zone]" suffix — e.g.,
    /// "play lands from your graveyard", "cast this card from exile", "exile
    /// X target cards from a single graveyard".
    private static final Parser<Zone.Named> IN_ZONE_FROM = w("from")
            .then(anyOf(
                    sequence(
                            anyCiWord("your", "their", "its", "a", "any"),
                            anyOf(w("single").then(SelectorParsers.ZONE_NAME), SelectorParsers.ZONE_NAME),
                            Zone.Named::new),
                    SelectorParsers.ZONE_NAME.map(zone -> new Zone.Named(null, zone))));

    // ── Effects ────────────────────────────────────────────────────────

    /// The implicit "you" subject — used when an effect omits the player
    /// (e.g., "Draw a card." = "you draw a card.").
    private static final Subject YOU = Subject.player(Subject.PlayerRef.YOU);

    // Removal

    /// "At [timing], …" phrasings used as trailing scheduler suffixes on
    /// actions like {@link #DESTROY} (e.g., Silent Assassin: "Destroy
    /// target blocking creature at end of combat.").
    private static final Parser<Duration> AT_TIMING = anyOf(
            ciWords("at end of combat").thenReturn(Duration.untilEndOfCombat()),
            ciWords("at end of turn").thenReturn(Duration.untilEndOfTurn()));

    static final Parser<Effect.Destroy> DESTROY = w("destroy")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.Destroy::new)
            .optionallyFollowedBy(AT_TIMING, Effect.Destroy::withAt);

    /// What follows the verb `exile`: either cards selected by a subject, or
    /// the contents of one or more zones ("all graveyards").
    private static final Parser<Exiled> EXILED = anyOf(
            ciWords("all").then(SelectorParsers.PLURAL_ZONE_NAME).<Exiled>map(Exiled.Zones::new),
            SubjectParsers.SUBJECT.<Exiled>map(Exiled.Objects::new));

    /// "from [player-ref]'s [zone] and [zone]" — combined two-zone source
    /// (e.g., Identity Crisis: "from target player's hand and graveyard").
    /// The parsed {@link Zone.Multi} keeps the shared possessive and the
    /// list of named zones.
    private static final Parser<Zone.Source> MULTI_ZONE_FROM = sequence(
            w("from").then(SubjectParsers.PLAYER_REF).followedBy(string("'s")),
            sequence(
                    SelectorParsers.ZONE_NAME.followedBy(w("and")), SelectorParsers.ZONE_NAME, (a, b) -> List.of(a, b)),
            (ref, zones) -> Zone.Source.fromZone(new Zone.Multi(ref.name().toLowerCase() + "'s", zones)));

    /// Exile head: a plain imperative "exile" (actor null) or a player
    /// subject followed by "exiles" (the parsed player becomes the
    /// {@link Effect.Exile#actor()}). Mudhole: "Target player exiles all
    /// land cards from their graveyard.".
    private static final Parser<@Nullable Subject> EXILE_HEAD = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("exiles", "exile")),
            w("exile").map(_ -> (Subject) null));

    static final Parser<Effect.Exile> EXILE = anyOf(
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    MULTI_ZONE_FROM,
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            sequence(EXILE_HEAD, EXILED, (actor, exiled) -> new Effect.Exile(exiled, null, actor)));

    /// Optional trailing "of [possessive] choice" clause on a sacrifice
    /// (e.g., Tremble: "Each player sacrifices a land of their choice.").
    /// Consumed as flavor — the grammar doesn't yet model the chooser.
    private static final Parser<String> OF_CHOICE =
            ciWords("of").then(anyCiWord("your", "their", "his", "her", "its")).followedBy(w("choice"));

    /// What follows `sacrifice[s]` — a full {@link Subject} so selectors
    /// ("a creature you control") and self-references ("this creature",
    /// Barbarian Outcast) both parse.
    private static final Parser<Subject> SACRIFICE_NO_PLAYER = anyCiWord("sacrifices", "sacrifice")
            .then(SubjectParsers.SUBJECT)
            .optionallyFollowedBy(OF_CHOICE, (sel, _) -> sel);

    static final Parser<Effect.Sacrifice> SACRIFICE = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECT, SACRIFICE_NO_PLAYER, Effect.Sacrifice::new),
            SACRIFICE_NO_PLAYER.map(what -> new Effect.Sacrifice(YOU, what)));

    /// "Return [subject] [from [zone]]? [to destination]." — the optional
    /// source zone (e.g., Auroral Procession: "… from your graveyard …")
    /// is captured structurally; most bounces omit it and it stays null.
    /// Tried with-from first so the optional arm doesn't shadow it.
    static final Parser<Effect.Bounce> BOUNCE = anyOf(
            sequence(
                    w("return").then(SubjectParsers.SUBJECT),
                    IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    ZoneParsers.ZONE_DESTINATION,
                    Effect.Bounce::new),
            sequence(
                    w("return").then(SubjectParsers.SUBJECT),
                    ZoneParsers.ZONE_DESTINATION,
                    (subject, dest) -> new Effect.Bounce(subject, null, dest)));

    // Damage & Life

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_SUBJ = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("deals", "deal")),
            SelectorParsers.AMOUNT.followedBy(ciWords("damage to")),
            SubjectParsers.SUBJECT,
            Effect.DealDamage::new);

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_VERB = sequence(
            w("deal").then(SelectorParsers.AMOUNT),
            ciWords("damage to").then(SubjectParsers.SUBJECT),
            (amount, target) -> new Effect.DealDamage(Subject.selfRef(null), amount, target));

    /// "[source] deals N damage to A and M damage to B." — split damage to
    /// two targets from the same source (e.g., Char). Emitted as a
    /// {@link Effect.Compound} of two {@link Effect.DealDamage} sharing the
    /// parsed source. Tried before the single-target forms so the full
    /// phrase is consumed as one compound effect.
    static final Parser<Effect.Compound> DEAL_DAMAGE_SPLIT = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("deals", "deal")),
            sequence(SelectorParsers.AMOUNT.followedBy(ciWords("damage to")), SubjectParsers.SUBJECT, Map::entry),
            sequence(
                    w("and").then(SelectorParsers.AMOUNT).followedBy(ciWords("damage to")),
                    SubjectParsers.SUBJECT,
                    Map::entry),
            (source, first, second) -> new Effect.Compound(
                    new Effect.DealDamage(source, first.getKey(), first.getValue()),
                    new Effect.DealDamage(source, second.getKey(), second.getValue())));

    // DEAL_DAMAGE assembled below, after PROPERTY_OF_AMOUNT is declared so
    // the trailing-amount variant ("damage to X equal to Y's power") can
    // reference it.

    // Count-of expressions ("for each …"). Declared up here because several
    // downstream effects (Draw, GainLife, LoseLife, AddMana) use them.

    /// Trailing "on the battlefield" zone scope — common in count-of phrases
    /// like "for each Goblin on the battlefield".
    private static final Parser<Zone.Named> ON_BATTLEFIELD =
            ciWords("on the battlefield").thenReturn(new Zone.Named(null, ZoneName.BATTLEFIELD));

    /// "for each [subject] [in zone | on the battlefield]" — a count-of
    /// expression. Produces an {@link Amount.CountOf} equal to the number of
    /// matching objects.
    private static final Parser<Amount.CountOf> FOR_EACH = ciWords("for each")
            .then(anyOf(
                    sequence(SubjectParsers.SUBJECT, IN_ZONE, Amount.CountOf::new),
                    sequence(SubjectParsers.SUBJECT, ON_BATTLEFIELD, Amount.CountOf::new),
                    SubjectParsers.SUBJECT.map(Amount.CountOf::new)));

    /// Optional "each" distributive prefix — "[subjects] each [verb]"
    /// (e.g., Hunters' Feast: "target players each gain 6 life").
    private static Parser<String> each(Parser<String> verb) {
        return anyOf(w("each").then(verb), verb);
    }

    /// A property name in a property-of expression.
    private static final Parser<String> PROPERTY_NAME =
            anyOf(anyCiWord("power", "toughness", "strength"), ciWords("life total"));

    /// Possessive pronouns ("your", "their", "its") mapped to a
    /// {@link Subject} — used as the owner of a property without the
    /// intervening `'s` (e.g., "your life total").
    private static final Parser<Subject> POSSESSIVE_OWNER = anyOf(
            ciWords("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            ciWords("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            ciWords("its").thenReturn(Subject.pronoun("it")));

    /// "[owner] [property]" or "[owner]'s [property]" — a property
    /// reference as an amount. Handles both "target creature's power"
    /// (needs the explicit `'s`) and "your life total" (possessive
    /// pronoun, no `'s`).
    private static final Parser<Amount> PROPERTY_OF_AMOUNT = anyOf(
            sequence(POSSESSIVE_OWNER, PROPERTY_NAME, Amount.PropertyOf::new),
            sequence(SubjectParsers.SUBJECT.followedBy(string("'s")), PROPERTY_NAME, Amount.PropertyOf::new));

    /// "[source] deals damage to [target] equal to [amount]." — amount
    /// trails the target (e.g., Solar Blaze: "Each creature deals damage
    /// to itself equal to its power."). Declared after
    /// {@link #PROPERTY_OF_AMOUNT} because the amount commonly references
    /// a property (e.g., "its power").
    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_TRAILING_AMOUNT = sequence(
            SubjectParsers.SUBJECT
                    .followedBy(anyCiWord("deals", "deal"))
                    .followedBy(w("damage"))
                    .followedBy(w("to")),
            SubjectParsers.SUBJECT,
            ciWords("equal to").then(anyOf(PROPERTY_OF_AMOUNT, SelectorParsers.AMOUNT)),
            (source, target, amount) -> new Effect.DealDamage(source, amount, target));

    /// Deal-damage dispatcher. Trailing-amount variant must precede the
    /// standard "[source] deals N damage to X" shape because both share
    /// the "[source] deals" prefix.
    static final Parser<Effect.DealDamage> DEAL_DAMAGE =
            anyOf(DEAL_DAMAGE_TRAILING_AMOUNT, DEAL_DAMAGE_SUBJ, DEAL_DAMAGE_VERB);

    /// Amount after "gain life" — either `N` followed by `life`, or the
    /// longer form `life equal to X's power` where the amount trails.
    private static final Parser<Amount> GAIN_LIFE_AMOUNT = anyOf(
            w("life").then(ciWords("equal to")).then(PROPERTY_OF_AMOUNT), SelectorParsers.AMOUNT.followedBy(w("life")));

    private static final Parser<Amount> GAIN_LIFE_NO_PLAYER =
            each(anyCiWord("gains", "gain")).then(GAIN_LIFE_AMOUNT).optionallyFollowedBy(FOR_EACH, (base, e) -> e);

    static final Parser<Effect.GainLife> GAIN_LIFE = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, GAIN_LIFE_NO_PLAYER, Effect.GainLife::new),
            GAIN_LIFE_NO_PLAYER.map(a -> new Effect.GainLife(YOU, a)));

    /// `half [possessive] life[, rounded up/down]` — an Amount for "lose
    /// half your life" style phrases (Cruel Bargain, Infernal Contract).
    /// Consumes the literal "life" word; default rounding is UP (the sole
    /// form used by current cards is "rounded up").
    private static final Parser<Amount> HALF_LIFE = ciWords("half")
            .then(anyCiWord("your", "their", "its"))
            .followedBy(w("life"))
            .thenReturn((Amount) new Amount.Half(
                    new Amount.PropertyOf(Subject.player(Subject.PlayerRef.YOU), "life total"),
                    Amount.Half.Rounding.UP))
            .optionallyFollowedBy(
                    string(",").then(ciWords("rounded")).then(anyCiWord("up", "down")),
                    (base, dir) -> new Amount.Half(
                            ((Amount.Half) base).base(),
                            dir.equalsIgnoreCase("down") ? Amount.Half.Rounding.DOWN : Amount.Half.Rounding.UP));

    private static final Parser<Amount> LOSE_LIFE_NO_PLAYER = each(anyCiWord("loses", "lose"))
            .then(anyOf(
                    HALF_LIFE,
                    SelectorParsers.AMOUNT.followedBy(w("life")).optionallyFollowedBy(FOR_EACH, (base, e) -> e)));

    static final Parser<Effect.LoseLife> LOSE_LIFE = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, LOSE_LIFE_NO_PLAYER, Effect.LoseLife::new),
            LOSE_LIFE_NO_PLAYER.map(a -> new Effect.LoseLife(YOU, a)));

    // Card Manipulation

    /// Amount following "draw[s] <amount> card(s)". A trailing "for each X"
    /// clause replaces the base amount with a count-of-X expression.
    private static final Parser<Amount> DRAW_AMOUNT = SelectorParsers.AMOUNT
            .followedBy(anyCiWord("cards", "card"))
            .optionallyFollowedBy(FOR_EACH, (base, each) -> each);

    private static final Parser<Amount> DRAW_NO_PLAYER =
            each(anyCiWord("draws", "draw")).then(DRAW_AMOUNT);

    static final Parser<Effect.Draw> DRAW = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, DRAW_NO_PLAYER, Effect.Draw::new),
            DRAW_NO_PLAYER.map(amount -> new Effect.Draw(YOU, amount)));

    /// Discard clause following the verb "discard": "N card(s) [at random]",
    /// "a card for each X" (count-scaled — Mind Sludge), or "<possessive>
    /// hand".
    private static final Parser<Discarded> DISCARD_WHAT = anyOf(
            // "N card(s) at random" first so the at-random flag wins.
            sequence(SelectorParsers.AMOUNT.followedBy(anyCiWord("cards", "card")), ciWords("at random"), (amt, ign) ->
                    (Discarded) new Discarded.Cards(amt, true)),
            // "N card(s) for each X" — count replaces N (e.g., Mind Sludge).
            sequence(SelectorParsers.AMOUNT.followedBy(anyCiWord("cards", "card")), FOR_EACH, (_, count) ->
                    (Discarded) new Discarded.Cards(count, false)),
            SelectorParsers.AMOUNT
                    .followedBy(anyCiWord("cards", "card"))
                    .<Discarded>map(amt -> new Discarded.Cards(amt, false)),
            anyCiWord("your", "their", "his", "her", "its").then(w("hand")).thenReturn(Discarded.Hand.HAND));

    private static final Parser<Discarded> DISCARD_NO_PLAYER =
            each(anyCiWord("discards", "discard")).then(DISCARD_WHAT);

    static final Parser<Effect.Discard> DISCARD = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, DISCARD_NO_PLAYER, Effect.Discard::new),
            DISCARD_NO_PLAYER.map(d -> new Effect.Discard(YOU, d)));

    /// "half [possessive] library[, rounded up/down]" — an Amount used by
    /// {@link #MILL_NO_PLAYER} for Traumatize ("mills half their library,
    /// rounded down"). Mirrors {@link #HALF_LIFE} but over the library zone;
    /// default rounding is UP.
    private static final Parser<Amount> HALF_LIBRARY = ciWords("half")
            .then(anyCiWord("your", "their", "its"))
            .followedBy(w("library"))
            .thenReturn((Amount) new Amount.Half(
                    new Amount.PropertyOf(Subject.player(Subject.PlayerRef.THEY), "library"), Amount.Half.Rounding.UP))
            .optionallyFollowedBy(
                    string(",").then(ciWords("rounded")).then(anyCiWord("up", "down")),
                    (base, dir) -> new Amount.Half(
                            ((Amount.Half) base).base(),
                            dir.equalsIgnoreCase("down") ? Amount.Half.Rounding.DOWN : Amount.Half.Rounding.UP));

    private static final Parser<Amount> MILL_NO_PLAYER = anyCiWord("mills", "mill")
            .then(anyOf(
                    // "[N] card(s)" — the common numeric form.
                    SelectorParsers.AMOUNT.followedBy(anyCiWord("cards", "card")),
                    // "half [possessive] library[, rounded up/down]" — Traumatize.
                    HALF_LIBRARY,
                    // "cards equal to [owner] [property]" — property-driven
                    // (e.g., Space-Time Anomaly: "mills cards equal to
                    // your life total").
                    anyCiWord("cards", "card").then(ciWords("equal to")).then(PROPERTY_OF_AMOUNT)));

    static final Parser<Effect.Mill> MILL = anyOf(
            // PLAYER_SUBJECTS also matches possessives like "its
            // controller", which Psychic Strike / Countermand need.
            sequence(SubjectParsers.PLAYER_SUBJECTS, MILL_NO_PLAYER, Effect.Mill::new),
            MILL_NO_PLAYER.map(amount -> new Effect.Mill(YOU, amount)));

    static final Parser<Effect.Scry> SCRY =
            anyCiWord("scry", "surveil").then(SelectorParsers.AMOUNT).map(Effect.Scry::new);

    static final Parser<Effect.Search> SEARCH = sequence(
            w("search").then(anyOf(w("your"), w("their"), w("its"))),
            ciWords("library for").then(SelectorParsers.SELECTOR),
            Effect.Search::new);

    /// Tail of a shuffle clause following the verb: either nothing, a bare
    /// zone (the implicit target, e.g., "their library"), or a two-zone
    /// "[source] into [destination]" phrase (Mnemonic Nexus). Each arm
    /// produces the (source, destination) pair to fold onto the parsed
    /// player; nulls mean "use the default library".
    private static final Parser<Map.Entry<Zone, Zone>> SHUFFLE_TAIL = anyOf(
            sequence(
                    ZoneParsers.ZONE.followedBy(w("into")),
                    ZoneParsers.ZONE,
                    (src, dst) -> Map.<Zone, Zone>entry(src, dst)),
            ZoneParsers.ZONE.map(z -> Map.<Zone, Zone>entry(z, z)));

    /// "[player] shuffles [their library | [source] into [destination]]?.".
    /// Implicit "you" when oracle text omits the subject. Both zones are
    /// nullable in the resulting {@link Effect.Shuffle}.
    static final Parser<Effect.Shuffle> SHUFFLE = anyOf(
            sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("shuffles", "shuffle")),
                    SHUFFLE_TAIL,
                    (player, tail) -> new Effect.Shuffle(player, tail.getKey(), tail.getValue())),
            SubjectParsers.PLAYER_SUBJECT
                    .followedBy(anyCiWord("shuffles", "shuffle"))
                    .map(player -> new Effect.Shuffle(player, null, null)),
            anyCiWord("shuffles", "shuffle")
                    .then(SHUFFLE_TAIL)
                    .map(tail -> new Effect.Shuffle(YOU, tail.getKey(), tail.getValue())),
            anyCiWord("shuffles", "shuffle").thenReturn(new Effect.Shuffle(YOU, null, null)));

    static final Parser<Effect.Reveal> REVEAL =
            w("reveal").then(SubjectParsers.SUBJECT).map(Effect.Reveal::new);

    // Tap/Untap

    static final Parser<Effect.Tap> TAP = w("tap").then(SubjectParsers.SUBJECT).map(Effect.Tap::new);

    static final Parser<Effect.Untap> UNTAP = anyOf(
                    w("untap").then(SubjectParsers.SUBJECT).map(Effect.Untap::new),
                    // "[player] untap[s] [target]." — player-initiated form
                    // (e.g., Early Harvest: "Target player untaps all basic
                    // lands they control."). The player actor is flavor in
                    // the current model; only the untapped target is kept.
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("untaps", "untap")),
                            SubjectParsers.SUBJECT,
                            (_, target) -> new Effect.Untap(target)))
            // Trailing "during [scope]" qualifier consumed as flavor
            // (Thousand Moons Infantry: "during each other player's untap
            // step."). Uses WORD_OR_CONTRACTION since the scope is free
            // text of words / apostrophes / hyphens.
            .optionallyFollowedBy(
                    w("during").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
                    (u, _) -> u);

    /// "[you may] tap or untap [target]." — e.g., Thassa's Ire, Puppeteer.
    /// Tried before TAP so "tap or untap" isn't matched as a plain TAP
    /// followed by stray "or untap" tokens.
    static final Parser<Effect.TapOrUntap> TAP_OR_UNTAP = anyOf(
                    ciWords("you may tap or untap"), ciWords("tap or untap"))
            .then(SubjectParsers.SUBJECT)
            .map(Effect.TapOrUntap::new);

    /// Tail of a "[player]? play with the top card of [poss] library
    /// revealed" phrase — consumes the verb and its body, leaving only
    /// the optional leading subject for the outer parser.
    private static final Parser<String> PLAY_WITH_TOP_REVEALED_TAIL = anyCiWord("plays", "play")
            .followedBy(ciWords("with the top card of"))
            .followedBy(anyCiWord("your", "their", "its"))
            .followedBy(anyCiWord("libraries", "library"))
            .followedBy(w("revealed"));

    /// "[player]? play[s] with the top card of [their] library revealed."
    /// — e.g., Goblin Spy, Future Sight, Field of Dreams (plural
    /// "libraries"). The player defaults to "you" when omitted.
    static final Parser<Effect.PlayWithTopRevealed> PLAY_WITH_TOP_REVEALED = anyOf(
            sequence(
                    SubjectParsers.PLAYER_SUBJECT,
                    PLAY_WITH_TOP_REVEALED_TAIL,
                    (subj, _) -> new Effect.PlayWithTopRevealed(subj)),
            PLAY_WITH_TOP_REVEALED_TAIL.thenReturn(new Effect.PlayWithTopRevealed(YOU)));

    /// "[subject] can block any number of [what]." — e.g., Palace Guard.
    /// Emits a {@link Effect.CanBlock} with an {@link
    /// Effect.CanBlock.Capability.AnyNumberOf} capability so additional
    /// block-capability shapes can slot in beside it.
    static final Parser<Effect.CanBlock> CAN_BLOCK_ANY_NUMBER = sequence(
            SubjectParsers.SUBJECT.followedBy(ciWords("can block any number of")),
            SelectorParsers.SELECTOR,
            (subj, what) -> new Effect.CanBlock(subj, new Effect.CanBlock.Capability.AnyNumberOf(what)));

    /// "[subject] can block an additional [selector] each combat." —
    /// Foriysian Brigade, Night Market Guard, Spike-Tailed Ceratops.
    /// The trailing "each combat" is consumed as flavor since Additional
    /// implies per-combat by rule.
    static final Parser<Effect.CanBlock> CAN_BLOCK_ADDITIONAL = sequence(
                    SubjectParsers.SUBJECT.followedBy(ciWords("can block")),
                    anyOf(anyCiWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT)
                            .followedBy(w("additional")),
                    SelectorParsers.SELECTOR,
                    (subj, count, what) ->
                            new Effect.CanBlock(subj, new Effect.CanBlock.Capability.Additional(count, what)))
            .followedBy(ciWords("each combat"));

    // Counters

    /// "Put [N] [type] counter(s) on [target]." — the standard active-voice
    /// form used for most counter placements.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_PUT = sequence(
            w("put").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyCiWord("counters", "counter"))
                    .followedBy(w("on")),
            SubjectParsers.SUBJECT,
            Effect.AddCounters::new);

    /// "[subject] gets [N] [type] counter(s) [, rounded up/down]?." —
    /// passive-voice form (Prologue to Phyresis; Contaminated Drink). The
    /// optional ", rounded up/down" applies to an enclosing {@link
    /// Amount.Half} (default UP); non-Half amounts simply consume it as
    /// flavor.
    private static final Parser<Effect.AddCounters> ADD_COUNTERS_GETS = sequence(
                    SubjectParsers.SUBJECT.followedBy(anyCiWord("gets", "get")),
                    SelectorParsers.AMOUNT,
                    SelectorParsers.COUNTER_TYPE.followedBy(anyCiWord("counters", "counter")),
                    (target, amount, type) -> new Effect.AddCounters(amount, type, target))
            .optionallyFollowedBy(string(",").then(ciWords("rounded")).then(anyCiWord("up", "down")), (ac, _) -> ac);

    static final Parser<Effect.AddCounters> ADD_COUNTERS = anyOf(ADD_COUNTERS_PUT, ADD_COUNTERS_GETS);

    static final Parser<Effect.RemoveCounters> REMOVE_COUNTERS = sequence(
            w("remove").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyCiWord("counters", "counter"))
                    .followedBy(w("from")),
            SubjectParsers.SUBJECT,
            Effect.RemoveCounters::new);

    // Counterspell

    /// A condition-clause token — like a word but also accepts mana symbols
    /// (`{X}`, `{2}{R}`) and apostrophes so predicates such as "its
    /// controller pays {X}" round-trip verbatim.
    private static final Parser<String> CONDITION_TOKEN =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'{}-]"), "condition token");

    /// Condition tail on a counterspell — either `if [clause]` (Ertai's
    /// Trickery: "if it was kicked") or `unless [clause]` (Clash of Wills:
    /// "unless its controller pays {X}"). The two are semantic negations
    /// of one another and land in {@link Condition.Kind#IF} /
    /// {@link Condition.Kind#UNLESS} respectively.
    private static final Parser<Condition> COUNTER_CONDITION = sequence(
            anyOf(w("if").thenReturn(Condition.Kind.IF), w("unless").thenReturn(Condition.Kind.UNLESS)),
            CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)),
            Condition::new);

    static final Parser<Effect.CounterSpell> COUNTER_SPELL = w("counter")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.CounterSpell::new)
            .optionallyFollowedBy(COUNTER_CONDITION, Effect.CounterSpell::withCondition);

    // Ability modification

    /// Ability enclosed in double quotes — used by {@link #GAIN_ABILITY} for
    /// granted abilities like `Creatures you control have "{T}: Add {G}."`
    /// Refers to {@link OracleParser#ABILITY} so any ability shape (activated,
    /// triggered, spell, keyword) is accepted. Wrapped in a one-element list
    /// to match the keyword-list shape downstream.
    private static final Parser<List<Ability>> QUOTED_ABILITY =
            OracleParser.ABILITY.optionallyFollowedBy(".").between("\"", "\"").map(List::of);

    /// "During your turn," — duration prefix that applies
    /// {@link Duration#duringYourTurn()} to the effect it precedes
    /// (e.g., Sporeback Wolf, Daggersail Aeronaut).
    private static final Parser<Duration> DURING_YOUR_TURN =
            ciWords("during your turn").followedBy(string(",")).thenReturn(Duration.duringYourTurn());

    /// "During turns other than yours," — duration prefix scoped to
    /// turns belonging to another player (e.g., Mesa Lynx).
    private static final Parser<Duration> DURING_OTHERS_TURN =
            ciWords("during turns other than yours").followedBy(string(",")).thenReturn(Duration.duringOthersTurn());

    /// "As long as <predicate>," — duration prefix applied to the following
    /// effect (e.g., Dwarfhold Champion: "As long as this creature is
    /// equipped, it gets +0/+2."). Mirrors the suffix form {@link #AS_LONG_AS}
    /// but fronts the clause before the effect; the predicate runs to the
    /// comma.
    private static final Parser<Duration> AS_LONG_AS_PREFIX = ciWords("as long as")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .followedBy(string(","))
            .map(Duration::forAsLongAs);

    private static final Parser<Effect.GainAbility> GAIN_ABILITY_CORE = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("gains", "gain", "has", "have")),
            anyOf(QUOTED_ABILITY, KeywordParsers.KEYWORD_LIST),
            Effect.GainAbility::new);

    static final Parser<Effect.GainAbility> GAIN_ABILITY = anyOf(
                    sequence(DURING_YOUR_TURN, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    GAIN_ABILITY_CORE)
            .optionallyFollowedBy(DURATION, Effect.GainAbility::withDuration);

    // P/T modification

    /// ", where X is <amount>" — defines the X used by a variable P/T
    /// modifier (e.g., Death's Shadow). Consumes the leading comma so it
    /// can be chained as an {@code optionallyFollowedBy} on MODIFY_PT.
    private static final Parser<Amount> WHERE_X_IS =
            string(",").then(ciWords("where X is")).then(anyOf(PROPERTY_OF_AMOUNT, SelectorParsers.AMOUNT));

    /// MODIFY_PT core — accepts the distributive "each" between a plural
    /// subject and the verb (Sick and Tired / Symbiosis: "Two target
    /// creatures each get …") via the shared {@link #each} helper.
    private static final Parser<Effect.ModifyPT> MODIFY_PT_CORE = sequence(
                    SubjectParsers.SUBJECT.followedBy(each(anyCiWord("gets", "get"))),
                    PT_MODIFIER,
                    Effect.ModifyPT::new)
            .optionallyFollowedBy(FOR_EACH, Effect.ModifyPT::withScaleBy)
            .optionallyFollowedBy(WHERE_X_IS, Effect.ModifyPT::withXDefinition);

    static final Parser<Effect.ModifyPT> MODIFY_PT = anyOf(
                    sequence(DURING_YOUR_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(DURING_OTHERS_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    MODIFY_PT_CORE)
            .optionallyFollowedBy(DURATION, Effect.ModifyPT::withDuration);

    /// "[subject] get[s] [PT] and have/has [keywords]." — shorthand for a
    /// subject getting both a P/T modifier and keyword abilities (e.g.,
    /// Goblin King: "Other Goblins get +1/+1 and have mountainwalk.").
    /// Emits a {@link Effect.Compound} of ModifyPT + GainAbility so each
    /// piece remains a first-class effect. Tried before {@link #MODIFY_PT}.
    static final Parser<Effect.Compound> MODIFY_PT_AND_ABILITY = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("gets", "get")),
            PT_MODIFIER.followedBy(ciWords("and")).followedBy(anyCiWord("have", "has", "gain", "gains")),
            KeywordParsers.KEYWORD_LIST,
            (subj, mod, abils) ->
                    new Effect.Compound(new Effect.ModifyPT(subj, mod), new Effect.GainAbility(subj, abils)));

    // Control

    /// "[player] gain[s] control of [target] [duration]." — active-voice
    /// transfer (e.g., Mind Control).
    private static final Parser<Effect.GainControl> GAIN_CONTROL_ACTIVE = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            anyCiWord("gains", "gain").then(ciWords("control of")).then(SubjectParsers.SUBJECT),
            Effect.GainControl::new);

    /// "Gain control of [target]." — implicit-you variant (e.g., Entrancing
    /// Melody: "Gain control of target creature with mana value X.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_YOU = w("gain")
            .then(ciWords("control of"))
            .then(SubjectParsers.SUBJECT)
            .map(target -> new Effect.GainControl(YOU, target));

    /// "[player] control[s] [target]." — static-ability control grant (e.g.,
    /// Conquer: "You control enchanted land.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_STATIC = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("controls", "control")),
            SubjectParsers.SUBJECT,
            Effect.GainControl::new);

    static final Parser<Effect.GainControl> GAIN_CONTROL = anyOf(
                    GAIN_CONTROL_ACTIVE, GAIN_CONTROL_YOU, GAIN_CONTROL_STATIC)
            .optionallyFollowedBy(DURATION, Effect.GainControl::withDuration);

    /// "Exchange control of [selector]." — e.g., Switcheroo.
    static final Parser<Effect.ExchangeControl> EXCHANGE_CONTROL =
            ciWords("exchange control of").then(SelectorParsers.SELECTOR).map(Effect.ExchangeControl::new);

    // Tokens

    static final Parser<Effect.CreateToken> CREATE_TOKEN =
            sequence(w("create").then(SelectorParsers.AMOUNT), TOKEN_DESCRIPTION, Effect.CreateToken::new);

    // Mana

    /// One fixed-mana option — a contiguous run of mana symbols (e.g. `{G}`,
    /// `{G}{G}`, `{2}{U}`). Wraps the symbols in a {@link ManaOption.Fixed}.
    private static final Parser<ManaOption> FIXED_MANA_OPTION =
            MANA_SYMBOL.atLeastOnce().map(ManaOption.Fixed::new);

    private static final List<ManaSymbol> BASIC_COLORS = List.of(
            new ManaSymbol("{W}"),
            new ManaSymbol("{U}"),
            new ManaSymbol("{B}"),
            new ManaSymbol("{R}"),
            new ManaSymbol("{G}"));

    /// Expands "{@code <amount>} mana of any one color" into one
    /// {@link ManaOption.Repeated} per basic color.
    private static List<Zone.Named> addZone(List<Zone.Named> list, Zone.Named more) {
        var all = new ArrayList<Zone.Named>(list);
        all.add(more);
        return List.copyOf(all);
    }

    private static List<ManaOption> anyOneColor(Amount count) {
        return BASIC_COLORS.stream()
                .<ManaOption>map(c -> new ManaOption.Repeated(count, c))
                .toList();
    }

    private static final Parser<List<ManaOption>> MANA_OPTIONS = anyOf(
            // "one mana of any color" — unambiguous shorthand for one of any basic color.
            ciWords("one mana of any color").thenReturn(anyOneColor(Amount.exact(1))),
            // "<amount> mana of any one color" — amount may be a word number,
            // an integer, or variable X.
            SelectorParsers.AMOUNT.followedBy(ciWords("mana of any one color")).map(EffectParsers::anyOneColor),
            // "<amount> mana in any combination of colors" — each of N mana
            // may be any color independently (Manamorphose). Modelled the
            // same as "mana of any one color" for now.
            SelectorParsers.AMOUNT
                    .followedBy(ciWords("mana in any combination of colors"))
                    .map(EffectParsers::anyOneColor),
            // "<symbol> for each X" — one Repeated option of count(X) copies of symbol.
            sequence(MANA_SYMBOL, FOR_EACH, (sym, count) -> List.<ManaOption>of(new ManaOption.Repeated(count, sym))),
            // "<amount> <symbol>" — amount-scaled repeats of one symbol
            // (e.g., Mana Seism: "add that much {C}").
            sequence(
                    SelectorParsers.AMOUNT,
                    MANA_SYMBOL,
                    (amt, sym) -> List.<ManaOption>of(new ManaOption.Repeated(amt, sym))),
            // "an amount of <symbol> equal to <property>" — Viridian Joiner:
            // "Add an amount of {G} equal to this creature's power.".
            sequence(
                    ciWords("an amount of").then(MANA_SYMBOL),
                    ciWords("equal to").then(PROPERTY_OF_AMOUNT),
                    (sym, amt) -> List.<ManaOption>of(new ManaOption.Repeated(amt, sym))),
            // Fallback: an or-list of fixed groups ({G}, {G}{G}, or {1}{R}, …).
            MtgParsers.orList(FIXED_MANA_OPTION));

    static final Parser<Effect.AddMana> ADD_MANA = w("add").then(MANA_OPTIONS).map(Effect.AddMana::new);

    // Transform/Copy

    static final Parser<Effect.Transform> TRANSFORM =
            w("transform").then(SubjectParsers.SUBJECT).map(Effect.Transform::new);

    static final Parser<Effect.Copy> COPY =
            w("copy").then(SubjectParsers.SUBJECT).map(Effect.Copy::new);

    // Combat

    static final Parser<Effect.Fight> FIGHT = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("fights", "fight")), SubjectParsers.SUBJECT, Effect.Fight::new);

    // Win/Loss

    private static final Parser<String> WIN_GAME_NO_PLAYER =
            anyCiWord("wins", "win").then(ciWords("the game"));

    static final Parser<Effect.WinGame> WIN_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(WIN_GAME_NO_PLAYER).map(Effect.WinGame::new),
            WIN_GAME_NO_PLAYER.thenReturn(new Effect.WinGame(YOU)));

    private static final Parser<String> LOSE_GAME_NO_PLAYER =
            anyCiWord("loses", "lose").then(ciWords("the game"));

    static final Parser<Effect.LoseGame> LOSE_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(LOSE_GAME_NO_PLAYER).map(Effect.LoseGame::new),
            LOSE_GAME_NO_PLAYER.thenReturn(new Effect.LoseGame(YOU)));

    // Zone movement

    /// "Put [subject] [from [zone]]? [destination]." — covers both the
    /// simple "Put [subject] [destination]" form and the sourced form
    /// (e.g., Reclaim: "Put target card from your graveyard on top of
    /// your library.").
    static final Parser<Effect.ZoneMove> ZONE_MOVE = anyOf(
            sequence(
                    w("put").then(SubjectParsers.SUBJECT),
                    IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    ZoneParsers.ZONE_DESTINATION,
                    Effect.ZoneMove::new),
            sequence(
                    w("put").then(SubjectParsers.SUBJECT),
                    ZoneParsers.ZONE_DESTINATION,
                    (subject, dest) -> new Effect.ZoneMove(subject, null, dest)));

    // Prevent

    static final Parser<Effect.Prevent> PREVENT = anyOf(
            // "prevent the next N damage"
            w("prevent")
                    .then(ciWords("the next").then(SelectorParsers.AMOUNT).followedBy(w("damage")))
                    .map(amount -> new Effect.Prevent("prevent the next " + amount + " damage")),
            // "prevent all combat damage that would be dealt this turn" (Fog, Darkness, Holy Day).
            // Must precede the generic "prevent all damage" so "combat" isn't left unconsumed.
            ciWords("prevent all combat damage that would be dealt this turn")
                    .thenReturn(new Effect.Prevent("prevent all combat damage this turn")),
            // "prevent all damage that would be dealt to [subject]" (Bubble Matrix, Cho-Manno)
            sequence(
                    ciWords("prevent all damage").followedBy(ciWords("that would be dealt to")),
                    SubjectParsers.SUBJECT,
                    (_, subject) -> new Effect.Prevent("prevent all damage dealt to " + subject)),
            // "prevent all damage" (no qualifier)
            ciWords("prevent all damage").thenReturn(new Effect.Prevent("prevent all damage")));

    // Combat restrictions

    /// Universal "all creatures" subject used when a block restriction
    /// omits an explicit target ("can't block" == "can't block any
    /// creature").
    private static final Subject ALL_CREATURES = Subject.select(new Selector(
            Selector.Quantifier.all(), Selector.TypeExpression.single(Selector.SingleType.ofCard(CardType.CREATURE))));

    /// "[subject] can't block and can't be blocked." — compound evasion on
    /// the same subject (e.g., Tormented Soul). Tried before {@link
    /// #CANT_BLOCK} so the full phrase is consumed as one effect.
    static final Parser<Effect.Compound> CANT_BLOCK_AND_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't block and can't be blocked"))
            .map(s -> new Effect.Compound(new Effect.CantBlock(s, ALL_CREATURES), new Effect.CantBeBlocked(s)));

    /// "[subject] can't block [what] [duration]." — what defaults to
    /// {@link #ALL_CREATURES}, duration defaults to null. {@code what} is
    /// a full {@link Subject} so "this creature" / "that creature" /
    /// self-references parse alongside selectors.
    static final Parser<Effect.CantBlock> CANT_BLOCK = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't block"))
            .map(s -> new Effect.CantBlock(s, ALL_CREATURES))
            .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.CantBlock::withWhat)
            .optionallyFollowedBy(DURATION, Effect.CantBlock::withDuration);

    static final Parser<Effect.CantAttack> CANT_ATTACK =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't attack")).map(Effect.CantAttack::new);

    /// "[player] take[s] the initiative." — Aarakocra Sneak.
    static final Parser<Effect.TakeInitiative> TAKE_INITIATIVE = SubjectParsers.PLAYER_SUBJECT
            .followedBy(anyCiWord("takes", "take"))
            .followedBy(ciWords("the initiative"))
            .map(Effect.TakeInitiative::new);

    /// "[subject] don't untap [during <scope>]?." — Choke. The optional
    /// scope (e.g., "during their controllers' untap steps") is captured
    /// as free text as a compact placeholder for the structured timing.
    private static final Parser<String> DONT_UNTAP_SCOPE =
            w("during").then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)));

    static final Parser<Effect.DontUntap> DONT_UNTAP = SubjectParsers.SUBJECT
            .followedBy(ciWords("don't untap"))
            .map(Effect.DontUntap::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.DontUntap::withScope);

    /// "[player] can't untap more than [amount] [selector] [during scope]?."
    /// — Mungha Wurm.
    static final Parser<Effect.UntapLimit> UNTAP_LIMIT = sequence(
                    SubjectParsers.PLAYER_SUBJECT
                            .followedBy(ciWords("can't untap"))
                            .followedBy(ciWords("more than")),
                    SelectorParsers.AMOUNT,
                    SelectorParsers.SELECTOR,
                    Effect.UntapLimit::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.UntapLimit::withScope);

    /// "You can cast only [amount] more spell[s] [duration]?." —
    /// Irencrag Feat.
    static final Parser<Effect.CastCountLimit> CAST_COUNT_LIMIT = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("can cast only")),
                    SelectorParsers.AMOUNT.followedBy(w("more")).followedBy(anyCiWord("spells", "spell")),
                    Effect.CastCountLimit::new)
            .optionallyFollowedBy(DURATION, (cl, d) -> new Effect.CastCountLimit(cl.player(), cl.max(), d));

    /// "[player] can't play lands [duration]?." — Turf Wound.
    static final Parser<Effect.CantPlayLands> CANT_PLAY_LANDS = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't play lands"))
            .map(Effect.CantPlayLands::new)
            .optionallyFollowedBy(DURATION, Effect.CantPlayLands::withDuration);

    /// "[chooser] choose[s] how [voter] vote[s] [duration]?." —
    /// Illusion of Choice.
    static final Parser<Effect.ChoosePlayerVote> CHOOSE_PLAYER_VOTE = sequence(
                    SubjectParsers.PLAYER_SUBJECT
                            .followedBy(anyCiWord("chooses", "choose"))
                            .followedBy(w("how")),
                    SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("votes", "vote")),
                    Effect.ChoosePlayerVote::new)
            .optionallyFollowedBy(DURATION, Effect.ChoosePlayerVote::withDuration);

    /// "[subject] have base power and toughness [P/T] [duration]?." —
    /// Godhead of Awe.
    static final Parser<Effect.SetBasePT> SET_BASE_PT = sequence(
                    SubjectParsers.SUBJECT
                            .followedBy(anyCiWord("have", "has"))
                            .followedBy(ciWords("base power and toughness")),
                    SelectorParsers.PT_VALUE,
                    Effect.SetBasePT::new)
            .optionallyFollowedBy(DURATION, Effect.SetBasePT::withDuration);

    /// "[subject] can block only [restriction]." — Gloomwidow.
    static final Parser<Effect.CanBlockOnly> CAN_BLOCK_ONLY = sequence(
            SubjectParsers.SUBJECT.followedBy(ciWords("can block only")),
            SelectorParsers.SELECTOR,
            Effect.CanBlockOnly::new);

    /// "Exchange [possessive] [zone] and [zone]." — Harness Infinity.
    static final Parser<Effect.ExchangeZones> EXCHANGE_ZONES = sequence(
            w("exchange")
                    .then(anyOf(
                            anyCiWord("your", "their", "its")
                                    .map(s -> s.equalsIgnoreCase("your")
                                            ? Subject.player(Subject.PlayerRef.YOU)
                                            : Subject.player(Subject.PlayerRef.THEY)),
                            w("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)))),
            SelectorParsers.ZONE_NAME.followedBy(w("and")),
            SelectorParsers.ZONE_NAME,
            (player, a, b) -> new Effect.ExchangeZones(player, new Zone.Named(a), new Zone.Named(b)));

    /// "Exchange [player]'s life total with [subject]'s [property]." —
    /// Evra, Halcyon Witness.
    static final Parser<Effect.ExchangeLifeWithProperty> EXCHANGE_LIFE_WITH_PROPERTY = sequence(
            ciWords("exchange").then(anyCiWord("your", "their")).followedBy(ciWords("life total")),
            ciWords("with").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
            anyCiWord("power", "toughness", "strength"),
            (poss, source, prop) ->
                    new Effect.ExchangeLifeWithProperty(Subject.player(Subject.PlayerRef.YOU), source, prop));

    /// "Players don't lose unspent mana as steps and phases end." —
    /// Upwelling. Captures the full phrase shape; the unique effect
    /// doesn't parameterize further.
    static final Parser<Effect.ManaPoolPersists> MANA_POOL_PERSISTS = SubjectParsers.PLAYER_SUBJECT
            .followedBy(ciWords("don't lose unspent mana as steps and phases end"))
            .map(Effect.ManaPoolPersists::new);

    /// "Spend only mana produced by [selector] to cast this spell." —
    /// Myr Superion.
    static final Parser<Effect.ManaSpendRestriction> MANA_SPEND_RESTRICTION = ciWords("spend only mana produced by")
            .then(SelectorParsers.SELECTOR)
            .followedBy(ciWords("to cast this spell"))
            .map(Effect.ManaSpendRestriction::new);

    /// "As an additional cost to cast this spell, [cost]." — Mardu
    /// Outrider. The leading "As an additional cost to cast this spell,"
    /// is consumed as flavor; only the cost expression is retained.
    static final Parser<Effect.AdditionalCost> ADDITIONAL_COST = ciWords("as an additional cost to cast this spell")
            .then(string(","))
            .then(CostParsers.COST_EXPRESSION)
            .map(Effect.AdditionalCost::new);

    /// "No more than N creatures can attack [whom] each combat." —
    /// Crawlspace. The trailing "each combat" is consumed as flavor since
    /// the effect is inherently per-combat.
    static final Parser<Effect.AttackLimit> ATTACK_LIMIT = sequence(
                    ciWords("no more than").then(SelectorParsers.AMOUNT),
                    anyCiWord("creatures", "creature")
                            .then(ciWords("can attack"))
                            .then(SubjectParsers.PLAYER_SUBJECT),
                    Effect.AttackLimit::new)
            .followedBy(ciWords("each combat"));

    /// "Until end of turn," — duration prefix used before certain
    /// temporary effects (e.g., Exponential Growth: "Until end of turn,
    /// double target creature's power X times.").
    private static final Parser<Duration> UNTIL_END_OF_TURN_PREFIX =
            ciWords("until end of turn").followedBy(string(",")).thenReturn(Duration.untilEndOfTurn());

    /// Core of a "double …" P/T phrase. Supports both orders that appear
    /// in oracle text: "double the [stat] of [subject]" (Unleash Fury) and
    /// "double [subject]'s [stat]" (Exponential Growth).
    private static final Parser<boolean[]> DOUBLE_STAT_CHOICE = anyOf(
            ciWords("power and toughness").thenReturn(new boolean[] {true, true}),
            w("power").thenReturn(new boolean[] {true, false}),
            w("toughness").thenReturn(new boolean[] {false, true}));

    private static final Parser<Effect.DoublePT> DOUBLE_PT_CORE = anyOf(
            sequence(
                    ciWords("double").then(w("the")).then(DOUBLE_STAT_CHOICE),
                    w("of").then(SubjectParsers.SUBJECT),
                    (flags, subj) -> new Effect.DoublePT(subj, flags[0], flags[1])),
            sequence(
                    ciWords("double").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
                    DOUBLE_STAT_CHOICE,
                    (subj, flags) -> new Effect.DoublePT(subj, flags[0], flags[1])));

    /// "[Until end of turn,]? Double the [power|toughness|…] of [subject]
    /// [N times]? [duration]?." — Unleash Fury, Berserk, Exponential
    /// Growth.
    static final Parser<Effect.DoublePT> DOUBLE_PT = anyOf(
                    sequence(UNTIL_END_OF_TURN_PREFIX, DOUBLE_PT_CORE, (d, m) -> m.withDuration(d)), DOUBLE_PT_CORE)
            .optionallyFollowedBy(
                    SelectorParsers.AMOUNT.followedBy(anyCiWord("times", "time")), Effect.DoublePT::withTimes)
            .optionallyFollowedBy(DURATION, Effect.DoublePT::withDuration);

    /// "Change the target of [subject]." — Deflection. Single-target
    /// redirect; a trailing "with a single target" qualifier is consumed
    /// as flavor.
    static final Parser<Effect.ChangeTheTarget> CHANGE_THE_TARGET = ciWords("change the target of")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.ChangeTheTarget::new)
            .optionallyFollowedBy(ciWords("with a single target"), (c, _) -> c);

    /// "[subject] enter[s] as a copy of [target]." — Essence of the Wild.
    /// Replacement-style entry substitution.
    static final Parser<Effect.EnterAsCopy> ENTER_AS_COPY = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("enters", "enter")).followedBy(ciWords("as a copy of")),
            SubjectParsers.SUBJECT,
            Effect.EnterAsCopy::new);

    // Enter tapped

    static final Parser<Effect.EnterTapped> ENTER_TAPPED = SubjectParsers.SUBJECT
            .followedBy(anyCiWord("enters", "enter"))
            .followedBy(w("tapped"))
            .map(Effect.EnterTapped::new)
            .optionallyFollowedBy(DURATION, Effect.EnterTapped::withDuration);

    /// "[subject] enter[s] with [count] [type] counters on it." — e.g.,
    /// Endless One: "This creature enters with X +1/+1 counters on it."
    static final Parser<Effect.EnterWithCounters> ENTER_WITH_COUNTERS = sequence(
            SubjectParsers.SUBJECT.followedBy(anyCiWord("enters", "enter")).followedBy(w("with")),
            SelectorParsers.AMOUNT,
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyCiWord("counters", "counter"))
                    .followedBy(w("on"))
                    .followedBy(anyCiWord("it", "them")),
            Effect.EnterWithCounters::new);

    // Characteristic-setting: "[subject] are/is [colors|colorless|subtype]"

    /// Head of a characteristic-setting clause: the subject followed by a
    /// copula verb — "are" / "is" for static characteristics or "becomes" /
    /// "become" for target-acquires forms (e.g., Moonlace: "Target spell or
    /// permanent becomes colorless."). Shared by SET_COLORS / SET_COLORLESS /
    /// SET_SUBTYPE.
    private static final Parser<Subject> ARE_SUBJECT =
            SubjectParsers.SUBJECT.followedBy(anyCiWord("are", "is", "becomes", "become"));

    private static final List<Color> ALL_COLORS = List.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

    static final Parser<Effect.SetColors> SET_COLORS = sequence(
                    ARE_SUBJECT, MtgParsers.andList(SelectorParsers.COLOR), Effect.SetColors::new)
            .optionallyFollowedBy(DURATION, Effect.SetColors::withDuration);

    /// "X are/is colorless [duration]?" — SetColors with an empty list
    /// (Ancient Kavu: "becomes colorless until end of turn").
    static final Parser<Effect.SetColors> SET_COLORLESS = ARE_SUBJECT
            .followedBy(w("colorless"))
            .map(s -> new Effect.SetColors(s, List.of()))
            .optionallyFollowedBy(DURATION, Effect.SetColors::withDuration);

    /// "X are/is all colors [duration]?" — SetColors with the five basic
    /// colors.
    static final Parser<Effect.SetColors> SET_ALL_COLORS = ARE_SUBJECT
            .followedBy(ciWords("all colors"))
            .map(s -> new Effect.SetColors(s, ALL_COLORS))
            .optionallyFollowedBy(DURATION, Effect.SetColors::withDuration);

    static final Parser<Effect.SetSubtype> SET_SUBTYPE = sequence(
                    ARE_SUBJECT,
                    anyOf(anyCiWord("a", "an").then(SelectorParsers.SUBTYPE_NAME), SelectorParsers.SUBTYPE_NAME),
                    Effect.SetSubtype::new)
            // "in addition to its other land/creature/… types" — flavor
            // indicating the new subtype is additive, not replacing
            // (e.g., Blanket of Night: "Each land is a Swamp in addition to
            // its other land types.").
            .optionallyFollowedBy(
                    ciWords("in addition to")
                            .then(anyCiWord("its", "their"))
                            .then(w("other"))
                            .then(SelectorParsers.CARD_TYPE)
                            .then(w("types")),
                    (s, _) -> s)
            .optionallyFollowedBy(DURATION, Effect.SetSubtype::withDuration);

    /// "[subject] are [P/T] [type] [that are still [type]]." — become a
    /// permanent type with a stated P/T (e.g., Living Plane: "All lands are
    /// 1/1 creatures that are still lands."). Emits a
    /// {@link Effect.SetCharacteristic} with the characteristic as free text
    /// because the full shape (P/T + added type + retained types) is beyond
    /// what the current structured types model.
    /// Core of a become-permanent characteristic: [P/T] [color]? [card
    /// type]. The optional color allows forms like Kormus Bell: "1/1 black
    /// creatures". Returned as free text for the SetCharacteristic
    /// description.
    private static final Parser<String> BECOME_PT_TYPE_CORE = sequence(
            SelectorParsers.PT_VALUE,
            anyOf(
                    sequence(
                            SelectorParsers.COLOR,
                            SelectorParsers.CARD_TYPE,
                            (c, t) -> c.name().toLowerCase() + " " + t.name().toLowerCase()),
                    SelectorParsers.CARD_TYPE.map(t -> t.name().toLowerCase())),
            (pt, rest) -> pt + " " + rest);

    static final Parser<Effect.SetCharacteristic> BECOME_PT_TYPE = sequence(
                    ARE_SUBJECT, BECOME_PT_TYPE_CORE, Effect.SetCharacteristic::new)
            .optionallyFollowedBy(
                    ciWords("that are still").then(SelectorParsers.CARD_TYPE),
                    (sc, still) -> new Effect.SetCharacteristic(
                            sc.target(),
                            sc.description() + " (still " + still.name().toLowerCase() + ")"));

    /// "[subject] are [supertype]" — add a supertype (Rootpath Purifier).
    static final Parser<Effect.SetSupertype> SET_SUPERTYPE =
            sequence(ARE_SUBJECT, SelectorParsers.SUPERTYPE, Effect.SetSupertype::new);

    /// "Turn [subject] face up." — Break Open.
    static final Parser<Effect.TurnFaceUp> TURN_FACE_UP = w("turn")
            .then(SubjectParsers.SUBJECT)
            .followedBy(ciWords("face up"))
            .map(Effect.TurnFaceUp::new);

    /// "X are/is no longer [supertype]" — remove a supertype.
    static final Parser<Effect.LoseSupertype> LOSE_SUPERTYPE =
            sequence(ARE_SUBJECT, ciWords("no longer").then(SelectorParsers.SUPERTYPE), Effect.LoseSupertype::new);

    // Regeneration (701.15)

    static final Parser<Effect.Regenerate> REGENERATE =
            w("regenerate").then(SubjectParsers.SUBJECT).map(Effect.Regenerate::new);

    // Action restrictions

    static final Parser<Effect.CantCycle> CANT_CYCLE =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't cycle cards")).map(Effect.CantCycle::new);

    /// "Activated abilities of [selector] can't be activated." — e.g.,
    /// Collector Ouphe, Cursed Totem.
    static final Parser<Effect.CantActivate> CANT_ACTIVATE = ciWords("activated abilities of")
            .then(SelectorParsers.SELECTOR)
            .followedBy(ciWords("can't be activated"))
            .map(Effect.CantActivate::new);

    /// "[subject] can't be blocked [by|except by X] [duration]." Structured
    /// as a single CantBeBlocked effect with an optional {@link Effect.CantBeBlocked.By}
    /// variant: {@link Effect.CantBeBlocked.By.Matching} for "by X"
    /// (e.g., "can't be blocked by Walls") and
    /// {@link Effect.CantBeBlocked.By.Except} for "except by X"
    /// (e.g., Shifting Sliver).
    private static final Parser<Effect.CantBeBlocked.By> CANT_BE_BLOCKED_BY = anyOf(
            ciWords("except by")
                    .then(SelectorParsers.SELECTOR)
                    .<Effect.CantBeBlocked.By>map(Effect.CantBeBlocked.By.Except::new),
            // "by more than N X" — an upper bound on the number of blockers
            // (Huang Zhong: "can't be blocked by more than one creature.").
            sequence(ciWords("by more than").then(SelectorParsers.AMOUNT), SelectorParsers.SELECTOR, (max, sel) ->
                    (Effect.CantBeBlocked.By) new Effect.CantBeBlocked.By.LimitOf(max, sel)),
            w("by").then(SelectorParsers.SELECTOR).<Effect.CantBeBlocked.By>map(Effect.CantBeBlocked.By.Matching::new));

    static final Parser<Effect.CantBeBlocked> CANT_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't be blocked"))
            .map(Effect.CantBeBlocked::new)
            .optionallyFollowedBy(CANT_BE_BLOCKED_BY, Effect.CantBeBlocked::withBy)
            .optionallyFollowedBy(DURATION, Effect.CantBeBlocked::withDuration);

    static final Parser<Effect.CantBlockAlone> CANT_BLOCK_ALONE =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't block alone")).map(Effect.CantBlockAlone::new);

    static final Parser<Effect.CantAttackAlone> CANT_ATTACK_ALONE =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't attack alone")).map(Effect.CantAttackAlone::new);

    /// "[subject] can't attack [whom]" — e.g., "Creatures can't attack you."
    static final Parser<Effect.CantAttackWhom> CANT_ATTACK_WHOM = sequence(
            SubjectParsers.SUBJECT.followedBy(ciWords("can't attack")),
            SubjectParsers.PLAYER_SUBJECT,
            Effect.CantAttackWhom::new);

    /// "[player] takes/take [count] extra turn(s) [after this one]." —
    /// `count` accepts the indefinite article ("an extra turn") as 1 or
    /// an explicit number (Time Stretch: "two extra turns"). Player is
    /// either named (Time Warp) or implicitly "you".
    static final Parser<Effect.TakeExtraTurn> TAKE_EXTRA_TURN = sequence(
                    anyOf(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("takes", "take")),
                            anyCiWord("takes", "take").thenReturn(YOU)),
                    anyOf(anyCiWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT),
                    ciWords("extra").followedBy(anyCiWord("turns", "turn")),
                    (player, count, _) -> new Effect.TakeExtraTurn(player, count))
            .optionallyFollowedBy(ciWords("after this one"), (eff, _) -> eff);

    /// "[player] may play lands from [zone]." — e.g., Crucible of Worlds:
    /// "You may play lands from your graveyard."
    static final Parser<Effect.PlayLandsFrom> PLAY_LANDS_FROM = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may play lands")),
            IN_ZONE_FROM,
            Effect.PlayLandsFrom::new);

    /// `[up to] N additional land(s)` — amount for a "play additional lands"
    /// effect. "Up to" bounds the max; a bare amount is an exact count.
    private static final Parser<Amount> ADDITIONAL_LANDS_AMOUNT = anyOf(
                    ciWords("up to").then(SelectorParsers.AMOUNT), SelectorParsers.AMOUNT)
            .followedBy(w("additional"))
            .followedBy(anyCiWord("lands", "land"));

    /// Body of a "play additional lands" clause, starting at the verb. Used
    /// both directly in {@link #PLAY_ADDITIONAL_LANDS} and by {@link #MAY}
    /// for the "may" form (Summer Bloom).
    private static final Parser<Amount> PLAY_ADDITIONAL_LANDS_NO_PLAYER =
            anyCiWord("plays", "play").then(ADDITIONAL_LANDS_AMOUNT);

    static final Parser<Effect.PlayAdditionalLands> PLAY_ADDITIONAL_LANDS = anyOf(
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT,
                            PLAY_ADDITIONAL_LANDS_NO_PLAYER,
                            Effect.PlayAdditionalLands::new),
                    PLAY_ADDITIONAL_LANDS_NO_PLAYER.map(amount -> new Effect.PlayAdditionalLands(YOU, amount)))
            .optionallyFollowedBy(DURATION, Effect.PlayAdditionalLands::withDuration);

    /// "The Ring tempts [you]." — rule 716.
    static final Parser<Effect.RingTempts> RING_TEMPTS =
            ciWords("the Ring tempts").then(SubjectParsers.PLAYER_SUBJECT).map(Effect.RingTempts::new);

    /// "[player]'s hand" — a possessive reference used in "Look at target
    /// player's hand" etc. Packaged as a {@link Subject.PossessiveSubject}
    /// so LOOK_AT's target is always a {@link Subject}. Also covers the
    /// possessive form "its controller's hand" / "its owner's hand" (Lay
    /// Bare: "Counter target spell. Look at its controller's hand.").
    private static final Parser<Subject> PLAYER_HAND_SUBJECT = anyOf(
            SubjectParsers.PLAYER_REF
                    .followedBy(string("'s"))
                    .followedBy(w("hand"))
                    .map(ref -> Subject.possessiveSubject(ref.name().toLowerCase() + "'s", "hand")),
            sequence(
                    anyCiWord("its", "their", "your"),
                    anyCiWord("controller", "owner").followedBy(string("'s")).followedBy(w("hand")),
                    (pronoun, role) -> Subject.possessiveSubject(pronoun + " " + role + "'s", "hand")));

    /// "the top card of [player]'s library" — a positional card reference
    /// used as a LOOK_AT target (e.g., Rootwater Mystic). Rendered as a
    /// {@link Subject.PossessiveSubject} with role "top card of library".
    private static final Parser<Subject> TOP_CARD_OF_LIBRARY = ciWords("the top card of")
            .then(SubjectParsers.PLAYER_REF)
            .followedBy(string("'s"))
            .followedBy(w("library"))
            .map(ref -> Subject.possessiveSubject(ref.name().toLowerCase() + "'s", "top card of library"));

    /// "Look at [target]." — reveal-to-looker. Covers player-hand targets
    /// ("target player's hand"), top-of-library ("the top card of target
    /// player's library"), and game-object targets ("target face-down
    /// creature", Smoke Teller).
    static final Parser<Effect.LookAt> LOOK_AT = ciWords("look at")
            .then(anyOf(TOP_CARD_OF_LIBRARY, PLAYER_HAND_SUBJECT, SubjectParsers.SUBJECT))
            .map(Effect.LookAt::new);

    /// "[player] may cast [what] from [zone]+." — permission to cast from
    /// one or more zones (Misthollow Griffin; Squee, the Immortal: "…
    /// from your graveyard or from exile.").
    static final Parser<Effect.CastFromZone> CAST_FROM_ZONE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may cast")),
            SubjectParsers.SUBJECT,
            IN_ZONE_FROM
                    .<List<Zone.Named>>map(List::of)
                    .optionallyFollowedBy(w("or").then(IN_ZONE_FROM), EffectParsers::addZone),
            Effect.CastFromZone::new);

    /// "[player] may choose new targets for [spell]." — e.g., Redirect.
    static final Parser<Effect.ChooseNewTargets> CHOOSE_NEW_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may choose new targets for")),
            SubjectParsers.SUBJECT,
            Effect.ChooseNewTargets::new);

    /// "Choose [selector] [at random]?." — e.g., Duneblast, Last One Standing.
    static final Parser<Effect.Choose> CHOOSE = w("choose")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.Choose::new)
            .optionallyFollowedBy(ciWords("at random"), (c, _) -> new Effect.Choose(c.what(), true));

    /// "[player] become[s] the monarch." — rule 716 (e.g., Palace Sentinels).
    static final Parser<Effect.BecomeMonarch> BECOME_MONARCH = SubjectParsers.PLAYER_SUBJECT
            .followedBy(anyCiWord("becomes", "become"))
            .followedBy(ciWords("the monarch"))
            .map(Effect.BecomeMonarch::new);

    /// "[players] exchange life totals." — e.g., Soul Conduit.
    static final Parser<Effect.ExchangeLifeTotals> EXCHANGE_LIFE_TOTALS =
            SubjectParsers.SUBJECT.followedBy(ciWords("exchange life totals")).map(Effect.ExchangeLifeTotals::new);

    /// "[player] may change any targets of [spell]." — e.g., Sideswipe.
    static final Parser<Effect.ChangeAnyTargets> CHANGE_ANY_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may change any targets of")),
            SubjectParsers.SUBJECT,
            Effect.ChangeAnyTargets::new);

    /// Marker-counter token like `{E}` (energy) or `{TK}` (tickets). Kept as
    /// a raw `{...}` string so new markers slot in without a grammar change.
    private static final Parser<String> MARKER_TOKEN = string("{")
            .then(consecutive(CharPredicate.noneOf(" {}"), "marker content"))
            .followedBy(string("}"))
            .map(s -> "{" + s + "}");

    /// Amount paired with a marker-counter token. Accepts three oracle
    /// shapes: explicit count (`"2 {TK}"`), multi-symbol count
    /// (`"{E}{E}"` → 2× `{E}`, common on energy/ticket payouts such as
    /// Tune the Narrative), and bare single (`"{TK}"` → 1×).
    private static final Parser<Map.Entry<Amount, String>> AMOUNT_MARKER = anyOf(
            sequence(SelectorParsers.AMOUNT, MARKER_TOKEN, Map::entry),
            MARKER_TOKEN.atLeastOnce().map(tokens -> {
                var head = tokens.getFirst();
                return Map.entry(Amount.exact(tokens.size()), head);
            }));

    /// "[player] get[s] [N] [marker]." — e.g., Blorbian Buddy: "You get
    /// {TK}." When the amount is omitted ("You get {E}."), it defaults to 1.
    static final Parser<Effect.GetMarker> GET_MARKER = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(anyCiWord("gets", "get")),
            AMOUNT_MARKER,
            (player, pair) -> new Effect.GetMarker(player, pair.getKey(), pair.getValue()));

    /// "[subject] can't be countered." — spell counter-immunity.
    static final Parser<Effect.CantBeCountered> CANT_BE_COUNTERED =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't be countered")).map(Effect.CantBeCountered::new);

    /// "[subject] must be blocked [duration]? [if able]?." — combat
    /// must-block restriction.
    static final Parser<Effect.MustBeBlocked> MUST_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(ciWords("must be blocked"))
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .optionallyFollowedBy(ciWords("if able"), (mb, _) -> mb);

    /// "[blockers] able to block [target] do so." — e.g., Taunting Elf,
    /// Elvish Bard. The blockers subject is discarded as flavor (it's
    /// always "all creatures"-like); semantically this forces {@code target}
    /// to be blocked by any creature that can.
    static final Parser<Effect.MustBeBlocked> ABLE_TO_BLOCK_DO_SO = SubjectParsers.SUBJECT
            .followedBy(ciWords("able to block"))
            .then(SubjectParsers.SUBJECT)
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .followedBy(ciWords("do so"));

    /// "[subject] can't attack or block [duration]." — combined combat
    /// restriction with optional duration suffix.
    static final Parser<Effect.CantAttackOrBlock> CANT_ATTACK_OR_BLOCK = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't attack or block"))
            .map(Effect.CantAttackOrBlock::new)
            .optionallyFollowedBy(DURATION, Effect.CantAttackOrBlock::withDuration);

    /// "[subject] can't have counters put on it." — e.g., Melira's Keepers.
    static final Parser<Effect.CantHaveCounters> CANT_HAVE_COUNTERS = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't have counters put on"))
            .followedBy(anyCiWord("it", "them"))
            .map(Effect.CantHaveCounters::new);

    /// "[subject] can't be regenerated [duration]." — e.g., Tunnel
    /// (static) and Furnace Brood ("this turn").
    static final Parser<Effect.CantBeRegenerated> CANT_BE_REGENERATED = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't be regenerated"))
            .map(Effect.CantBeRegenerated::new)
            .optionallyFollowedBy(DURATION, Effect.CantBeRegenerated::withDuration);

    /// "[subject] [entering|dying|entering or dying] don't cause abilities
    /// [of [selector]]? to trigger." — suppresses ETB/death triggers on the
    /// named permanents. Covers Tocatli Honor Guard / Torpor Orb (entering,
    /// all abilities), Hushbringer ("entering or dying"), and Elesh Norn,
    /// Mother of Machines (entering, scoped to abilities of opponents'
    /// permanents). "entering or dying" is tried before the single-word
    /// forms so the longer match wins.
    private static final Parser<Effect.SuppressEtbTriggers.Event> ETB_SUPPRESS_EVENT = anyOf(
            ciWords("entering or dying").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING_OR_DYING),
            w("entering").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING),
            w("dying").thenReturn(Effect.SuppressEtbTriggers.Event.DYING));

    static final Parser<Effect.SuppressEtbTriggers> SUPPRESS_ETB_TRIGGERS = sequence(
                    SubjectParsers.SUBJECT,
                    ETB_SUPPRESS_EVENT.followedBy(ciWords("don't cause abilities")),
                    Effect.SuppressEtbTriggers::new)
            .optionallyFollowedBy(w("of").then(SelectorParsers.SELECTOR), Effect.SuppressEtbTriggers::withScope)
            .followedBy(ciWords("to trigger"));

    /// "If <trigger-clause>, that ability triggers [N] additional time[s]."
    /// — Elesh Norn, Mother of Machines. The trigger clause is captured as
    /// free word tokens up to the comma; {@code additional} defaults to 1
    /// for "an additional time". Because it shares the "If …," prefix with
    /// {@link #IF_PREFIX_CONDITION} and must win when the tail is the
    /// Panharmonicon-style ", that ability triggers …", it is dispatched in
    /// {@link #EFFECT} ahead of the generic if-prefix conditional.
    static final Parser<Effect.AdditionalEtbTriggers> ADDITIONAL_ETB_TRIGGERS = sequence(
            w("if").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
            string(",")
                    .then(ciWords("that ability triggers"))
                    .then(anyOf(anyCiWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT))
                    .followedBy(w("additional"))
                    .followedBy(anyCiWord("times", "time")),
            Effect.AdditionalEtbTriggers::new);

    /// "[subject] can't be equipped." — e.g., Goblin Brawler.
    static final Parser<Effect.CantBeEquipped> CANT_BE_EQUIPPED =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't be equipped")).map(Effect.CantBeEquipped::new);

    /// "Unattach [selector] from [target]." — e.g., Disarm: "Unattach all
    /// Equipment from target creature."
    static final Parser<Effect.Unattach> UNATTACH = sequence(
            w("unattach").then(SelectorParsers.SELECTOR), w("from").then(SubjectParsers.SUBJECT), Effect.Unattach::new);

    /// "[player] may cast [what] as though [clause]." — e.g., Vedalken
    /// Orrery. The as-though clause is captured as free text for now.
    static final Parser<Effect.CastAsThough> CAST_AS_THOUGH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may cast")),
            SelectorParsers.SELECTOR.followedBy(ciWords("as though")),
            word().atLeastOnce().map(words -> String.join(" ", words)),
            Effect.CastAsThough::new);

    /// "[player] may cast [what] without paying [its|their] mana cost[s]."
    /// — Dracogenesis.
    static final Parser<Effect.CastWithoutPaying> CAST_WITHOUT_PAYING = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may cast")),
                    SelectorParsers.SELECTOR,
                    Effect.CastWithoutPaying::new)
            .followedBy(ciWords("without paying"))
            .followedBy(anyCiWord("its", "their"))
            .followedBy(w("mana"))
            .followedBy(anyCiWord("costs", "cost"));

    /// "[player] may spend [X] mana as though it were [Y] mana." — color
    /// substitution on mana spend (Sunglasses of Urza). Only color-to-color
    /// substitution is captured here; broader forms ("mana of any color")
    /// can grow new arms as they appear.
    static final Parser<Effect.SpendManaAsThough> SPEND_MANA_AS_THOUGH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("may spend")),
            SelectorParsers.COLOR.followedBy(w("mana")).followedBy(ciWords("as though it were")),
            SelectorParsers.COLOR.followedBy(w("mana")),
            Effect.SpendManaAsThough::new);

    /// "[subject] can't attack or block alone." — e.g., Ember Beast.
    static final Parser<Effect.CantAttackOrBlockAlone> CANT_ATTACK_OR_BLOCK_ALONE = SubjectParsers.SUBJECT
            .followedBy(ciWords("can't attack or block alone"))
            .map(Effect.CantAttackOrBlockAlone::new);

    /// "[subject] attacks [each combat/turn | this turn] if able." — static
    /// or temporary must-attack restriction.
    static final Parser<Effect.MustAttack> MUST_ATTACK = anyOf(
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(anyCiWord("attacks", "attack")),
                            DURATION,
                            (s, d) -> new Effect.MustAttack(s).withDuration(d)),
                    SubjectParsers.SUBJECT
                            .followedBy(anyCiWord("attacks", "attack"))
                            .followedBy(w("each"))
                            .followedBy(anyCiWord("combat", "turn"))
                            .map(Effect.MustAttack::new))
            .optionallyFollowedBy(ciWords("if able"), (s, _) -> s);

    /// "[player]'s life total becomes N."
    static final Parser<Effect.LifeTotalBecomes> LIFE_TOTAL_BECOMES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")).followedBy(ciWords("life total becomes")),
            SelectorParsers.AMOUNT,
            Effect.LifeTotalBecomes::new);

    /// Named game rule inside quotes — for now only the "legend rule".
    private static final Parser<GameRule> RULE_NAME =
            anyOf(ciWords("legend rule").thenReturn(GameRule.LEGEND_RULE));

    /// `The "<rule>" doesn't apply.` — e.g., Mirror Gallery.
    static final Parser<Effect.RuleDoesntApply> RULE_DOESNT_APPLY = ciWords("the")
            .then(string("\""))
            .then(RULE_NAME)
            .followedBy(string("\""))
            .followedBy(ciWords("doesn't apply"))
            .map(Effect.RuleDoesntApply::new);

    // Skip (rule 614.10)

    /// Bare step-name parser — no trailing "step(s)" suffix. Used by the
    /// demonstrative skip form ("skip that draw").
    private static final Parser<Step> STEP_NAME = anyOf(
            ciWords("beginning of combat").thenReturn(Step.BEGINNING_OF_COMBAT),
            ciWords("declare attackers").thenReturn(Step.DECLARE_ATTACKERS),
            ciWords("declare blockers").thenReturn(Step.DECLARE_BLOCKERS),
            ciWords("combat damage").thenReturn(Step.COMBAT_DAMAGE),
            ciWords("end of combat").thenReturn(Step.END_OF_COMBAT),
            w("untap").thenReturn(Step.UNTAP),
            w("upkeep").thenReturn(Step.UPKEEP),
            w("draw").thenReturn(Step.DRAW),
            w("end").thenReturn(Step.END),
            w("cleanup").thenReturn(Step.CLEANUP));

    /// Matches a Step name followed by "step"/"steps". Multi-word step
    /// names come first via {@link #STEP_NAME}'s ordering so their first
    /// word isn't consumed by a shorter alternative.
    private static final Parser<Step> SKIPPABLE_STEP = STEP_NAME.followedBy(anyCiWord("steps", "step"));

    private static final Parser<Phase> SKIPPABLE_PHASE = anyOf(
                    w("beginning").thenReturn(Phase.BEGINNING),
                    w("main").thenReturn(Phase.MAIN),
                    w("combat").thenReturn(Phase.COMBAT),
                    w("ending").thenReturn(Phase.ENDING))
            .followedBy(anyCiWord("phases", "phase"));

    private static final Parser<Skippable> SKIPPABLE = anyOf(
            SKIPPABLE_STEP.<Skippable>map(Skippable.OfStep::new),
            SKIPPABLE_PHASE.<Skippable>map(Skippable.OfPhase::new),
            anyCiWord("turns", "turn").thenReturn(Skippable.Turn.TURN));

    /// Tail of the "all X of [possessive] [next]? turn" skip form — the
    /// possessive-prefixed turn reference used by {@link #SKIP_NO_PLAYER}'s
    /// distributive arm. Consumed as flavor.
    private static final Parser<String> OF_NEXT_TURN_TAIL = ciWords("of")
            .then(anyCiWord("your", "their", "his", "her", "its"))
            .then(anyOf(w("next").followedBy(anyCiWord("turns", "turn")), anyCiWord("turns", "turn")));

    /// "[player] skip[s] [target] [what]." — rule 614.10. {@code target} is
    /// either a possessive pronoun ("your/their/…") optionally preceded by
    /// "next", or the all-of-type form "all X of [possessive] [next]? turn"
    /// (e.g., False Peace: "skips all combat phases of their next turn.").
    private static final Parser<Skippable> SKIP_NO_PLAYER = anyCiWord("skips", "skip")
            .then(anyOf(
                    // "all [X] of [possessive] [next]? turn(s)" — emits the
                    // same Skippable as the short form; the "of … turn" tail
                    // is consumed as flavor since per-turn scope is implicit.
                    sequence(ciWords("all").then(SKIPPABLE), OF_NEXT_TURN_TAIL, (s, _) -> s),
                    // "that [step-name]" — demonstrative form referring back
                    // to an event in the triggering clause (Obstinate
                    // Familiar: "If you would draw a card, you may skip
                    // that draw instead."). Matches a bare step name
                    // without requiring the "step" suffix.
                    w("that").then(STEP_NAME).<Skippable>map(Skippable.OfStep::new),
                    anyCiWord("your", "their", "his", "her", "its")
                            .then(anyOf(w("next").then(SKIPPABLE), SKIPPABLE))));

    static final Parser<Effect.Skip> SKIP = anyOf(
                    sequence(SubjectParsers.PLAYER_SUBJECT, SKIP_NO_PLAYER, Effect.Skip::new),
                    SKIP_NO_PLAYER.map(s -> new Effect.Skip(YOU, s)))
            .optionallyFollowedBy(DURATION, Effect.Skip::withDuration);

    static final Parser<Effect.CantSearchLibraries> CANT_SEARCH_LIBRARIES =
            SubjectParsers.SUBJECT.followedBy(ciWords("can't search libraries")).map(Effect.CantSearchLibraries::new);

    /// "[players] can cast spells only during [timing]." — e.g., Dosan
    /// the Falling Leaf. The timing is captured as free text.
    static final Parser<Effect.RestrictSpellTiming> RESTRICT_SPELL_TIMING = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(ciWords("can cast spells only during")),
            WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)),
            Effect.RestrictSpellTiming::new);

    static final Parser<Effect.CantCast> CANT_CAST = sequence(
                    SubjectParsers.SUBJECT.followedBy(ciWords("can't cast")),
                    SelectorParsers.SELECTOR,
                    Effect.CantCast::new)
            .optionallyFollowedBy(w("spells"), (cc, ign) -> cc)
            .optionallyFollowedBy(DURATION, Effect.CantCast::withDuration);

    /// "[subject] can't [draw|cast] more than [N] [cards|spells] each turn."
    /// — a per-turn upper limit (Spirit of the Labyrinth, Arcane Laboratory,
    /// Eidolon of Rhetoric).
    static final Parser<Effect.PerTurnLimit> PER_TURN_LIMIT = sequence(
            SubjectParsers.SUBJECT.followedBy(ciWords("can't")),
            anyOf(
                    w("draw").followedBy(ciWords("more than")).thenReturn(Effect.PerTurnLimit.Action.DRAW_CARDS),
                    w("cast").followedBy(ciWords("more than")).thenReturn(Effect.PerTurnLimit.Action.CAST_SPELLS)),
            SelectorParsers.AMOUNT
                    .followedBy(anyCiWord("cards", "card", "spells", "spell"))
                    .followedBy(ciWords("each turn")),
            Effect.PerTurnLimit::new);

    /// "[player] have no maximum hand size" — MaximumHandSize.None variant.
    /// Maximum hand size is a player-only concept, so the subject is a player.
    static final Parser<Effect.MaximumHandSize> NO_MAXIMUM_HAND_SIZE = SubjectParsers.PLAYER_SUBJECT
            .followedBy(anyCiWord("have", "has"))
            .followedBy(ciWords("no maximum hand size"))
            .map(s -> new Effect.MaximumHandSize(s, Effect.MaximumHandSize.HandSize.None.NONE));

    /// Possessive player prefix used in phrases like "Your maximum hand size"
    /// or "Each opponent's maximum hand size". Accepts the bare pronouns
    /// "your" / "their" as well as an explicit {@link Subject.PlayerRef}
    /// followed by `'s` (e.g., "each opponent's", "each player's", "target
    /// player's").
    private static final Parser<Subject> POSSESSIVE_PLAYER = anyOf(
            ciWords("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            ciWords("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")));

    /// "[possessive] maximum hand size is reduced/increased by N." — Delta
    /// variant (e.g., Thought Nibbler: "Your maximum hand size is reduced by
    /// two.").
    static final Parser<Effect.MaximumHandSize> MAXIMUM_HAND_SIZE_DELTA = sequence(
            POSSESSIVE_PLAYER.followedBy(ciWords("maximum hand size is")),
            anyOf(
                    w("reduced")
                            .followedBy(w("by"))
                            .then(SelectorParsers.NUMBER)
                            .map(n -> -n),
                    w("increased").followedBy(w("by")).then(SelectorParsers.NUMBER)),
            (p, delta) -> new Effect.MaximumHandSize(p, new Effect.MaximumHandSize.HandSize.Delta(delta)));

    // Cost modification: "<subject> cost[s] <mana> more/less [to cast]"

    private static final Parser<CostDelta> COST_DELTA =
            anyOf(w("more").thenReturn(CostDelta.MORE), w("less").thenReturn(CostDelta.LESS));

    /// Known cost-keyword names ("buyback", "kicker", "cycling", …).
    /// Underscored enum constants (LEVEL_UP) map to their oracle-text form.
    private static final Parser<CostKeyword> COST_KEYWORD = anyOf(
            ciWords("level up").thenReturn(CostKeyword.LEVEL_UP),
            ciWords("aura swap").thenReturn(CostKeyword.AURA_SWAP),
            w("buyback").thenReturn(CostKeyword.BUYBACK),
            w("kicker").thenReturn(CostKeyword.KICKER),
            w("multikicker").thenReturn(CostKeyword.MULTIKICKER),
            w("flashback").thenReturn(CostKeyword.FLASHBACK),
            w("madness").thenReturn(CostKeyword.MADNESS),
            w("echo").thenReturn(CostKeyword.ECHO),
            w("cycling").thenReturn(CostKeyword.CYCLING),
            w("equip").thenReturn(CostKeyword.EQUIP),
            w("fortify").thenReturn(CostKeyword.FORTIFY),
            w("ward").thenReturn(CostKeyword.WARD),
            w("bestow").thenReturn(CostKeyword.BESTOW),
            w("dash").thenReturn(CostKeyword.DASH),
            w("entwine").thenReturn(CostKeyword.ENTWINE),
            w("splice").thenReturn(CostKeyword.SPLICE),
            w("replicate").thenReturn(CostKeyword.REPLICATE),
            w("suspend").thenReturn(CostKeyword.SUSPEND),
            w("transmute").thenReturn(CostKeyword.TRANSMUTE),
            w("transfigure").thenReturn(CostKeyword.TRANSFIGURE),
            w("recover").thenReturn(CostKeyword.RECOVER),
            w("ninjutsu").thenReturn(CostKeyword.NINJUTSU),
            w("outlast").thenReturn(CostKeyword.OUTLAST),
            w("scavenge").thenReturn(CostKeyword.SCAVENGE),
            w("unearth").thenReturn(CostKeyword.UNEARTH),
            w("reinforce").thenReturn(CostKeyword.REINFORCE),
            w("awaken").thenReturn(CostKeyword.AWAKEN),
            w("emerge").thenReturn(CostKeyword.EMERGE),
            w("escape").thenReturn(CostKeyword.ESCAPE),
            w("embalm").thenReturn(CostKeyword.EMBALM),
            w("eternalize").thenReturn(CostKeyword.ETERNALIZE));

    /// Cost source for a modify-cost effect: a keyword ability ("buyback
    /// costs"), "[Keyword] abilities you activate" (Fluctuator), or a
    /// spell-matching subject ("spells you cast"). Keyword variants are
    /// tried first so their trailing "costs"/"abilities" isn't consumed by
    /// the subject grammar.
    private static final Parser<CostSource> COST_SOURCE = anyOf(
            COST_KEYWORD.followedBy(w("costs")).<CostSource>map(CostSource.Ability::new),
            // "[Keyword] abilities you activate" — treats the keyword's
            // activation costs collectively (e.g., Fluctuator).
            COST_KEYWORD
                    .followedBy(w("abilities"))
                    .followedBy(ciWords("you activate"))
                    .<CostSource>map(CostSource.Ability::new),
            SubjectParsers.SUBJECT.<CostSource>map(CostSource.Spell::new));

    /// "[source] cost[s] <mana> more/less [to cast | to activate]." Handles
    /// both spell-subject forms ({@code Spells you cast cost {1} more to
    /// cast}) and keyword-ability forms ({@code Buyback costs cost {2}
    /// less}, {@code Cycling abilities you activate cost {2} less to
    /// activate}).
    static final Parser<Effect.ModifyCost> MODIFY_COST = sequence(
                    COST_SOURCE.followedBy(anyCiWord("costs", "cost")),
                    MANA_SYMBOL.atLeastOnce(),
                    COST_DELTA,
                    Effect.ModifyCost::new)
            .optionallyFollowedBy(anyOf(ciWords("to cast"), ciWords("to activate")), (mc, ign) -> mc);

    // Lose ability

    /// The tail of a "[subject] lose[s] …" clause. Either "all abilities"
    /// (produces {@link Effect.LoseAbility.Lost.All}) or a keyword list
    /// (produces {@link Effect.LoseAbility.Lost.Specific}).
    private static final Parser<Effect.LoseAbility.Lost> LOST_ABILITIES = anyOf(
            ciWords("all abilities").thenReturn(Effect.LoseAbility.Lost.All.ALL),
            KeywordParsers.KEYWORD_LIST.map(Effect.LoseAbility.Lost.Specific::new));

    static final Parser<Effect.LoseAbility> LOSE_ABILITY = sequence(
                    SubjectParsers.SUBJECT.followedBy(anyCiWord("loses", "lose")),
                    LOST_ABILITIES,
                    Effect.LoseAbility::new)
            .optionallyFollowedBy(DURATION, Effect.LoseAbility::withDuration);

    /// "[subject] can't be the target[s] of spells or abilities / of [what]."
    static final Parser<Effect.CantBeTargeted> CANT_BE_TARGETED = anyOf(
            SubjectParsers.SUBJECT
                    .followedBy(ciWords("can't be the"))
                    .followedBy(anyCiWord("targets", "target"))
                    .followedBy(ciWords("of spells or abilities"))
                    .map(Effect.CantBeTargeted::new),
            sequence(
                    SubjectParsers.SUBJECT
                            .followedBy(ciWords("can't be the"))
                            .followedBy(anyCiWord("targets", "target"))
                            .followedBy(w("of")),
                    SelectorParsers.SELECTOR,
                    Effect.CantBeTargeted::new));

    /// "[subject] must be blocked [if able]." / "[subject] blocks [if able]
    /// [this turn]." — the former already exists as MUST_BE_BLOCKED; this is
    /// the must-block-as-blocker variant.
    static final Parser<Effect.MustBlock> MUST_BLOCK = SubjectParsers.SUBJECT
            .followedBy(anyCiWord("blocks", "block"))
            .map(Effect.MustBlock::new)
            .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.MustBlock::withTarget)
            .optionallyFollowedBy(DURATION, Effect.MustBlock::withDuration)
            .optionallyFollowedBy(ciWords("if able"), (mb, ign) -> mb);

    /// "[players] play with [their/its/your] hands revealed."
    static final Parser<Effect.PlayWithHandsRevealed> PLAY_WITH_HANDS_REVEALED = SubjectParsers.PLAYER_SUBJECT
            .followedBy(ciWords("play with"))
            .followedBy(anyCiWord("your", "their", "its"))
            .followedBy(ciWords("hands revealed"))
            .map(Effect.PlayWithHandsRevealed::new);

    // ── Master dispatcher ──────────────────────────────────────────────

    /// Trailing `if <predicate>` condition on any effect — emits a
    /// {@link Condition.Kind#IF}. Used as an optional suffix on
    /// {@link #EFFECT} so `Draw a card if you have no cards in hand.`
    /// becomes {@link Effect.Conditional}.
    private static final Parser<Condition> IF_CONDITION = w("if").then(
                    word().atLeastOnce().map(words -> String.join(" ", words)))
            .map(Condition::ifCondition);

    /// Trailing `unless <predicate>` condition on any effect — emits a
    /// {@link Condition.Kind#UNLESS}. Uses {@link #CONDITION_TOKEN} so the
    /// predicate can contain mana symbols (e.g., Rhystic Deluge: "Tap
    /// target creature unless its controller pays {1}.").
    private static final Parser<Condition> UNLESS_CONDITION = w("unless")
            .then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Condition::unlessCondition);

    private static final Parser<Effect> BASE_EFFECT = Parser.<Effect>anyOf(
            DESTROY,
            EXILE,
            BOUNCE,
            SACRIFICE,
            DEAL_DAMAGE_SPLIT, // must precede DEAL_DAMAGE (shares "[source] deals N damage to A" prefix)
            DEAL_DAMAGE,
            GAIN_LIFE,
            LOSE_LIFE,
            DRAW,
            DISCARD,
            MILL,
            SCRY,
            SEARCH,
            SHUFFLE,
            REVEAL,
            TAP_OR_UNTAP, // must precede TAP — "tap or untap" starts with "tap"
            PLAY_WITH_TOP_REVEALED,
            CAN_BLOCK_ADDITIONAL, // must precede CANT_BLOCK and CAN_BLOCK_ANY_NUMBER
            CAN_BLOCK_ANY_NUMBER, // must precede CANT_BLOCK family checks
            TAP,
            UNTAP,
            ADD_COUNTERS,
            REMOVE_COUNTERS,
            COUNTER_SPELL,
            GAIN_ABILITY,
            MODIFY_PT_AND_ABILITY, // must precede MODIFY_PT
            MODIFY_PT,
            GAIN_CONTROL,
            EXCHANGE_CONTROL,
            CREATE_TOKEN,
            ADD_MANA,
            TRANSFORM,
            COPY,
            FIGHT,
            WIN_GAME,
            LOSE_GAME,
            ZONE_MOVE,
            PREVENT,
            CANT_ATTACK_OR_BLOCK_ALONE, // must precede CANT_ATTACK_OR_BLOCK
            CANT_ATTACK_OR_BLOCK, // must precede CANT_ATTACK and CANT_BLOCK
            CANT_HAVE_COUNTERS,
            CANT_BE_REGENERATED,
            CANT_BE_EQUIPPED,
            UNATTACH,
            CAST_WITHOUT_PAYING, // must precede CAST_AS_THOUGH / CAST_FROM_ZONE (same "may cast" prefix)
            CAST_AS_THOUGH, // must precede CAST_FROM_ZONE (both start with "may cast")
            SPEND_MANA_AS_THOUGH,
            SUPPRESS_ETB_TRIGGERS,
            CANT_BLOCK_AND_BE_BLOCKED, // must precede CANT_BLOCK and CANT_BE_BLOCKED
            CANT_BLOCK_ALONE, // must precede CANT_BLOCK
            CANT_ATTACK_ALONE, // must precede CANT_ATTACK
            MUST_ATTACK,
            CANT_ATTACK_WHOM, // must precede CANT_ATTACK
            CANT_BLOCK,
            CANT_ATTACK,
            CANT_PLAY_LANDS,
            TAKE_INITIATIVE,
            UNTAP_LIMIT, // must precede DONT_UNTAP (longer "can't untap more than" prefix)
            DONT_UNTAP,
            CAST_COUNT_LIMIT,
            CHOOSE_PLAYER_VOTE, // must precede CHOOSE (starts with "choose")
            SET_BASE_PT,
            CAN_BLOCK_ONLY, // must precede CAN_BLOCK_ANY_NUMBER
            EXCHANGE_ZONES,
            EXCHANGE_LIFE_WITH_PROPERTY,
            MANA_POOL_PERSISTS,
            MANA_SPEND_RESTRICTION,
            ADDITIONAL_COST,
            ATTACK_LIMIT,
            DOUBLE_PT,
            CHANGE_THE_TARGET,
            ENTER_AS_COPY,
            CANT_CYCLE,
            CANT_ACTIVATE,
            CANT_BE_BLOCKED,
            CANT_SEARCH_LIBRARIES,
            PER_TURN_LIMIT, // must precede CANT_CAST (shares "can't cast" prefix)
            CANT_CAST,
            RESTRICT_SPELL_TIMING,
            NO_MAXIMUM_HAND_SIZE,
            MAXIMUM_HAND_SIZE_DELTA,
            TAKE_EXTRA_TURN,
            PLAY_LANDS_FROM,
            PLAY_ADDITIONAL_LANDS,
            SKIP,
            RING_TEMPTS,
            LOOK_AT,
            CAST_FROM_ZONE,
            CHOOSE_NEW_TARGETS,
            CHANGE_ANY_TARGETS,
            CHOOSE,
            BECOME_MONARCH,
            EXCHANGE_LIFE_TOTALS,
            GET_MARKER,
            CANT_BE_COUNTERED,
            CANT_BE_TARGETED,
            ABLE_TO_BLOCK_DO_SO, // must precede MUST_BE_BLOCKED
            MUST_BE_BLOCKED,
            MUST_BLOCK,
            PLAY_WITH_HANDS_REVEALED,
            LIFE_TOTAL_BECOMES,
            RULE_DOESNT_APPLY,
            ENTER_WITH_COUNTERS, // must precede ENTER_TAPPED
            ENTER_TAPPED,
            SET_ALL_COLORS, // must precede SET_COLORS
            SET_COLORLESS, // must precede SET_COLORS (COLORLESS is unambiguous)
            LOSE_SUPERTYPE, // must precede SET_* since all share "are/is" head
            SET_SUPERTYPE,
            TURN_FACE_UP,
            BECOME_PT_TYPE, // must precede SET_COLORS since both start with "are/is"
            SET_COLORS,
            SET_SUBTYPE,
            REGENERATE,
            LOSE_ABILITY,
            MODIFY_COST);

    /// Prefix "If [condition], [effect]" — e.g., Idle Thoughts: "If you
    /// have no cards in hand." The predicate runs to the comma. Does NOT
    /// match "If you do," / "If they do," — those tails are consumed by
    /// {@link #IF_DO_CONTINUATION} as follow-ups to a preceding
    /// {@link Effect.Optional}.
    private static final Parser<Condition> IF_PREFIX_CONDITION = sequence(
                    w("if").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
                    string(","),
                    (text, _) -> text)
            .suchThat(s -> !s.equalsIgnoreCase("you do") && !s.equalsIgnoreCase("they do"), "non-may-linked if")
            .map(Condition::ifCondition);

    /// `. If you/they do, [effect]` — follow-up clause that attaches to a
    /// preceding {@link Effect.Optional} (action wrapped by "you may …").
    /// Consumes the preceding sentence-terminating period so downstream
    /// {@code EFFECT_SEQUENCE} delimiters see a clean boundary.
    private static final Parser<Effect> IF_DO_CONTINUATION = string(".")
            .then(w("if"))
            .then(anyCiWord("you", "they"))
            .then(w("do"))
            .followedBy(string(","))
            .then(BASE_EFFECT);

    /// `[player] may <action>` — a single generic parser. Uses
    /// {@link Parser#flatMap} to capture the already-parsed player subject
    /// in a closure and dispatch to any player-scoped action tail. The
    /// optional `. If you/they do, …` continuation attaches to the produced
    /// {@link Effect.Optional} so the "may" and its conditional stay
    /// structurally linked. Adding a new may-able effect = one more branch
    /// in the inner {@code anyOf}.
    static final Parser<Effect.Optional> MAY = SubjectParsers.PLAYER_SUBJECTS
            .followedBy(w("may"))
            .flatMap(subject -> Parser.<Effect>anyOf(
                    DRAW_NO_PLAYER.map(amount -> new Effect.Draw(subject, amount)),
                    DISCARD_NO_PLAYER.map(d -> new Effect.Discard(subject, d)),
                    GAIN_LIFE_NO_PLAYER.map(amount -> new Effect.GainLife(subject, amount)),
                    LOSE_LIFE_NO_PLAYER.map(amount -> new Effect.LoseLife(subject, amount)),
                    PLAY_ADDITIONAL_LANDS_NO_PLAYER
                            .map(amount -> new Effect.PlayAdditionalLands(subject, amount))
                            .optionallyFollowedBy(DURATION, Effect.PlayAdditionalLands::withDuration)
                            .map(e -> (Effect) e),
                    // Effects where the "may" actor is the implicit source,
                    // not a parameter on the effect — the target comes from
                    // the parser directly (e.g., "may tap target creature",
                    // "may destroy target Aura", "may add {R}{R}", "may
                    // skip that draw").
                    TAP,
                    DESTROY,
                    ADD_MANA,
                    SKIP))
            .map(Effect.Optional::new)
            .optionallyFollowedBy(IF_DO_CONTINUATION, Effect.Optional::withIfDone);

    /// "If [subject] would [event], [replacement] instead." — replacement
    /// effect (rule 614, e.g., Thought Reflection: "If you would draw a
    /// card, draw two cards instead."). Must precede the generic if-prefix
    /// conditional so the "instead" suffix is honored.
    private static final Parser<Effect.Replace> REPLACE = sequence(
            w("if").then(SubjectParsers.SUBJECT),
            ciWords("would")
                    .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
                    .followedBy(string(",")),
            // Replacement allows either a plain effect or a "may"-wrapped
            // one (e.g., Obstinate Familiar: "… you may skip that draw
            // instead."). MAY precedes BASE_EFFECT so the "you may" prefix
            // isn't treated as a bare subject.
            Parser.<Effect>anyOf(MAY, BASE_EFFECT).followedBy(w("instead")),
            Effect.Replace::new);

    public static final Parser<Effect> EFFECT = Parser.<Effect>anyOf(
                    // "[player] may …" — single entry point for every
                    // may-wrapped action. Must precede BASE_EFFECT so "you
                    // may X" is captured as Effect.Optional rather than a
                    // plain Effect.
                    MAY,
                    REPLACE,
                    // Panharmonicon-style trigger duplication; shares the
                    // "If …," prefix with IF_PREFIX_CONDITION so must come
                    // first. The ", that ability triggers …" tail is what
                    // distinguishes it.
                    ADDITIONAL_ETB_TRIGGERS,
                    sequence(IF_PREFIX_CONDITION, BASE_EFFECT, (c, e) -> new Effect.Conditional(e, c)),
                    BASE_EFFECT)
            .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c))
            .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c));
}
