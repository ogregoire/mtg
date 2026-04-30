package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PLURAL_ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Map;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.*;

/// Leaf-effect parsers for removal verbs: destroy, exile, sacrifice,
/// and return-to-hand ("bounce"). Each field produces a single
/// [Effect] variant and is consumed directly by
/// [EffectParsers#BASE_EFFECT] / [EffectParsers#MAY].
final class RemovalEffectParsers {
    private RemovalEffectParsers() {}

    private static final Subject YOU = Subject.player(PlayerRef.Pronoun.YOU);

    /// "At [timing], …" phrasings used as trailing scheduler suffixes on
    /// actions like [#DESTROY] (e.g., Silent Assassin: "Destroy
    /// target blocking creature at end of combat.").
    private static final Parser<Duration> AT_TIMING = anyOf(
            phrase("at end of combat").thenReturn(Duration.Fixed.UNTIL_END_OF_COMBAT),
            phrase("at end of turn").thenReturn(Duration.Fixed.UNTIL_END_OF_TURN));

    // ── Destroy ───────────────────────────────────────────────────────

    static final Parser<Effect.Destroy> DESTROY = phrase("Destroy")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.Destroy::new)
            .optionallyFollowedBy(phrase("at random"), (e, _) -> e.withAtRandom())
            .optionallyFollowedBy(AT_TIMING, Effect.Destroy::withAt);

    // ── Exile ─────────────────────────────────────────────────────────

    /// What follows the verb `exile`: either cards selected by a subject,
    /// the contents of one specific player's zone ("target player's
    /// graveyard"), or the contents of every player's zone ("all
    /// graveyards").
    private static final Parser<Exiled> EXILED = anyOf(
            phrase("all").then(PLURAL_ZONE_NAME).<Exiled>map(Exiled.Zones::new),
            sequence(SubjectParsers.PLAYER_REF.followedBy(string("'s")), ZONE_NAME, (ref, zone) ->
                    (Exiled) new Exiled.PlayerZone(ref, zone)),
            SubjectParsers.SUBJECT.<Exiled>map(Exiled.Objects::new));

    /// Possessive-prefixed zone reference — "their graveyard" / "your
    /// hand" resolved to a [Exiled.PlayerZone] keyed on the
    /// pronominal player ref. Used by the two-target exile form.
    private static final Parser<Exiled> POSSESSIVE_EXILED_ZONE = sequence(
            anyOf(
                    phrase("their").thenReturn(PlayerRef.Pronoun.THEY),
                    phrase("your").thenReturn(PlayerRef.Pronoun.YOU),
                    phrase("its").thenReturn(PlayerRef.Pronoun.THAT_PLAYER)),
            ZONE_NAME,
            (ref, zone) -> (Exiled) new Exiled.PlayerZone(ref, zone));

    /// Exile head: a plain imperative "exile" (actor null) or a player
    /// subject followed by "exiles" (the parsed player becomes the
    /// [Effect.Exile#actor()]). Mudhole: "Target player exiles all
    /// land cards from their graveyard.".
    private static final Parser<@Nullable Subject> EXILE_HEAD = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("exile(s)")),
            phrase("Exile").map(_ -> (Subject) null));

    static final Parser<Effect.Exile> EXILE = anyOf(
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    ZoneExpressionParsers.MULTI_ZONE_FROM,
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            // "exile [exiled] from all [zone] and [zone]" — bulk two-zone form
            // (Worldfire: "Exile all cards from all hands and graveyards.").
            // Must precede the single-plural IN_ZONE_FROM arm so "from all hands and"
            // doesn't get consumed as "from all hands" leaving "and graveyards" unmatched.
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    ZoneExpressionParsers.ALL_ZONES_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    ZoneExpressionParsers.PLAYER_ZONE_FROM,
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            sequence(
                    EXILE_HEAD,
                    EXILED,
                    ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    (actor, exiled, from) -> new Effect.Exile(exiled, from, actor)),
            sequence(EXILE_HEAD, EXILED, (actor, exiled) -> new Effect.Exile(exiled, null, actor)));

    /// "[player] exiles [object] and [poss] [zone]." — Strategic
    /// Betrayal: "Target opponent exiles a creature they control and
    /// their graveyard." Emits two [Effect.Exile] effects sharing
    /// the actor; the pair is flattened by [EffectParsers#CLAUSE] into
    /// the surrounding effect list.
    static final Parser<List<Effect>> EXILE_OBJECT_AND_ZONE = sequence(
            EXILE_HEAD,
            SubjectParsers.SUBJECT.followedBy(word("and")),
            POSSESSIVE_EXILED_ZONE,
            (actor, subj, zone) -> List.of(
                    new Effect.Exile(new Exiled.Objects(subj), null, actor), new Effect.Exile(zone, null, actor)));

    // ── Sacrifice ─────────────────────────────────────────────────────

    /// Optional trailing "of [possessive] choice" clause on a sacrifice
    /// (e.g., Tremble: "Each player sacrifices a land of their choice.").
    /// Consumed as flavor — the grammar doesn't yet model the chooser.
    private static final Parser<String> OF_CHOICE = phrase("of [your|their|his|her|its] choice");

    /// What follows `sacrifice[s]` — a full [Subject] so selectors
    /// ("a creature you control") and self-references ("this creature",
    /// Barbarian Outcast) both parse.
    private static final Parser<Subject> SACRIFICE_NO_PLAYER =
            phrase("Sacrifice(s)").then(SubjectParsers.SUBJECT).optionallyFollowedBy(OF_CHOICE, (sel, _) -> sel);

    static final Parser<Effect.Sacrifice> SACRIFICE = anyOf(
                    // PLAYER_LIKE_SUBJECT also matches possessives like "its
                    // controller" — Killing Wave: "its controller sacrifices
                    // it unless they pay X life.".
                    sequence(SubjectParsers.PLAYER_LIKE_SUBJECT, SACRIFICE_NO_PLAYER, Effect.Sacrifice::new),
                    SACRIFICE_NO_PLAYER.map(what -> new Effect.Sacrifice(YOU, what)))
            .optionallyFollowedBy(AT_TIMING, Effect.Sacrifice::withAt);

    /// [#SACRIFICE] with an optional trailing "for each …" scaling
    /// suffix (Thoughts of Ruin: "Each player sacrifices a land of
    /// their choice for each card in your hand."). Registered in the
    /// effect dispatcher in place of the bare SACRIFICE so the scaled
    /// form is always considered.
    static final Parser<Effect.Sacrifice> SACRIFICE_WITH_SCALE =
            SACRIFICE.optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.Sacrifice::withScaleBy);

    // ── Bounce (return to hand) ───────────────────────────────────────

    /// Subject following "return", optionally tagged "at random" — the
    /// random-selection flag is consumed as flavor (e.g., Make a Wish:
    /// "Return two cards at random from your graveyard to your hand.").
    private static final Parser<Subject> RETURN_SUBJECT =
            SubjectParsers.SUBJECT.optionallyFollowedBy(phrase("at random"), (s, _) -> s);

    /// "meld \[subject\] into \<melded-name\>." — meld action (rule
    /// 701.39, Eldritch Moon: Gisela, the Broken Blade / Bruna, the
    /// Fading Light → Brisela, Voice of Nightmares). The melded name
    /// is a literal printed card name captured via
    /// [CardNameParsers#CARD_NAME] so legendary epithets with commas
    /// land verbatim.
    static final Parser<Effect.Meld> MELD = sequence(
            phrase("meld").then(SubjectParsers.SUBJECT).followedBy(phrase("into")),
            CardNameParsers.CARD_NAME,
            Effect.Meld::new);

    /// "Return [subject] [from [zone]]? [to destination]." — the optional
    /// source zone (e.g., Auroral Procession: "… from your graveyard …")
    /// is captured structurally; most bounces omit it and it stays null.
    /// Tried with-from first so the optional arm doesn't shadow it.
    static final Parser<Effect.Bounce> BOUNCE = Parser.<Effect.Bounce>anyOf(
                    sequence(
                            phrase("Return").then(RETURN_SUBJECT),
                            ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                            ZoneParsers.ZONE_DESTINATION,
                            Effect.Bounce::new),
                    sequence(
                            phrase("Return").then(RETURN_SUBJECT),
                            ZoneParsers.ZONE_DESTINATION,
                            (subject, dest) -> new Effect.Bounce(subject, null, dest)),
                    // "Return to [destination] [subject]" — destination-first
                    // inversion (Shadow of the Grave: "Return to your hand all
                    // cards in your graveyard that you cycled or discarded this
                    // turn."). Emits the same Bounce record with the canonical
                    // subject/destination ordering.
                    sequence(
                            phrase("Return").then(ZoneParsers.ZONE_DESTINATION),
                            RETURN_SUBJECT,
                            (dest, subject) -> new Effect.Bounce(subject, null, dest)),
                    // "[player] returns [subject] [from [zone]]? to [zone]." —
                    // player-actor form (Curfew: "Each player returns a creature
                    // they control to its owner's hand."; Empty the Catacombs:
                    // "Each player returns all creature cards from their
                    // graveyard to their hand."). The player actor is flavor
                    // in the current Bounce model; only the subject + source +
                    // destination are preserved.
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT
                                    .followedBy(phrase("return(s)"))
                                    .then(RETURN_SUBJECT),
                            ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                            ZoneParsers.ZONE_DESTINATION,
                            Effect.Bounce::new),
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT
                                    .followedBy(phrase("return(s)"))
                                    .then(RETURN_SUBJECT),
                            ZoneParsers.ZONE_DESTINATION,
                            (subject, dest) -> new Effect.Bounce(subject, null, dest)))
            // Trailing ", rounded up/down" on a half-quantified target
            // (Split the Party: "Return half the creatures they control
            // to their owner's hand, rounded up."). Folds the rounding
            // onto the [Subject.HalfOf] target so downstream code sees
            // the rounding mode without a separate side-channel.
            .optionallyFollowedBy(
                    CountOfParsers.ROUNDING_DIRECTION,
                    (b, r) -> b.target() instanceof Subject.HalfOf h
                            ? new Effect.Bounce(h.withRounding(r), b.from(), b.to())
                            : b)
            // "with [a|an]? [count] [type] counter[s] on it" — the returned
            // permanent enters with counters (Unbreakable Bond: "with a
            // lifelink counter on it"; Persist: "with a -1/-1 counter on it").
            .optionallyFollowedBy(
                    phrase("with")
                            .then(sequence(
                                    AMOUNT, COUNTER_TYPE.followedBy(phrase("counter(s) on [it|them]")), Map::entry)),
                    (b, e) -> b.withEnterCounter(e.getKey(), e.getValue()))
            // "except for Krakens, Leviathans, Octopuses, and Serpents" — trailing
            // exclusion clause on a mass bounce (Whelming Wave). The listed creature
            // types are stored structurally on [Effect.Bounce#except] so the engine
            // can skip those permanents instead of returning them.
            .optionallyFollowedBy(phrase("except for").then(MtgParsers.andList(SUBTYPE)), Effect.Bounce::withExcept);
}
