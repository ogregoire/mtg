package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Structured parsers for {@link TriggerEvent}. Replaces the prior
/// free-text event capture used by {@link OracleParser#TRIGGERED}. Each
/// arm recognizes a specific oracle-text shape; unknown shapes now fail
/// the parse rather than being swallowed by a catch-all.
final class TriggerEventParsers {
    private TriggerEventParsers() {}

    // ── Combat-side verbs ─────────────────────────────────────────────

    private static final Parser<TriggerEvent> ENTERS = SubjectParsers.SUBJECT
            .followedBy(phrase("enter(s)"))
            .map(TriggerEvent.Enters::new)
            .optionallyFollowedBy(word("tapped"), (ev, _) -> ev.withTapped())
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> DIES =
            SubjectParsers.SUBJECT.followedBy(phrase("die(s)")).map(TriggerEvent.Dies::new);

    /// "[subject] enters or dies" — combined enter/leave trigger sharing
    /// the subject (Ashen Rider: "When this creature enters or dies, exile
    /// target permanent."). Yields an {@link TriggerEvent.Or} of
    /// Enters+Dies so downstream dispatch can handle either.
    private static final Parser<TriggerEvent> ENTERS_OR_DIES = SubjectParsers.SUBJECT
            .followedBy(words("enters or dies"))
            .map(s -> new TriggerEvent.Or(List.of(new TriggerEvent.Enters(s), new TriggerEvent.Dies(s))));

    private static final Parser<TriggerEvent> ATTACKS = SubjectParsers.SUBJECT
            .followedBy(phrase("attack(s)"))
            .map(TriggerEvent.Attacks::new)
            .optionallyFollowedBy(SubjectParsers.PLAYER_SUBJECT, TriggerEvent.Attacks::withTarget)
            .optionallyFollowedBy(word("alone"), (ev, _) -> ev.attackingAlone())
            .map(x -> x); // widen for typing

    /// "[subject] attacks or blocks" — combined combat trigger sharing the
    /// attacker/blocker subject (common on "sacrifice at end of combat"
    /// cards). Yields an {@link TriggerEvent.Or} of Attacks+Blocks.
    private static final Parser<TriggerEvent> ATTACKS_OR_BLOCKS = SubjectParsers.SUBJECT
            .followedBy(words("attacks or blocks"))
            .map(s -> new TriggerEvent.Or(List.of(new TriggerEvent.Attacks(s), new TriggerEvent.Blocks(s))));

    private static final Parser<TriggerEvent> BLOCKS = SubjectParsers.SUBJECT
            .followedBy(phrase("block(s)"))
            .map(TriggerEvent.Blocks::new)
            .optionallyFollowedBy(SubjectParsers.SUBJECT, TriggerEvent.Blocks::withTarget)
            .map(x -> x); // widen for typing

    /// "[subject] becomes blocked [by X]?" — distinct from {@link #BLOCKS}.
    private static final Parser<TriggerEvent> BECOMES_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("become(s) blocked"))
            .map(TriggerEvent.BecomesBlocked::new)
            .optionallyFollowedBy(word("by").then(SubjectParsers.SUBJECT), TriggerEvent.BecomesBlocked::withBy)
            .map(x -> x); // widen for typing

    /// "[subject] blocks or becomes blocked [by X]?" — combined trigger.
    private static final Parser<TriggerEvent> BLOCKS_OR_BECOMES_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(words("blocks or becomes blocked"))
            .map(s -> new TriggerEvent.Or(List.of(new TriggerEvent.Blocks(s), new TriggerEvent.BecomesBlocked(s))))
            .optionallyFollowedBy(
                    word("by").then(SubjectParsers.SUBJECT),
                    (or, by) -> new TriggerEvent.Or(List.of(
                            or.events().getFirst(),
                            ((TriggerEvent.BecomesBlocked) or.events().get(1)).withBy(by))))
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> BECOMES_TAPPED =
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) tapped")).map(TriggerEvent::becomesTapped);

    private static final Parser<TriggerEvent> BECOMES_UNTAPPED =
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) untapped")).map(TriggerEvent::becomesUntapped);

    private static final Parser<TriggerEvent> BECOMES_TARGET_OF = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) the target of")),
            SelectorParsers.SELECTOR,
            TriggerEvent.BecomesTargetOf::new);

    // ── Damage verbs ──────────────────────────────────────────────────

    /// "[source] deals [combat]? damage [to [target]]?".
    private static final Parser<TriggerEvent> DEALS_DAMAGE = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("deal(s)")),
                    anyOf(
                            phrase("combat damage").thenReturn(true),
                            word("damage").thenReturn(false)),
                    TriggerEvent.DealsDamage::new)
            .optionallyFollowedBy(word("to").then(SubjectParsers.SUBJECT), TriggerEvent.DealsDamage::withTarget)
            .map(x -> x); // widen for typing

    /// "[subject] is [combat]? dealt damage" — the passive-voice form
    /// (e.g., Dromad Purebred: "Whenever this creature is dealt damage, …").
    private static final Parser<TriggerEvent> IS_DEALT_DAMAGE = sequence(
            SubjectParsers.SUBJECT,
            anyOf(
                    phrase("[is|are] dealt combat damage").thenReturn(true),
                    phrase("[is|are] dealt damage").thenReturn(false)),
            TriggerEvent.IsDealtDamage::new);

    // ── "is cast"/"is countered"/"is put into" ────────────────────────

    private static final Parser<TriggerEvent> IS_CAST =
            SubjectParsers.SUBJECT.followedBy(phrase("is cast")).map(TriggerEvent.IsCast::new);

    private static final Parser<TriggerEvent> IS_COUNTERED =
            SubjectParsers.SUBJECT.followedBy(phrase("is countered")).map(TriggerEvent.IsCountered::new);

    /// "[subject] is put into [zone source]".
    private static final Parser<TriggerEvent> IS_PUT_INTO = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("is put into")),
            ZoneParsers.ZONE_SOURCE,
            TriggerEvent.PutInto::new);

    /// "one or more <subject> leave [zone]".
    private static final Parser<TriggerEvent> LEAVES =
            sequence(SubjectParsers.SUBJECT.followedBy(phrase("leave(s)")), ZoneParsers.ZONE, TriggerEvent.Leaves::new);

    // ── Player verbs ──────────────────────────────────────────────────

    private static final Parser<TriggerEvent> PLAYER_CASTS = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cast(s)")),
                    SelectorParsers.SELECTOR,
                    TriggerEvent.PlayerCasts::new)
            // "from [zone-source]" — Secrets of the Dead: "from your
            // graveyard". Restricts the trigger to casts originating in
            // the named zone (hand vs. flashback vs. suspend exile).
            .optionallyFollowedBy(ZoneParsers.ZONE_SOURCE, TriggerEvent.PlayerCasts::withFrom)
            // "this turn" — Glimpse-of-Nature-style temporal scope.
            .optionallyFollowedBy(phrase("this turn"), (ev, _) -> ev.scopedToThisTurn())
            .map(x -> x); // widen for typing

    /// "[player] cast[s] [your|their] [first|second|...] spell each turn" —
    /// PlayerCasts specialization that fires only on the n-th spell each
    /// turn (Rodeo Pyromancers, Glimpse of Nature-style cards).
    private static final Parser<Integer> SPELL_ORDINAL = anyOf(
            word("first").thenReturn(1),
            word("second").thenReturn(2),
            word("third").thenReturn(3),
            word("fourth").thenReturn(4));

    private static final Selector ANY_SPELL = new Selector(
            Selector.Quantifier.one(),
            Selector.TypeExpression.single(Selector.SingleType.ofGameObject(GameObjectType.SPELL)));

    private static final Parser<TriggerEvent> PLAYER_CASTS_NTH = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cast(s) [your|their]")),
                    SPELL_ORDINAL.followedBy(phrase("spell(s)")),
                    (player, nth) -> new TriggerEvent.PlayerCasts(player, ANY_SPELL).nth(nth))
            .followedBy(words("each turn"))
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> PLAYER_CYCLES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cycle(s)")),
            SelectorParsers.SELECTOR,
            TriggerEvent.PlayerCycles::new);

    private static final Parser<TriggerEvent> PLAYER_DISCARDS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("discard(s)")),
            SelectorParsers.SELECTOR,
            TriggerEvent.PlayerDiscards::new);

    /// "[subject] is turned face up" — morph/manifest flip trigger.
    private static final Parser<TriggerEvent> IS_TURNED_FACE_UP =
            SubjectParsers.SUBJECT.followedBy(words("is turned face up")).map(TriggerEvent.IsTurnedFaceUp::new);

    /// "[subject] mutates" — mutate trigger (Ikoria).
    private static final Parser<TriggerEvent> MUTATES =
            SubjectParsers.SUBJECT.followedBy(phrase("mutate(s)")).map(TriggerEvent.Mutates::new);

    /// "[player] give[s] a gift" — Aetherdrift Gifts trigger (Jolly
    /// Gerbils).
    private static final Parser<TriggerEvent> PLAYER_GIVES_GIFT =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("give(s) a gift")).map(TriggerEvent.PlayerGivesGift::new);

    /// "[player] attack[s] with [amount] creature(s)" — Raiding Horde.
    private static final Parser<TriggerEvent> ATTACKS_WITH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("attack(s) with")),
            SelectorParsers.AMOUNT.followedBy(phrase("creature(s)")),
            TriggerEvent.AttacksWith::new);

    /// "[player] control[s] no [selector]" — state-condition trigger.
    private static final Parser<TriggerEvent> CONTROLS_NONE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("control(s) no")),
            SelectorParsers.SELECTOR,
            TriggerEvent.ControlsNone::new);

    /// "[player] play[s] [selector]" — generic land/card-play trigger
    /// (e.g., "When you play another land"). Distinct from
    /// {@link #PLAYER_PLAYS_LAND} because the selector carries qualifiers.
    private static final Parser<TriggerEvent> PLAYER_PLAYS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("play(s)")),
            SelectorParsers.SELECTOR,
            TriggerEvent.PlayerPlays::new);

    private static final Parser<TriggerEvent> PLAYER_DRAWS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("draw(s)")),
            SelectorParsers.AMOUNT.followedBy(phrase("card(s)")),
            TriggerEvent.PlayerDraws::new);

    private static final Parser<TriggerEvent> PLAYER_GAINS_LIFE =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("gain(s) life")).map(TriggerEvent.PlayerGainsLife::new);

    private static final Parser<TriggerEvent> PLAYER_LOSES_LIFE =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("lose(s) life")).map(TriggerEvent.PlayerLosesLife::new);

    private static final Parser<TriggerEvent> PLAYER_PLAYS_LAND = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("play(s) [a|an] land"))
            .map(TriggerEvent.PlayerPlaysLand::new);

    private static final Parser<TriggerEvent> PLAYER_SACRIFICES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("sacrifice(s)")),
            SelectorParsers.SELECTOR,
            TriggerEvent.PlayerSacrifices::new);

    private static final Parser<TriggerEvent> TAPS_FOR_MANA = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("tap(s)")),
            SelectorParsers.SELECTOR.followedBy(words("for mana")),
            TriggerEvent.TapsForMana::new);

    // ── At-the-beginning-of-step/phase ────────────────────────────────

    /// Step names (optional "step" suffix) used as trigger anchors.
    private static final Parser<Step> STEP_NAME = anyOf(
            phrase("beginning of combat").thenReturn(Step.BEGINNING_OF_COMBAT),
            phrase("declare attackers").thenReturn(Step.DECLARE_ATTACKERS),
            phrase("declare blockers").thenReturn(Step.DECLARE_BLOCKERS),
            phrase("combat damage").thenReturn(Step.COMBAT_DAMAGE),
            phrase("end of combat").thenReturn(Step.END_OF_COMBAT),
            phrase("end step").thenReturn(Step.END),
            phrase("cleanup step").thenReturn(Step.CLEANUP),
            phrase("draw step").thenReturn(Step.DRAW),
            phrase("upkeep step").thenReturn(Step.UPKEEP),
            phrase("untap step").thenReturn(Step.UNTAP),
            word("upkeep").thenReturn(Step.UPKEEP),
            word("end").thenReturn(Step.END),
            word("cleanup").thenReturn(Step.CLEANUP),
            word("draw").thenReturn(Step.DRAW),
            word("untap").thenReturn(Step.UNTAP));

    /// A phase reference, always produced as an {@link TriggerEvent.AtPhase}
    /// so the twin-main-phase qualifier (first/second/precombat/postcombat)
    /// is preserved for the rules engine. {@link Phase#MAIN} represents
    /// both mains; the qualifier disambiguates.
    private static final Parser<TriggerEvent.PhaseQualifier> MAIN_PHASE_QUALIFIER = anyOf(
            word("first").thenReturn(TriggerEvent.PhaseQualifier.FIRST),
            word("second").thenReturn(TriggerEvent.PhaseQualifier.SECOND),
            word("precombat").thenReturn(TriggerEvent.PhaseQualifier.PRECOMBAT),
            word("postcombat").thenReturn(TriggerEvent.PhaseQualifier.POSTCOMBAT));

    private static final Parser<TriggerEvent.AtPhase> PHASE_NAME = anyOf(
                    sequence(
                            MAIN_PHASE_QUALIFIER,
                            word("main"),
                            (qualifier, _) -> new TriggerEvent.AtPhase(Phase.MAIN, qualifier)),
                    word("beginning").thenReturn(new TriggerEvent.AtPhase(Phase.BEGINNING)),
                    word("main").thenReturn(new TriggerEvent.AtPhase(Phase.MAIN)),
                    word("combat").thenReturn(new TriggerEvent.AtPhase(Phase.COMBAT)),
                    word("ending").thenReturn(new TriggerEvent.AtPhase(Phase.ENDING)))
            .followedBy(word("phase"));

    /// "[possessive]? [each]? [step-name] step" owner marker used in
    /// "at the beginning of …" triggers. {@code each} is true when oracle
    /// text reads "each [step]" (applies to every player's version).
    private record StepOwner(@Nullable Subject owner, boolean each) {}

    private static final Parser<StepOwner> STEP_OWNER = anyOf(
            word("your").thenReturn(new StepOwner(Subject.player(Subject.PlayerRef.YOU), false)),
            phrase("each [player's|players]").thenReturn(new StepOwner(null, true)),
            word("each").thenReturn(new StepOwner(null, true)));

    private static final Parser<TriggerEvent> AT_BEGINNING_OF = phrase("the beginning of")
            .then(sequence(
                    STEP_OWNER,
                    Parser.<TriggerEvent.OwnerScoped>anyOf(STEP_NAME.map(TriggerEvent.AtStep::new), PHASE_NAME),
                    (owner, event) -> event.withOwner(owner.owner(), owner.each())))
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> AT_END_OF_COMBAT =
            phrase("end of combat").thenReturn(TriggerEvent.EndOfCombat.END_OF_COMBAT);

    private static final Parser<TriggerEvent> AT_END_OF_TURN =
            phrase("end of turn").thenReturn(TriggerEvent.EndOfTurn.END_OF_TURN);

    // ── Dispatcher ────────────────────────────────────────────────────

    /// Atomic (non-composable) trigger events — longest-match ordering.
    private static final Parser<TriggerEvent> ATOMIC = anyOf(
            // "at the beginning of …" — only meaningful for "at" triggers
            AT_BEGINNING_OF,
            AT_END_OF_COMBAT,
            AT_END_OF_TURN,
            // Multi-verb shapes must precede single verbs.
            ATTACKS_OR_BLOCKS,
            BLOCKS_OR_BECOMES_BLOCKED,
            // Combat state changes.
            BECOMES_BLOCKED,
            BECOMES_TAPPED,
            BECOMES_UNTAPPED,
            BECOMES_TARGET_OF,
            // Attack formation must precede ATTACKS so "attack with two"
            // isn't swallowed as a bare "attack" + stray "with".
            ATTACKS_WITH,
            // Single combat verbs.
            ATTACKS,
            BLOCKS,
            // Damage and stack events.
            DEALS_DAMAGE,
            IS_DEALT_DAMAGE,
            IS_CAST,
            IS_COUNTERED,
            IS_PUT_INTO,
            IS_TURNED_FACE_UP,
            LEAVES,
            MUTATES,
            // Player actions — must precede ENTERS because PLAYER_SUBJECT
            // has narrower overlap with SUBJECT (e.g., "you").
            CONTROLS_NONE,
            PLAYER_CASTS_NTH, // must precede PLAYER_CASTS (longer prefix)
            PLAYER_CASTS,
            PLAYER_CYCLES,
            PLAYER_DISCARDS,
            PLAYER_DRAWS,
            PLAYER_GAINS_LIFE,
            PLAYER_GIVES_GIFT,
            PLAYER_LOSES_LIFE,
            PLAYER_PLAYS_LAND, // must precede PLAYER_PLAYS (longer match)
            PLAYER_PLAYS,
            PLAYER_SACRIFICES,
            TAPS_FOR_MANA,
            // Default object verbs.
            ENTERS_OR_DIES, // must precede ENTERS (shares "[subject] enters" prefix)
            ENTERS,
            DIES);

    /// A single trigger event, possibly a composition of atomic events
    /// joined by "or" (e.g., Raging Ravine: "… or becomes blocked").
    public static final Parser<TriggerEvent> TRIGGER_EVENT =
            ATOMIC.optionallyFollowedBy(word("or").then(ATOMIC), TriggerEventParsers::joinOr);

    private static TriggerEvent joinOr(TriggerEvent first, TriggerEvent next) {
        if (first instanceof TriggerEvent.Or(var events)) {
            var all = new ArrayList<>(events);
            all.add(next);
            return new TriggerEvent.Or(List.copyOf(all));
        }
        return new TriggerEvent.Or(List.of(first, next));
    }
}
