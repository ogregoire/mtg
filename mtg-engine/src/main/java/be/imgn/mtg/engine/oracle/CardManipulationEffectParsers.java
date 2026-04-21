package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Map;

import com.google.common.labs.parse.Parser;

/// Leaf-effect parsers for card-manipulation verbs: draw, discard, mill,
/// scry (and surveil, modelled as scry), search, shuffle, and reveal.
/// Produces [Effect] values consumed by [EffectParsers#BASE_EFFECT] /
/// [EffectParsers#CLAUSE]. The `*_NO_PLAYER` parsers are package-private
/// so [EffectParsers] can re-bind them onto a shared player actor in the
/// "X does A and B" chains.
final class CardManipulationEffectParsers {
    private CardManipulationEffectParsers() {}

    private static final Subject YOU = Subject.player(Subject.PlayerRef.YOU);

    // ── Draw ──────────────────────────────────────────────────────────

    /// Amount following "draw[s]". Either `[N] card(s) [for each X]?` or
    /// `card(s) equal to [property]` (Soul's Majesty: "Draw cards equal to
    /// the power of target creature you control.").
    private static final Parser<Amount> DRAW_AMOUNT = anyOf(
            SelectorParsers.AMOUNT
                    .followedBy(phrase("card(s)"))
                    .optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, each) -> each),
            phrase("card(s)").then(words("equal to")).then(CountOfParsers.PROPERTY_OF_AMOUNT));

    static final Parser<Amount> DRAW_NO_PLAYER =
            DamageEffectParsers.each(anyCiWord("draws", "draw")).then(DRAW_AMOUNT);

    static final Parser<Effect.Draw> DRAW = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, DRAW_NO_PLAYER, Effect.Draw::new),
            DRAW_NO_PLAYER.map(amount -> new Effect.Draw(YOU, amount)));

    // ── Discard ───────────────────────────────────────────────────────

    /// Discard clause following the verb "discard": "N card(s) [at random]",
    /// "a card for each X" (count-scaled — Mind Sludge), or "<possessive>
    /// hand".
    private static final Parser<Discarded> DISCARD_WHAT = anyOf(
            // "N card(s) at random" first so the at-random flag wins.
            sequence(SelectorParsers.AMOUNT.followedBy(phrase("card(s)")), words("at random"), (amt, ign) ->
                    (Discarded) new Discarded.Cards(amt, true)),
            // "N card(s) for each X" — count replaces N (e.g., Mind Sludge).
            sequence(SelectorParsers.AMOUNT.followedBy(phrase("card(s)")), CountOfParsers.FOR_EACH, (_, count) ->
                    (Discarded) new Discarded.Cards(count, false)),
            SelectorParsers.AMOUNT.followedBy(phrase("card(s)")).<Discarded>map(amt -> new Discarded.Cards(amt, false)),
            // "N of them" — pronoun back-reference to a recent card
            // group (Soldevi Sage: "Draw three cards, then discard one
            // of them."). Treated as a plain card-count discard since
            // the pronoun binding is resolved at resolution time.
            SelectorParsers.AMOUNT.followedBy(words("of them")).<Discarded>map(amt -> new Discarded.Cards(amt, false)),
            anyWord("your", "their", "his", "her", "its").then(word("hand")).thenReturn(Discarded.Hand.HAND),
            // "all the cards in [poss] hand" — explicit whole-hand form
            // (Tolarian Winds: "Discard all the cards in your hand…").
            words("all the cards in")
                    .then(anyWord("your", "their", "his", "her", "its"))
                    .followedBy(word("hand"))
                    .thenReturn(Discarded.Hand.HAND),
            // "discards all Trap cards" / "discards a creature card" —
            // selector-bound discard.
            SelectorParsers.SELECTOR.<Discarded>map(Discarded.Matching::new),
            // "discard it" / "discard that card" / "discard the rest" —
            // pronoun or demonstrative target (Fa'adiyah Seer;
            // Breakthrough: "choose X cards in your hand and discard
            // the rest.").
            anyOf(
                            anyCiWord("it", "them").map(Subject::pronoun),
                            ciWords("that card").thenReturn(Subject.demonstrative("that", "card")),
                            ciWords("the rest").thenReturn(Subject.pronoun("the rest")))
                    .<Discarded>map(Discarded.Specific::new));

    static final Parser<Discarded> DISCARD_NO_PLAYER =
            DamageEffectParsers.each(anyCiWord("discards", "discard")).then(DISCARD_WHAT);

    static final Parser<Effect.Discard> DISCARD = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, DISCARD_NO_PLAYER, Effect.Discard::new),
            DISCARD_NO_PLAYER.map(d -> new Effect.Discard(YOU, d)));

    // ── Mill ──────────────────────────────────────────────────────────

    /// "half [possessive] library[, rounded up/down]" — an Amount used by
    /// [#MILL_NO_PLAYER] for Traumatize ("mills half their library,
    /// rounded down"). Mirrors the half-life amount used by lose-life;
    /// default rounding is UP.
    private static final Parser<Amount> HALF_LIBRARY = ciWords("half")
            .then(anyWord("your", "their", "its"))
            .followedBy(word("library"))
            .thenReturn((Amount) new Amount.Half(
                    new Amount.PropertyOf(Subject.player(Subject.PlayerRef.THEY), "library"), Amount.Half.Rounding.UP))
            .optionallyFollowedBy(
                    string(",").then(word("rounded")).then(anyWord("up", "down")),
                    (base, dir) -> new Amount.Half(
                            ((Amount.Half) base).base(),
                            dir.equalsIgnoreCase("down") ? Amount.Half.Rounding.DOWN : Amount.Half.Rounding.UP));

    static final Parser<Amount> MILL_NO_PLAYER = anyCiWord("mills", "mill")
            .then(anyOf(
                    // "[N] card(s)" — the common numeric form.
                    SelectorParsers.AMOUNT.followedBy(phrase("card(s)")),
                    // "half [possessive] library[, rounded up/down]" — Traumatize.
                    HALF_LIBRARY,
                    // "cards equal to [owner] [property]" — property-driven
                    // (e.g., Space-Time Anomaly: "mills cards equal to
                    // your life total").
                    phrase("card(s)").then(words("equal to")).then(CountOfParsers.PROPERTY_OF_AMOUNT)));

    static final Parser<Effect.Mill> MILL = anyOf(
                    // PLAYER_SUBJECTS also matches possessives like "its
                    // controller", which Psychic Strike / Countermand need.
                    sequence(SubjectParsers.PLAYER_SUBJECTS, MILL_NO_PLAYER, Effect.Mill::new),
                    MILL_NO_PLAYER.map(amount -> new Effect.Mill(YOU, amount)))
            // Optional ", where X is <def>" — binds the X used in an
            // Amount.variable() count (Dreadwaters).
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.Mill::withXDefinition);

    // ── Scry / Surveil / Search ───────────────────────────────────────

    static final Parser<Effect.Scry> SCRY =
            anyCiWord("scry", "surveil").then(SelectorParsers.AMOUNT).map(Effect.Scry::new);

    static final Parser<Effect.Search> SEARCH = sequence(
            w("search").then(anyWord("your", "their", "its")),
            words("library for").then(SelectorParsers.SELECTOR),
            Effect.Search::new);

    // ── Shuffle ───────────────────────────────────────────────────────

    /// Tail of a shuffle clause following the verb: either nothing, a bare
    /// zone (the implicit target, e.g., "their library"), or a two-zone
    /// "[source] into [destination]" phrase (Mnemonic Nexus). Each arm
    /// produces the (source, destination) pair to fold onto the parsed
    /// player; nulls mean "use the default library".
    private static final Parser<Map.Entry<Zone, Zone>> SHUFFLE_TAIL = anyOf(
            sequence(
                    ZoneParsers.ZONE.followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (src, dst) -> Map.<Zone, Zone>entry(src, dst)),
            ZoneParsers.ZONE.map(z -> Map.<Zone, Zone>entry(z, z)));

    /// "[player] shuffles [their library | [source] into [destination]]?.".
    /// Implicit "you" when oracle text omits the subject. Both zones are
    /// nullable in the resulting [Effect.Shuffle].
    static final Parser<Effect.Shuffle> SHUFFLE = anyOf(
            // "[player] shuffles [subject] into [zone]" — shuffle an object
            // into a zone (Cerulean Sphinx: "This creature's owner shuffles
            // it into their library.").
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("shuffle(s)")),
                    SubjectParsers.ATOMIC_SUBJECT.followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (player, subj, dest) -> new Effect.Shuffle(player, null, dest).withSubject(subj)),
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("shuffle(s)")),
                    SHUFFLE_TAIL,
                    (player, tail) -> new Effect.Shuffle(player, tail.getKey(), tail.getValue())),
            SubjectParsers.PLAYER_LIKE_SUBJECT
                    .followedBy(phrase("shuffle(s)"))
                    .map(player -> new Effect.Shuffle(player, null, null)),
            // Imperative "shuffle [subject] from [zone] into [zone]" —
            // See Beyond: "shuffle a card from your hand into your
            // library.". Must precede the zoneless variant below.
            sequence(
                    w("shuffle").then(SubjectParsers.ATOMIC_SUBJECT),
                    ZoneParsers.ZONE_SOURCE.followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (subj, _, dest) -> new Effect.Shuffle(YOU, null, dest).withSubject(subj)),
            // Imperative "shuffle [subject] into [zone]" — YOU-defaulted
            // form without an explicit player-actor (Alabaster Dragon:
            // "… shuffle it into its owner's library.").
            sequence(
                    w("shuffle").then(SubjectParsers.ATOMIC_SUBJECT).followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (subj, dest) -> new Effect.Shuffle(YOU, null, dest).withSubject(subj)),
            anyCiWord("shuffles", "shuffle")
                    .then(SHUFFLE_TAIL)
                    .map(tail -> new Effect.Shuffle(YOU, tail.getKey(), tail.getValue())),
            anyCiWord("shuffles", "shuffle").thenReturn(new Effect.Shuffle(YOU, null, null)));

    // ── Reveal ────────────────────────────────────────────────────────

    /// "[poss] hand" as a reveal target — e.g., Trapfinder's Trick:
    /// "Target player reveals their hand…".
    static final Parser<Subject> POSSESSIVE_HAND = anyWord("your", "their", "his", "her", "its")
            .followedBy(word("hand"))
            .<Subject>map(poss -> Subject.possessiveSubject(poss.toLowerCase(), "hand"));

    static final Parser<Effect.Reveal> REVEAL = anyOf(
            // "[player] reveal[s] [their hand | subject]" — player-actor form
            // (Trapfinder's Trick).
            sequence(
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(phrase("reveal(s)")),
                    anyOf(POSSESSIVE_HAND, SubjectParsers.SUBJECT),
                    (actor, what) -> new Effect.Reveal(actor, what)),
            w("reveal").then(anyOf(POSSESSIVE_HAND, SubjectParsers.SUBJECT)).map(Effect.Reveal::new));
}
