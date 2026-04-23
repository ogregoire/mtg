package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.GameObjectType;
import be.imgn.mtg.engine.oracle.domain.Selector;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.TriggerEvent;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Structured parsers for [TriggerEvent]. Replaces the prior
/// free-text event capture used by [OracleParser#TRIGGERED]. Each
/// arm recognizes a specific oracle-text shape; unknown shapes now fail
/// the parse rather than being swallowed by a catch-all.
final class TriggerEventParsers {
    private TriggerEventParsers() {}

    // ── Combat-side verbs ─────────────────────────────────────────────

    private static final Parser<TriggerEvent> ENTERS = SubjectParsers.SUBJECT
            .followedBy(phrase("enter(s)"))
            .map(TriggerEvent.Enters::new)
            .optionallyFollowedBy(word("tapped"), (ev, _) -> ev.withTapped())
            // Optional temporal scope — "enters during your turn"
            // (Foe-liage). Absorbed as a flag on the event.
            .optionallyFollowedBy(phrase("during your turn"), (ev, _) -> ev.asDuringYourTurn())
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> DIES = SubjectParsers.SUBJECT
            .followedBy(phrase("die(s)"))
            .map(TriggerEvent.Dies::new)
            .optionallyFollowedBy(phrase("during combat"), (ev, _) -> ev.asDuringCombat())
            .map(x -> x);

    /// "[subject] enters or dies" — combined enter/leave trigger sharing
    /// the subject (Ashen Rider: "When this creature enters or dies, exile
    /// target permanent."). Yields two peer events so one triggered
    /// ability is emitted per event.
    private static final Parser<List<TriggerEvent>> ENTERS_OR_DIES = SubjectParsers.SUBJECT
            .followedBy(phrase("enters or dies"))
            .map(s -> List.of(new TriggerEvent.Enters(s), new TriggerEvent.Dies(s)));

    private static final Parser<TriggerEvent> ATTACKS = SubjectParsers.SUBJECT
            .followedBy(phrase("attack(s)"))
            .map(TriggerEvent.Attacks::new)
            // Attack target is any Subject — rule 508.1a allows a
            // player, planeswalker, or battle (Thrashing Frontliner:
            // "attacks a battle"). SUBJECT already tries the PLAYER
            // forms first via ATOMIC_SUBJECT, so "attacks you" still
            // lands on Subject.Player.
            .optionallyFollowedBy(SubjectParsers.SUBJECT, TriggerEvent.Attacks::withTarget)
            .optionallyFollowedBy(word("alone"), (ev, _) -> ev.attackingAlone())
            .map(x -> x); // widen for typing

    /// "[subject] attacks and isn't blocked" — compound combat trigger
    /// (Abyssal Nightstalker). Must precede [#ATTACKS] since both
    /// share the "[subject] attacks" prefix; the "and isn't blocked"
    /// tail is the distinguishing suffix.
    private static final Parser<TriggerEvent.AttacksUnblocked> ATTACKS_UNBLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("attack(s) and [isn't|aren't] blocked"))
            .map(TriggerEvent.AttacksUnblocked::new);

    /// "[subject] attacks or blocks" — combined combat trigger sharing the
    /// attacker/blocker subject (common on "sacrifice at end of combat"
    /// cards). Yields two peer events so one triggered ability is emitted
    /// per event.
    private static final Parser<List<TriggerEvent>> ATTACKS_OR_BLOCKS = SubjectParsers.SUBJECT
            .followedBy(phrase("attacks or blocks"))
            .map(s -> List.of(new TriggerEvent.Attacks(s), new TriggerEvent.Blocks(s)));

    private static final Parser<TriggerEvent> BLOCKS = SubjectParsers.SUBJECT
            .followedBy(phrase("block(s)"))
            .map(TriggerEvent.Blocks::new)
            .optionallyFollowedBy(SubjectParsers.SUBJECT, TriggerEvent.Blocks::withTarget)
            .map(x -> x); // widen for typing

    /// "[subject] becomes blocked [by X]?" — distinct from [#BLOCKS].
    private static final Parser<TriggerEvent> BECOMES_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("become(s) blocked"))
            .map(TriggerEvent.BecomesBlocked::new)
            .optionallyFollowedBy(word("by").then(SubjectParsers.SUBJECT), TriggerEvent.BecomesBlocked::withBy)
            .map(x -> x); // widen for typing

    /// "[subject] blocks or becomes blocked [by X]?" — combined trigger.
    /// Yields two peer events (Blocks + BecomesBlocked) so one triggered
    /// ability is emitted per event.
    private static final Parser<List<TriggerEvent>> BLOCKS_OR_BECOMES_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("blocks or becomes blocked"))
            .<List<TriggerEvent>>map(s -> List.of(new TriggerEvent.Blocks(s), new TriggerEvent.BecomesBlocked(s)))
            .optionallyFollowedBy(
                    word("by").then(SubjectParsers.SUBJECT),
                    (events, by) ->
                            List.of(events.getFirst(), ((TriggerEvent.BecomesBlocked) events.get(1)).withBy(by)));

    private static final Parser<TriggerEvent> BECOMES_TAPPED =
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) tapped")).map(TriggerEvent::becomesTapped);

    private static final Parser<TriggerEvent> BECOMES_UNTAPPED =
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) untapped")).map(TriggerEvent::becomesUntapped);

    private static final Parser<TriggerEvent> BECOMES_TARGET_OF = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("become(s) the target of")),
            SELECTOR,
            TriggerEvent.BecomesTargetOf::new);

    // ── Damage verbs ──────────────────────────────────────────────────

    /// "[source] deals [combat]? damage [to [target]]?".
    private static final Parser<TriggerEvent> DEALS_DAMAGE = Parser.sequence(
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

    /// "[subject] is put into [destination] [from [source]]?".
    /// The destination-first form (Planar Void: "is put into a graveyard
    /// from anywhere") carries an optional "from <source>" tail; the
    /// legacy direct-source form ("is put into from X") is preserved
    /// via the [ZoneParsers#ZONE_SOURCE] arm.
    private static final Parser<TriggerEvent> IS_PUT_INTO = SubjectParsers.SUBJECT
            .followedBy(phrase("is put into"))
            .flatMap(subj -> Parser.<TriggerEvent>anyOf(
                    ZoneParsers.ZONE
                            .<TriggerEvent>map(dest -> new TriggerEvent.PutInto(subj, dest))
                            .optionallyFollowedBy(
                                    ZoneParsers.ZONE_SOURCE,
                                    (ev, src) -> new TriggerEvent.PutInto(
                                            subj, ((TriggerEvent.PutInto) ev).destination(), src)),
                    ZoneParsers.ZONE_SOURCE.map(src -> new TriggerEvent.PutInto(subj, src))));

    /// "one or more <subject> leave [zone]".
    private static final Parser<TriggerEvent> LEAVES =
            sequence(SubjectParsers.SUBJECT.followedBy(phrase("leave(s)")), ZoneParsers.ZONE, TriggerEvent.Leaves::new);

    /// "[subject] is returned to [zone]" — bounce-style zone change
    /// (Warped Devotion: "Whenever a permanent is returned to a
    /// player's hand, …").
    private static final Parser<TriggerEvent> IS_RETURNED_TO = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("is returned to")),
            ZoneParsers.ZONE,
            TriggerEvent.IsReturnedTo::new);

    /// "[player] roll[s] [amount] dice" — dice-rolling trigger
    /// (Brazen Dwarf: "Whenever you roll one or more dice, …"). Rule
    /// 706.2.
    private static final Parser<TriggerEvent> PLAYER_ROLLS_DICE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("roll(s)")),
            AMOUNT.followedBy(phrase("dice")),
            TriggerEvent.PlayerRollsDice::new);

    // ── Player verbs ──────────────────────────────────────────────────

    /// "[player] cast[s] this spell/~" — cast-self trigger (Desolation
    /// Twin). Must precede [#PLAYER_CASTS] so the self-reference
    /// wins over a bare selector match.
    private static final Parser<TriggerEvent> PLAYER_CASTS_SELF = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cast(s)")),
            anyOf(phrase("this spell"), string("~")),
            (player, _) -> (TriggerEvent) new TriggerEvent.PlayerCastsSelf(player));

    /// "[player] proliferate[s]" — proliferate trigger (Scheming
    /// Aspirant: "Whenever you proliferate, …"). Rule 701.25.
    private static final Parser<TriggerEvent> PLAYER_PROLIFERATES = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("proliferate(s)"))
            .map(TriggerEvent.PlayerProliferates::new);

    /// "[player] activate[s] a [kind] ability" — ability-activation
    /// trigger (Frenzied Raider: "Whenever you activate a boast
    /// ability …"). The kind word before "ability" names the tagged
    /// ability family.
    private static final Parser<TriggerEvent> PLAYER_ACTIVATES_ABILITY = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("activate(s) a")),
            word().followedBy(word("ability")),
            TriggerEvent.PlayerActivatesAbility::new);

    /// Factory for an object-free player-verb trigger: consumes the verb
    /// token and yields a constructor that binds the shared subject
    /// parsed up front. Enables generic shared-subject disjunctions like
    /// "you scry or surveil" without a dedicated combo parser (Matoya,
    /// Archon Elder).
    private static final Parser<Function<Subject, TriggerEvent>> SCRIES_VERB = anyOf(phrase("scries"), phrase("scry"))
            .<Function<Subject, TriggerEvent>>thenReturn(TriggerEvent.PlayerScries::new);

    private static final Parser<Function<Subject, TriggerEvent>> SURVEILS_VERB =
            phrase("surveil(s)").<Function<Subject, TriggerEvent>>thenReturn(TriggerEvent.PlayerSurveils::new);

    private static final Parser<Function<Subject, TriggerEvent>> OBJECT_FREE_VERB = anyOf(SCRIES_VERB, SURVEILS_VERB);

    /// "[player] <verb> [or <verb>]*" — one or more object-free player
    /// verbs sharing a subject. Each verb produces one peer event;
    /// the single-verb case is a singleton list.
    private static final Parser<List<TriggerEvent>> PLAYER_OBJECT_FREE_TRIGGER = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            OBJECT_FREE_VERB.atLeastOnceDelimitedBy(word("or"), Collectors.toUnmodifiableList()),
            (p, fns) -> fns.stream().map(fn -> fn.apply(p)).toList());

    private static final Parser<TriggerEvent> PLAYER_CASTS = Parser.sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cast(s)")),
                    SELECTOR,
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
            .followedBy(anyOf(
                    phrase("each turn"),
                    // "during each opponent's turn" — narrower scope
                    // (Wavebreak Hippocamp: "Whenever you cast your
                    // first spell during each opponent's turn, draw
                    // a card.").
                    phrase("during each opponent's turn"),
                    phrase("during your turn")))
            .map(x -> x); // widen for typing

    private static final Parser<TriggerEvent> PLAYER_CYCLES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("cycle(s)")), SELECTOR, TriggerEvent.PlayerCycles::new);

    private static final Parser<TriggerEvent> PLAYER_DISCARDS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("discard(s)")), SELECTOR, TriggerEvent.PlayerDiscards::new);

    /// Cycle / discard verb factories for [#PLAYER_WITH_OBJECT_TRIGGER]
    /// — Grisly Survivor / Hekma Sentinels: "Whenever you cycle or
    /// discard a card, …". Single-verb cases stay reachable via
    /// [#PLAYER_CYCLES] / [#PLAYER_DISCARDS] (with their richer tails).
    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> CYCLES_VERB =
            phrase("cycle(s)").<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerCycles::new);

    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> DISCARDS_VERB = phrase("discard(s)")
            .<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerDiscards::new);

    /// "[player] kick[s] [spell]." — Saproling Infestation. Composes with
    /// [#WITH_OBJECT_VERB] so shared-subject fan-outs ("cast or kick a
    /// spell") land naturally; also fires as the single verb via
    /// [#ATOMIC].
    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> KICKS_VERB =
            phrase("kick(s)").<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerKicks::new);

    private static final Parser<TriggerEvent> PLAYER_KICKS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("kick(s)")), SELECTOR, TriggerEvent.PlayerKicks::new);

    /// "[player] searches [whose] library" — library-search trigger
    /// (Archivist of Oghma: "Whenever an opponent searches their
    /// library, …").
    private static final Parser<TriggerEvent.PlayerSearchesLibrary> PLAYER_SEARCHES_LIBRARY = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("search(es)")),
            anyOf(
                            word("your").thenReturn(Subject.PlayerRef.YOU),
                            word("their").thenReturn(Subject.PlayerRef.THEY),
                            word("its").thenReturn(Subject.PlayerRef.THAT_PLAYER))
                    .followedBy(word("library")),
            TriggerEvent.PlayerSearchesLibrary::new);

    /// "[subject] is turned face up" — morph/manifest flip trigger.
    private static final Parser<TriggerEvent> IS_TURNED_FACE_UP =
            SubjectParsers.SUBJECT.followedBy(phrase("is turned face up")).map(TriggerEvent.IsTurnedFaceUp::new);

    /// "[subject] mutates" — mutate trigger (Ikoria).
    private static final Parser<TriggerEvent> MUTATES =
            SubjectParsers.SUBJECT.followedBy(phrase("mutate(s)")).map(TriggerEvent.Mutates::new);

    /// "[player] give[s] a gift" — Aetherdrift Gifts trigger (Jolly
    /// Gerbils).
    private static final Parser<TriggerEvent> PLAYER_GIVES_GIFT =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("give(s) a gift")).map(TriggerEvent.PlayerGivesGift::new);

    /// "[player] attack[s] with [amount] creature(s) [with <keyword>]?"
    /// — Raiding Horde; Tide Skimmer: "Whenever you attack with two
    /// or more creatures with flying, draw a card." The optional
    /// [Selector.WithClause] attaches to the implicit attackers.
    private static final Parser<TriggerEvent> ATTACKS_WITH = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("attack(s) with")),
                    AMOUNT.followedBy(phrase("creature(s)")),
                    TriggerEvent.AttacksWith::new)
            .optionallyFollowedBy(SelectorParsers.WITH_CLAUSE, TriggerEvent.AttacksWith::withWith)
            .map(x -> x); // widen for typing

    /// "[player] control[s] no [selector]" — state-condition trigger.
    private static final Parser<TriggerEvent> CONTROLS_NONE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("control(s) no")),
            SELECTOR,
            TriggerEvent.ControlsNone::new);

    /// "[player] control[s] [selector]" — positive state-condition
    /// (Endangered Armodon). Must follow [#CONTROLS_NONE] because
    /// both start with "control(s)".
    private static final Parser<TriggerEvent> CONTROLS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("control(s)")), SELECTOR, TriggerEvent.Controls::new);

    /// "[player] play[s] [selector]" — generic land/card-play trigger
    /// (e.g., "When you play another land"). Distinct from
    /// [#PLAYER_PLAYS_LAND] because the selector carries qualifiers.
    private static final Parser<TriggerEvent> PLAYER_PLAYS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("play(s)")), SELECTOR, TriggerEvent.PlayerPlays::new);

    private static final Parser<TriggerEvent.PlayerDraws> PLAYER_DRAWS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("draw(s)")),
            AMOUNT.followedBy(phrase("card(s)")),
            TriggerEvent.PlayerDraws::new);

    /// "[player] draw[s] [your|their] [first|second|third|fourth] card
    /// each turn" — PlayerDraws specialization that fires only on the
    /// n-th draw each turn (Erudite Wizard, Knights of Dol Amroth,
    /// Lat-Nam Adept). Parallels [#PLAYER_CASTS_NTH].
    private static final Parser<TriggerEvent.PlayerDraws> PLAYER_DRAWS_NTH = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("draw(s) [your|their]")),
                    SPELL_ORDINAL.followedBy(phrase("card(s)")),
                    (player, nth) -> new TriggerEvent.PlayerDraws(player, Amount.exact(1)).nth(nth))
            .followedBy(phrase("each turn"));

    private static final Parser<TriggerEvent> PLAYER_GAINS_LIFE =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("gain(s) life")).map(TriggerEvent.PlayerGainsLife::new);

    private static final Parser<TriggerEvent> PLAYER_LOSES_LIFE =
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("lose(s) life")).map(TriggerEvent.PlayerLosesLife::new);

    private static final Parser<TriggerEvent> PLAYER_PLAYS_LAND = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("play(s) [a|an] land"))
            .map(TriggerEvent.PlayerPlaysLand::new);

    /// Factory for a shared-object player-verb trigger: consumes the verb
    /// token and yields a constructor that binds the shared subject and
    /// shared selector parsed up front and at the tail. Enables generic
    /// shared-subject shared-object disjunctions like "you create or
    /// sacrifice a token" (Mirkwood Bats) without a dedicated combo
    /// parser.
    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> SACRIFICES_VERB = phrase("sacrifice(s)")
            .<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerSacrifices::new);

    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> CREATES_VERB = phrase("create(s)")
            .<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerCreates::new);

    /// Bare-cast verb, shared with the multi-verb [#PLAYER_WITH_OBJECT_TRIGGER]
    /// pathway (Archmage Emeritus: "cast or copy an instant or
    /// sorcery spell"). The tail-carrying single-verb form lives in
    /// [#PLAYER_CASTS] instead so its "from \[zone\]" / "this turn"
    /// modifiers remain reachable.
    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> CASTS_VERB =
            phrase("cast(s)").<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerCasts::new);

    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> COPIES_VERB = phrase("[copy|copies]")
            .<BiFunction<Subject, Selector, TriggerEvent>>thenReturn(TriggerEvent.PlayerCopies::new);

    private static final Parser<BiFunction<Subject, Selector, TriggerEvent>> WITH_OBJECT_VERB =
            anyOf(SACRIFICES_VERB, CREATES_VERB, CASTS_VERB, COPIES_VERB, CYCLES_VERB, DISCARDS_VERB, KICKS_VERB);

    /// "[player] <verb> or <verb> [or <verb>]* <selector>" — two or
    /// more player verbs sharing both subject and object. Each verb
    /// produces one peer event. Restricted to ≥2 verbs so single-verb
    /// forms with richer tails ([#PLAYER_CASTS] "from …" / "this
    /// turn") stay reachable via [#ATOMIC].
    private static final Parser<List<TriggerEvent>> PLAYER_WITH_OBJECT_TRIGGER = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            WITH_OBJECT_VERB
                    .atLeastOnceDelimitedBy(word("or"), Collectors.toUnmodifiableList())
                    .suchThat(fns -> fns.size() >= 2, "two or more verbs"),
            SELECTOR,
            (p, fns, sel) -> fns.stream().map(fn -> fn.apply(p, sel)).toList());

    /// Single-verb ATOMIC fallback for [#SACRIFICES_VERB] /
    /// [#CREATES_VERB] — needed because [#PLAYER_WITH_OBJECT_TRIGGER]
    /// now requires ≥2 verbs. Single-verb [#CASTS_VERB] already has
    /// [#PLAYER_CASTS] as its tail-carrying fallback.
    private static final Parser<TriggerEvent> PLAYER_SINGLE_WITH_OBJECT_TRIGGER = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            anyOf(SACRIFICES_VERB, CREATES_VERB),
            SELECTOR,
            (p, fn, sel) -> fn.apply(p, sel));

    private static final Parser<TriggerEvent> TAPS_FOR_MANA = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("tap(s)")),
            SELECTOR.followedBy(phrase("for mana")),
            TriggerEvent.TapsForMana::new);

    /// "[subject] is tapped for mana" — passive-voice form used when
    /// the tapping agent is implicit (Vernal Bloom: "Whenever a
    /// Forest is tapped for mana, its controller adds an additional
    /// {G}.").
    private static final Parser<TriggerEvent> IS_TAPPED_FOR_MANA = SubjectParsers.SUBJECT
            .followedBy(phrase("[is|are] tapped for mana"))
            .map(TriggerEvent.IsTappedForMana::new);

    // ── At-the-beginning-of-step/phase ────────────────────────────────

    /// Step names (optional "step" suffix) used as trigger anchors.
    static final Parser<Step> STEP_NAME = anyOf(
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

    /// A phase reference, always produced as an [TriggerEvent.AtPhase]
    /// so the twin-main-phase qualifier (first/second/precombat/postcombat)
    /// is preserved for the rules engine. [Phase#MAIN] represents
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
    /// "at the beginning of …" triggers. `each` is true when oracle
    /// text reads "each [step]" (applies to every player's version).
    private record StepOwner(@Nullable Subject owner, boolean each) {}

    private static final Parser<StepOwner> STEP_OWNER = anyOf(
            word("your").thenReturn(new StepOwner(Subject.player(Subject.PlayerRef.YOU), false)),
            phrase("each [player's|players]").thenReturn(new StepOwner(null, true)),
            word("each").thenReturn(new StepOwner(null, true)),
            // "the end step" — unqualified; defaults to each turn's end step
            // per rule 514 (Groundbreaker: "At the beginning of the end step").
            word("the").thenReturn(new StepOwner(null, false)));

    private static final Parser<TriggerEvent> AT_BEGINNING_OF = phrase("the beginning of")
            .then(anyOf(
                    // "combat on [possessive] turn" — alternate form for
                    // the beginning-of-combat step with an inline turn
                    // qualifier (Mindwrack Harpy: "At the beginning of
                    // combat on your turn, …").
                    sequence(
                                    word("combat").followedBy(phrase("on")).thenReturn(Step.BEGINNING_OF_COMBAT),
                                    anyOf(
                                            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
                                            word("each").thenReturn((Subject) null)),
                                    (step, owner) -> {
                                        TriggerEvent.OwnerScoped ev = new TriggerEvent.AtStep(step);
                                        return ev.withOwner(owner, owner == null);
                                    })
                            .followedBy(word("turn")),
                    sequence(
                            STEP_OWNER,
                            Parser.<TriggerEvent.OwnerScoped>anyOf(STEP_NAME.map(TriggerEvent.AtStep::new), PHASE_NAME),
                            (owner, event) -> event.withOwner(owner.owner(), owner.each()))))
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
            // Combat state changes.
            BECOMES_BLOCKED,
            BECOMES_TAPPED,
            BECOMES_UNTAPPED,
            BECOMES_TARGET_OF,
            // Attack formation must precede ATTACKS so "attack with two"
            // isn't swallowed as a bare "attack" + stray "with".
            ATTACKS_WITH,
            ATTACKS_UNBLOCKED, // must precede ATTACKS (shared "attacks" prefix; "and isn't blocked" is the
            // distinguisher)
            // Single combat verbs.
            ATTACKS,
            BLOCKS,
            // Damage and stack events.
            DEALS_DAMAGE,
            IS_DEALT_DAMAGE,
            IS_CAST,
            IS_COUNTERED,
            IS_PUT_INTO,
            IS_RETURNED_TO,
            IS_TURNED_FACE_UP,
            PLAYER_ROLLS_DICE,
            LEAVES,
            MUTATES,
            // Player actions — must precede ENTERS because PLAYER_SUBJECT
            // has narrower overlap with SUBJECT (e.g., "you").
            CONTROLS_NONE, // must precede CONTROLS (both start with "control(s)")
            CONTROLS,
            PLAYER_CASTS_NTH, // must precede PLAYER_CASTS (longer prefix)
            PLAYER_CASTS_SELF, // must precede PLAYER_CASTS — self-ref wins over selector
            PLAYER_CASTS,
            PLAYER_SINGLE_WITH_OBJECT_TRIGGER, // single-verb sacrifice/create fallback
            PLAYER_PROLIFERATES,
            PLAYER_ACTIVATES_ABILITY,
            PLAYER_CYCLES,
            PLAYER_DISCARDS,
            PLAYER_KICKS,
            PLAYER_SEARCHES_LIBRARY,
            PLAYER_DRAWS_NTH, // must precede PLAYER_DRAWS (longer "your <ordinal> card each turn" prefix)
            PLAYER_DRAWS,
            PLAYER_GAINS_LIFE,
            PLAYER_GIVES_GIFT,
            PLAYER_LOSES_LIFE,
            PLAYER_PLAYS_LAND, // must precede PLAYER_PLAYS (longer match)
            PLAYER_PLAYS,
            IS_TAPPED_FOR_MANA, // must precede TAPS_FOR_MANA (passive form has longer match)
            TAPS_FOR_MANA,
            // Default object verbs.
            ENTERS,
            DIES);

    /// One trigger event or a shared-subject disjunction of peer events.
    /// A single-event match is a singleton list; a disjunction expands
    /// into one triggered ability per event (no composed trigger value is
    /// ever stored).
    public static final Parser<List<TriggerEvent>> TRIGGER_EVENT = Parser.<List<TriggerEvent>>anyOf(
                    // Shared-subject disjunctions first (longest match).
                    ENTERS_OR_DIES, // must precede ENTERS
                    ATTACKS_OR_BLOCKS,
                    BLOCKS_OR_BECOMES_BLOCKED,
                    // Generic player-verb disjunctions (compose any mix of
                    // registered verb factories sharing a subject, with or
                    // without a shared object). Multi-verb only — single-
                    // verb cases fall to the ATOMIC level so cast/copy get
                    // their respective tails.
                    PLAYER_WITH_OBJECT_TRIGGER,
                    PLAYER_OBJECT_FREE_TRIGGER,
                    ATOMIC.map(List::of))
            // Trailing "or [atomic]" (Raging Ravine: "enters or becomes
            // blocked"). Collects into a flat list of peer events.
            .optionallyFollowedBy(word("or").then(ATOMIC), TriggerEventParsers::appendEvent);

    private static List<TriggerEvent> appendEvent(List<TriggerEvent> events, TriggerEvent next) {
        var all = new ArrayList<>(events);
        all.add(next);
        return List.copyOf(all);
    }
}
