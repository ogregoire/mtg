package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.Map;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.*;

/// Leaf-effect parsers for card-manipulation verbs: draw, discard, mill,
/// scry, surveil, search, shuffle, and reveal.
/// Produces [Effect] values consumed by [EffectParsers#BASE_EFFECT] /
/// [EffectParsers#CLAUSE]. The `*_NO_PLAYER` parsers are package-private
/// so [EffectParsers] can re-bind them onto a shared player actor in the
/// "X does A and B" chains.
final class CardManipulationEffectParsers {
    private CardManipulationEffectParsers() {}

    private static final Subject YOU = Subject.player(PlayerRef.Pronoun.YOU);

    // ── Draw ──────────────────────────────────────────────────────────

    /// Amount following "draw[s]". Either `[N] \[additional\]? card(s) [for
    /// each X]?` or `card(s) equal to [property]` (Soul's Majesty: "Draw
    /// cards equal to the power of target creature you control."). The
    /// optional "additional" modifier is flavor — it's implied by the
    /// enclosing draw-step trigger (Kami of the Crescent Moon: "that
    /// player draws an additional card.").
    private static final Parser<Amount> DRAW_AMOUNT = Parser.anyOf(
            AMOUNT.optionallyFollowedBy(word("additional"), (a, _) -> a)
                    .followedBy(phrase("card(s)"))
                    // "draws that many cards plus/minus one" — Into the
                    // Night (plus), Dark Deal (minus). The plus/minus
                    // tail modifies the count after "cards" (rather
                    // than before, like the AMOUNT-level "plus" suffix).
                    .optionallyFollowedBy(word("plus").then(AMOUNT), Amount.Plus::new)
                    .optionallyFollowedBy(word("minus").then(AMOUNT), Amount.Minus::new)
                    .optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, each) -> each),
            phrase("card(s) equal to").then(CountOfParsers.PROPERTY_OF_AMOUNT),
            // "as many cards as [subject] discarded this way" — Forget.
            // Typed back-reference to the count discarded in a preceding
            // clause of the same effect. Uses bare PLAYER_SUBJECT so the
            // participial "discarded this way" isn't absorbed by
            // PLAYER_WITH_PARTICIPLE.
            phrase("as many cards as")
                    .then(SubjectParsers.PLAYER_SUBJECT)
                    .followedBy(phrase("discarded this way"))
                    .map(Amount.CardsDiscardedThisWay::new));

    static final Parser<Amount> DRAW_NO_PLAYER =
            DamageEffectParsers.each(phrase("Draw(s)")).then(DRAW_AMOUNT);

    static final Parser<Effect.Draw> DRAW = anyOf(
                    sequence(SubjectParsers.PLAYER_SUBJECTS, DRAW_NO_PLAYER, Effect.Draw::new),
                    DRAW_NO_PLAYER.map(amount -> new Effect.Draw(YOU, amount)))
            // Optional ", where X is <def>" — binds the X in a variable
            // amount (Lucid Dreams: "Draw X cards, where X is the
            // number of card types among cards in your graveyard.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.Draw::withXDefinition);

    // ── Discard ───────────────────────────────────────────────────────

    /// Discard clause following the verb "discard": "N card(s) [at random]",
    /// "a card for each X" (count-scaled — Mind Sludge), or "<possessive>
    /// hand".
    private static final Parser<Discarded> DISCARD_WHAT = anyOf(
            // "N card(s) at random" first so the at-random flag wins.
            sequence(AMOUNT.followedBy(phrase("card(s)")), phrase("at random"), (amt, ign) ->
                    (Discarded) new Discarded.Cards(amt, true)),
            // "N card(s) for each X" — count replaces N (e.g., Mind Sludge).
            sequence(AMOUNT.followedBy(phrase("card(s)")), CountOfParsers.FOR_EACH, (_, count) ->
                    (Discarded) new Discarded.Cards(count, false)),
            AMOUNT.followedBy(phrase("card(s)")).<Discarded>map(amt -> new Discarded.Cards(amt, false)),
            // "N of them" — pronoun back-reference to a recent card
            // group (Soldevi Sage: "Draw three cards, then discard one
            // of them."). Treated as a plain card-count discard since
            // the pronoun binding is resolved at resolution time.
            AMOUNT.followedBy(phrase("of them")).<Discarded>map(amt -> new Discarded.Cards(amt, false)),
            // Singular "hand" or plural "hands" (Wheel and Deal: "Any
            // number of target opponents each discard their hands").
            phrase("[your|their|his|her|its] [hand|hands]").thenReturn(Discarded.Hand.HAND),
            // "all the cards in [poss] hand" — explicit whole-hand form
            // (Tolarian Winds: "Discard all the cards in your hand…").
            phrase("all the cards in [your|their|his|her|its] hand").thenReturn(Discarded.Hand.HAND),
            // "discards all Trap cards" / "discards a creature card" —
            // selector-bound discard.
            SELECTOR.<Discarded>map(Discarded.Matching::new),
            // "discard it" / "discard that card" / "discard the rest" —
            // pronoun or demonstrative target (Fa'adiyah Seer;
            // Breakthrough: "choose X cards in your hand and discard
            // the rest."). The "it"/"them" branch keeps the matched
            // token because Subject.pronoun needs it.
            anyOf(
                            phrase("it").thenReturn(Subject.pronoun(PronounType.IT)),
                            phrase("them").thenReturn(Subject.pronoun(PronounType.THEM)),
                            phrase("that card").thenReturn(Subject.demonstrative("that", "card")),
                            // "those cards" — back-reference to a previously chosen
                            // card group (Mind Warp: "choose X cards from it. That
                            // player discards those cards.").
                            phrase("those cards").thenReturn(Subject.pronoun(PronounType.THOSE_CARDS)),
                            phrase("the rest").thenReturn(Subject.pronoun(PronounType.THE_REST)))
                    .<Discarded>map(Discarded.Specific::new));

    static final Parser<Discarded> DISCARD_NO_PLAYER =
            DamageEffectParsers.each(phrase("Discard(s)")).then(DISCARD_WHAT);

    static final Parser<Effect.Discard> DISCARD = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, DISCARD_NO_PLAYER, Effect.Discard::new),
            DISCARD_NO_PLAYER.map(d -> new Effect.Discard(YOU, d)));

    // ── Mill ──────────────────────────────────────────────────────────

    /// "half [possessive] library[, rounded up/down]" — an Amount used by
    /// [#MILL_NO_PLAYER] for Traumatize ("mills half their library,
    /// rounded down"). Mirrors the half-life amount used by lose-life;
    /// default rounding is UP. The library owner is left unspecified —
    /// the enclosing [Effect.Mill]'s `player` binds it.
    private static final Parser<Amount.Half> HALF_LIBRARY = phrase("half [your|their|its] library")
            .thenReturn(new Amount.Half(new Amount.ZoneSize(ZoneParsers.ownedZone("their", Zone.Name.LIBRARY))))
            .optionallyFollowedBy(CountOfParsers.ROUNDING_DIRECTION, Amount.Half::withRounding);

    /// Optional "each" distributive prefix — "[subjects] each mill ..."
    /// (e.g., Singularity Rupture: "any number of target players each
    /// mill half their library"). Mirrors the same pattern used by
    /// [#DISCARD_NO_PLAYER].
    static final Parser<Amount> MILL_NO_PLAYER = DamageEffectParsers.each(phrase("Mill(s)"))
            .then(anyOf(
                    // "[N] card(s)" — the common numeric form.
                    AMOUNT.followedBy(phrase("card(s)")),
                    // "half [possessive] library[, rounded up/down]" — Traumatize.
                    HALF_LIBRARY,
                    // "cards equal to [owner] [property]" — property-driven
                    // (e.g., Space-Time Anomaly: "mills cards equal to
                    // your life total").
                    phrase("card(s) equal to").then(CountOfParsers.PROPERTY_OF_AMOUNT)));

    static final Parser<Effect.Mill> MILL = anyOf(
                    // PLAYER_SUBJECTS also matches possessives like "its
                    // controller", which Psychic Strike / Countermand need.
                    sequence(SubjectParsers.PLAYER_SUBJECTS, MILL_NO_PLAYER, Effect.Mill::new),
                    MILL_NO_PLAYER.map(amount -> new Effect.Mill(YOU, amount)))
            // Optional trailing "for each X" multiplier — Trenchpost:
            // "Target player mills a card for each Locus you control."
            // Replaces the base amount with the count-of expression.
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, (m, each) -> new Effect.Mill(m.player(), each))
            // Optional ", where X is <def>" — binds the X used in an
            // Amount.variable() count (Dreadwaters).
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.Mill::withXDefinition);

    // ── Scry / Surveil / Search ───────────────────────────────────────

    static final Parser<Effect.Scry> SCRY = phrase("Scry")
            .then(AMOUNT)
            .map(Effect.Scry::new)
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.Scry::withXDefinition);

    /// 702.139 — "Adapt N." — Skitter Eel: "{2}{U}: Adapt 2."
    static final Parser<Effect.Adapt> ADAPT =
            phrase("Adapt").then(SelectorParsers.INTEGER).map(Effect.Adapt::new);

    static final Parser<Effect.Surveil> SURVEIL = phrase("Surveil").then(AMOUNT).map(Effect.Surveil::new);

    /// "Search \[whose\] library for [selector]." — possessive is either
    /// a pronoun ("your", "their", "its") mapped to a [Subject.PlayerRef]
    /// or a subject-qualified player ("target player's", "each opponent's";
    /// Extract: "Search target player's library for a card and exile it.").
    /// The parsed library owner lands on [Effect.Search#who()].
    static final Parser<Effect.Search> SEARCH = anyOf(
            sequence(
                    phrase("Search")
                            .then(anyOf(
                                    word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                                    word("their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
                                    word("its").thenReturn(Subject.player(PlayerRef.Pronoun.THAT_PLAYER))))
                            .followedBy(phrase("library for")),
                    SELECTOR,
                    Effect.Search::new),
            sequence(
                    phrase("Search").then(SubjectParsers.PLAYER_SUBJECTS).followedBy(phrase("'s library for")),
                    SELECTOR,
                    Effect.Search::new));

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
            // "[player] shuffles [subject] from [source] into [dest]" —
            // shuffle a subset (sourced) into a zone (Dwell on the Past
            // / Stream of Consciousness: "Target player shuffles up to
            // four target cards from their graveyard into their
            // library."). Must precede the bare "shuffles [subject]
            // into [zone]" form so the "from …" tail is not left
            // unconsumed.
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("shuffle(s)")),
                    SubjectParsers.ATOMIC_SUBJECT,
                    ZoneExpressionParsers.IN_ZONE_FROM.followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (player, subj, _, dest) -> new Effect.Shuffle(player, null, dest).withSubject(subj)),
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
                    phrase("Shuffle").then(SubjectParsers.ATOMIC_SUBJECT),
                    ZoneParsers.ZONE_SOURCE.followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (subj, _, dest) -> new Effect.Shuffle(YOU, null, dest).withSubject(subj)),
            // "Shuffle [subject] and [possessive] [zone] into [zone]" —
            // Elixir of Immortality: "Shuffle this artifact and your
            // graveyard into their owner's library." The possessive
            // zone is the source; the subject is shuffled alongside
            // the source-zone contents.
            sequence(
                    phrase("Shuffle").then(SubjectParsers.ATOMIC_SUBJECT),
                    word("and")
                            .then(phrase("[your|their|its|his|her]"))
                            .then(SelectorParsers.ZONE_NAME)
                            .<Zone>map(z -> ZoneParsers.zoneFor("your", z)),
                    word("into").then(ZoneParsers.ZONE),
                    (subj, src, dest) -> new Effect.Shuffle(YOU, src, dest).withSubject(subj)),
            // Imperative "shuffle [subject] into [zone]" — YOU-defaulted
            // form without an explicit player-actor (Alabaster Dragon:
            // "… shuffle it into its owner's library.").
            sequence(
                    phrase("Shuffle").then(SubjectParsers.ATOMIC_SUBJECT).followedBy(word("into")),
                    ZoneParsers.ZONE,
                    (subj, dest) -> new Effect.Shuffle(YOU, null, dest).withSubject(subj)),
            phrase("Shuffle(s)")
                    .then(SHUFFLE_TAIL)
                    .map(tail -> new Effect.Shuffle(YOU, tail.getKey(), tail.getValue())),
            phrase("Shuffle(s)").thenReturn(new Effect.Shuffle(YOU, null, null)));

    // ── Reorder zone ─────────────────────────────────────────────────

    /// "reorder \[zone\] as you choose." — the player rearranges the cards in
    /// a zone in any order (e.g., Fossil Find: "reorder your graveyard as
    /// you choose"). The actor is always the implicit controller ("you").
    static final Parser<Effect.ReorderZone> REORDER_ZONE = phrase("reorder")
            .then(ZoneParsers.ZONE)
            .followedBy(phrase("as you choose"))
            .map(zone -> new Effect.ReorderZone(YOU, zone));

    // ── Reveal ────────────────────────────────────────────────────────

    /// "\[your|their|…\] hand" — revealing the hand reveals every card
    /// it contains, so we parse the phrase to a selector over all
    /// [GameObjectType#CARD] objects in the [Zone.Name#HAND] zone. The
    /// possessive itself is discarded: the owning player is fixed by
    /// the surrounding verb's subject.
    static final Parser<Subject> HAND = phrase("[your|their|his|her|its] hand")
            .thenReturn(Subject.select(new Selector(Selector.Quantifier.all(), GameObjectType.CARD)
                    .withZone(ZoneParsers.ownedZone("their", Zone.Name.HAND))));

    /// Single card (one) in the hand zone — the target for the at-random
    /// reveal form ("reveals a card at random from their hand"). Distinct
    /// from [#HAND] which selects ALL cards.
    private static final Subject AT_RANDOM_CARD_FROM_HAND =
            Subject.select(new Selector(Selector.Quantifier.one(), GameObjectType.CARD)
                    .withZone(ZoneParsers.ownedZone("their", Zone.Name.HAND)));

    /// What can appear after "\[player\]? reveal\[s\]" — either the hand
    /// zone's contents or any other [Subject]. Mirrors [#DRAW_AMOUNT]
    /// / [#DISCARD_WHAT] in shape.
    private static final Parser<Subject> REVEAL_WHAT = anyOf(HAND, SubjectParsers.SUBJECT);

    static final Parser<Subject> REVEAL_NO_PLAYER =
            DamageEffectParsers.each(phrase("Reveal(s)")).then(REVEAL_WHAT);

    /// "reveal[s] a card at random from [poss] hand" — the at-random
    /// single-card form (Hired Torturer). Returns the [Subject] for one
    /// card in the hand zone; [EffectParsers] uses this to wire the actor
    /// in chain contexts ([EffectParsers#PLAYER_VERB_BODY]).
    static final Parser<Subject> REVEAL_AT_RANDOM_NO_PLAYER = DamageEffectParsers.each(phrase("Reveal(s)"))
            .followedBy(phrase("a card at random from [your|their|his|her|its] hand"))
            .thenReturn(AT_RANDOM_CARD_FROM_HAND);

    static final Parser<Effect.Reveal> REVEAL = anyOf(
                    // at-random: "[player] reveals a card at random from [poss] hand"
                    sequence(SubjectParsers.PLAYER_SUBJECTS, REVEAL_AT_RANDOM_NO_PLAYER, Effect.Reveal::new)
                            .map(Effect.Reveal::withAtRandom),
                    // at-random: subjectless "reveals a card at random from [poss] hand"
                    REVEAL_AT_RANDOM_NO_PLAYER.map(what -> new Effect.Reveal(YOU, what).withAtRandom()),
                    // standard form
                    sequence(SubjectParsers.PLAYER_SUBJECTS, REVEAL_NO_PLAYER, Effect.Reveal::new),
                    REVEAL_NO_PLAYER.map(what -> new Effect.Reveal(YOU, what)))
            // "as you draft it" — Conspiracy draft-action timing
            // (Cogwork Spy). Not stored; the timing is inherent to the
            // draft context where the ability fires.
            .optionallyFollowedBy(phrase("as you draft it"), (r, _) -> r);
}
