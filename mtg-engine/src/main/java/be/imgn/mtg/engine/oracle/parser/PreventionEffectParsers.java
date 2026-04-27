package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURATION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURING_YOUR_TURN;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Duration;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Effect.Prevent;
import be.imgn.mtg.engine.oracle.domain.Effect.Prevent.Kind;

/// Leaf-effect parsers for damage prevention: PREVENT (universal + back-reference,
/// emitting [Prevent]), PREVENT_NEXT_DAMAGE, DAMAGE_CANT_BE_PREVENTED,
/// THAT_DAMAGE_CANT_BE_PREVENTED. Extracted from [EffectParsers] to keep that
/// file under the per-file soft limit.
final class PreventionEffectParsers {
    private PreventionEffectParsers() {}

    /// "Prevent all \[combat|noncombat\]? damage" — the kind slot of a
    /// universal prevention. Emits the typed [Kind] so the trailing
    /// source/target/duration slots can attach structurally.
    private static final Parser<Kind> PREVENT_ALL_KIND = phrase("Prevent all")
            .then(anyOf(
                    phrase("combat damage").thenReturn(Kind.COMBAT),
                    phrase("noncombat damage").thenReturn(Kind.NONCOMBAT),
                    word("damage").thenReturn(Kind.ANY)));

    /// "… that would be dealt" — passive-voice connector that precedes a
    /// `to`/`by` qualifier. Consumed purely for its side effect of
    /// advancing past the "that would be dealt" phrase.
    private static final Parser<String> THAT_WOULD_BE_DEALT = phrase("that would be dealt");

    /// Universal prevention body (Fog, Ethereal Haze, Cho-Manno's Blessing,
    /// Bubble Matrix, Mark of Asylum, Harmless Assault, Statecraft,
    /// Indentured Oaf). Orders arms from most specific (two subjects,
    /// both-directions) to least specific (unqualified "prevent all damage")
    /// so the longer wording always wins.
    private static final Parser<Prevent.AllDamage> PREVENT_ALL = anyOf(
            // "prevent all combat damage that would be dealt to and dealt by
            // [subject]" — Statecraft. Both-sides form; must precede the
            // single-direction "to …" arm.
            sequence(
                    PREVENT_ALL_KIND,
                    THAT_WOULD_BE_DEALT.then(phrase("to and dealt by")).then(SubjectParsers.SUBJECT),
                    (kind, subj) -> new Prevent.AllDamage(kind).withBothDirections(subj)),
            // "prevent all damage that would be dealt to [tgt] by [src]" —
            // Champion Lancer. Two-subject form; must precede bare "to"/"by"
            // arms so the trailing "by …" wins.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("to").then(SubjectParsers.SUBJECT),
                    phrase("by").then(SubjectParsers.SUBJECT),
                    (kind, to, by) -> new Prevent.AllDamage(kind).withTo(to).withBy(by)),
            // "prevent all damage that would be dealt to [subject] this turn by [subject]"
            // — Scarecrow: "Prevent all damage that would be dealt to you this turn
            // by creatures with flying.". Combines all three slots
            // (to/duration/by) — the duration goes between target and source.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("to").then(SubjectParsers.SUBJECT),
                    phrase("this turn by").then(SubjectParsers.SUBJECT),
                    (kind, to, by) ->
                            new Prevent.AllDamage(kind).withTo(to).withBy(by).withDuration(Duration.Fixed.THIS_TURN)),
            // "prevent all damage that would be dealt this turn by [subject]"
            // — Repel the Abominable / Harmless Assault.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("this turn by").then(SubjectParsers.SUBJECT),
                    (kind, by) -> new Prevent.AllDamage(kind).withBy(by).withDuration(Duration.Fixed.THIS_TURN)),
            // "prevent all damage that would be dealt this turn to [subject]"
            // — Divine Light. "this turn to …" variant order; must precede
            // the bare "to …" arm.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("this turn to").then(SubjectParsers.SUBJECT),
                    (kind, to) -> new Prevent.AllDamage(kind).withTo(to).withDuration(Duration.Fixed.THIS_TURN)),
            // "prevent all damage that would be dealt to [subject]" — Bubble
            // Matrix, Cho-Manno, Forfend, Mark of Asylum, Everdawn Champion.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("to").then(SubjectParsers.SUBJECT),
                    (kind, to) -> new Prevent.AllDamage(kind).withTo(to)),
            // "prevent all damage that would be dealt by [subject]" —
            // Ethereal Haze.
            sequence(
                    PREVENT_ALL_KIND.followedBy(THAT_WOULD_BE_DEALT),
                    phrase("by").then(SubjectParsers.SUBJECT),
                    (kind, by) -> new Prevent.AllDamage(kind).withBy(by)),
            // "prevent all \[kind\] damage that would be dealt this turn" —
            // Fog, Holy Day, Darkness. Passive-voice with duration but no
            // source/target.
            PREVENT_ALL_KIND
                    .followedBy(THAT_WOULD_BE_DEALT)
                    .followedBy(phrase("this turn"))
                    .map(kind -> new Prevent.AllDamage(kind).withDuration(Duration.Fixed.THIS_TURN)),
            // "prevent all damage that [source] would deal to [target]" —
            // Indentured Oaf, Goblin Furrier, Chameleon Blur. Active-voice
            // variant binding source and target.
            sequence(
                    PREVENT_ALL_KIND.followedBy(word("that")),
                    SubjectParsers.SUBJECT.followedBy(phrase("would deal to")),
                    SubjectParsers.SUBJECT,
                    (kind, by, to) -> new Prevent.AllDamage(kind).withBy(by).withTo(to)),
            // "prevent all \[kind\] damage \[subject\] would deal" —
            // active-voice source-only (Pay No Heed, Serene Sunset). The
            // trailing "this turn" is captured by the outer DURATION
            // suffix on [#PREVENT].
            sequence(
                    PREVENT_ALL_KIND,
                    SubjectParsers.SUBJECT.followedBy(phrase("would deal")),
                    (kind, by) -> new Prevent.AllDamage(kind).withBy(by)),
            // "prevent all damage" (no qualifier).
            PREVENT_ALL_KIND.map(Prevent.AllDamage::new));

    /// Universal prevention with optional prefixes/suffixes. Attaches:
    ///  - a leading "During your turn, …" (Personal Sanctuary) as
    ///    [Duration.Fixed#DURING_YOUR_TURN];
    ///  - a trailing [DURATION] ("this turn") if the body didn't already
    ///    consume it.
    static final Parser<Prevent.AllDamage> PREVENT_UNIVERSAL = anyOf(
                    sequence(DURING_YOUR_TURN, PREVENT_ALL, (_, p) -> p.withDuration(Duration.Fixed.DURING_YOUR_TURN)),
                    PREVENT_ALL)
            .optionallyFollowedBy(DURATION, Prevent.AllDamage::withDuration);

    /// "Prevent \[N of\]? that damage." — back-reference to damage mentioned
    /// in the preceding clause (Callous Giant: "prevent that damage"; Urza's
    /// Armor: "prevent 1 of that damage"). The full-subset form is emitted
    /// as `new ThatDamage()` with `amount == null`; the partial-subset form
    /// carries the structured [Amount].
    private static final Parser<Prevent.ThatDamage> PREVENT_THAT_DAMAGE = anyOf(
            phrase("Prevent").then(AMOUNT).followedBy(phrase("of that damage")).map(Prevent.ThatDamage::new),
            phrase("Prevent that damage").thenReturn(new Prevent.ThatDamage()));

    /// Unified entry point for [Prevent] (all back-reference and universal
    /// variants). Back-reference arms are tried first since they share
    /// the "Prevent …" prefix with the universal arms but consume a distinct
    /// continuation ("that damage", "N of that damage", "all but N of that damage").
    /// The [Amount.AllBut] form is handled naturally by [#PREVENT_THAT_DAMAGE] arm 1
    /// since "all but N" is a recognized [Amount] variant.
    static final Parser<Prevent> PREVENT = Parser.<Prevent>anyOf(PREVENT_THAT_DAMAGE, PREVENT_UNIVERSAL);

    /// "Prevent the next \[amount\] \[combat\]? damage that would be
    /// dealt to \[subject\] \[duration\]?." — structured damage-shield
    /// (Shield of the Ages, Decorated Griffin). Emits
    /// [Effect.PreventNextDamage] with typed amount / target /
    /// duration — distinct from the universal [Prevent] forms.
    static final Parser<Effect.PreventNextDamage> PREVENT_NEXT_DAMAGE = sequence(
                    phrase("Prevent the next").then(AMOUNT),
                    anyOf(
                            phrase("combat damage").thenReturn(true),
                            word("damage").thenReturn(false)),
                    Effect.PreventNextDamage::new)
            .optionallyFollowedBy(
                    phrase("that would be dealt to").then(SubjectParsers.SUBJECT), Effect.PreventNextDamage::withTarget)
            .optionallyFollowedBy(DURATION, Effect.PreventNextDamage::withDuration);

    /// "Damage that would be dealt \[by|to\] \[subject\] can't be prevented."
    /// — Excruciator ("by") / shielding rules ("to").
    static final Parser<Effect.DamageCantBePrevented> DAMAGE_CANT_BE_PREVENTED = sequence(
            phrase("Damage that would be dealt")
                    .then(anyOf(word("by").thenReturn(true), word("to").thenReturn(false))),
            SubjectParsers.SUBJECT.followedBy(phrase("can't be prevented")),
            (dealtBy, subj) -> new Effect.DamageCantBePrevented(subj, dealtBy));

    /// "The damage can't be prevented." — back-reference to the damage
    /// dealt by the preceding effect (Pinpoint Avalanche).
    static final Parser<Effect.ThatDamageCantBePrevented> THAT_DAMAGE_CANT_BE_PREVENTED =
            phrase("The damage can't be prevented").thenReturn(new Effect.ThatDamageCantBePrevented());
}
