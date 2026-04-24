package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURATION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURING_YOUR_TURN;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Effect;

/// Leaf-effect parsers for damage prevention: PREVENT, PREVENT_NEXT_DAMAGE,
/// DAMAGE_CANT_BE_PREVENTED, THAT_DAMAGE_CANT_BE_PREVENTED. Extracted
/// from [EffectParsers] to keep that file under the per-file soft limit.
final class PreventionEffectParsers {
    private PreventionEffectParsers() {}

    /// Optional "During your turn, " prefix applied to a [Effect.Prevent]
    /// — Personal Sanctuary ("During your turn, prevent all damage that
    /// would be dealt to you."). Encoded inside the prevention
    /// description since [Effect.Prevent] has no structured duration
    /// slot.
    private static Effect.Prevent withDuringYourTurn(Effect.Prevent p) {
        return new Effect.Prevent("during your turn: " + p.description());
    }

    static final Parser<Effect.Prevent> PREVENT_BODY = anyOf(
                    // "prevent that damage" — back-reference to the
                    // "… would deal damage …" clause inside a wrapping
                    // "if" trigger (Callous Giant: "If a source would
                    // deal 3 or less damage to this creature, prevent
                    // that damage.").
                    phrase("Prevent that damage").thenReturn(new Effect.Prevent("prevent that damage")),
                    // "prevent N of that damage" — shielding subset
                    // (Urza's Armor: "If a source would deal damage to you,
                    // prevent 1 of that damage.").
                    phrase("Prevent")
                            .then(AMOUNT)
                            .followedBy(phrase("of that damage"))
                            .map(amount -> new Effect.Prevent("prevent " + amount + " of that damage")),
                    // "prevent all combat damage that would be dealt this
                    // turn by [source]" — Harmless Assault. Optional
                    // source restriction at the tail.
                    phrase("Prevent all combat damage that would be dealt this turn by")
                            .then(SubjectParsers.SUBJECT)
                            .map(src -> new Effect.Prevent("prevent all combat damage this turn by " + src)),
                    // "prevent all combat damage that would be dealt this turn" (Fog,
                    // Darkness, Holy Day). Must precede the generic "prevent all
                    // damage" so "combat" isn't left unconsumed.
                    phrase("Prevent all combat damage that would be dealt this turn")
                            .thenReturn(new Effect.Prevent("prevent all combat damage this turn")),
                    // "prevent all damage that would be dealt to [tgt] by
                    // [src]" — Champion Lancer. Must precede the
                    // no-by arms so the trailing "by …" wins.
                    sequence(
                            phrase("Prevent all damage")
                                    .then(phrase("that would be dealt to"))
                                    .then(SubjectParsers.SUBJECT.followedBy(word("by"))),
                            SubjectParsers.SUBJECT,
                            (tgt, src) -> new Effect.Prevent("prevent all damage dealt to " + tgt + " by " + src)),
                    // "prevent all \[noncombat\]? damage that would be dealt
                    // \[this turn\]? to [subject]" — Divine Light (plain
                    // "damage this turn to …"); Mark of Asylum
                    // ("noncombat damage … to …"). The optional "this
                    // turn" precedes the target here, distinct from the
                    // "… to X this turn" order captured below.
                    sequence(
                            phrase("Prevent all")
                                    .then(anyOf(
                                            phrase("noncombat damage").thenReturn("noncombat damage"),
                                            word("damage").thenReturn("damage")))
                                    .followedBy(phrase("that would be dealt")),
                            anyOf(
                                    phrase("this turn to").thenReturn("this turn "),
                                    word("to").thenReturn("")),
                            SubjectParsers.SUBJECT,
                            (kind, turn, subject) ->
                                    new Effect.Prevent("prevent all " + kind + " " + turn + "dealt to " + subject)),
                    // "prevent all damage that would be dealt to [subject]" (Bubble
                    // Matrix, Cho-Manno; Forfend).
                    sequence(
                            phrase("Prevent all damage").followedBy(phrase("that would be dealt to")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt to " + subject)),
                    // "prevent all damage that would be dealt by [subject]"
                    // (Ethereal Haze).
                    sequence(
                            phrase("Prevent all damage").followedBy(phrase("that would be dealt by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt by " + subject)),
                    // "prevent all damage that would be dealt this turn by
                    // [subject]" — Repel the Abominable.
                    sequence(
                            phrase("Prevent all damage").followedBy(phrase("that would be dealt this turn by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt this turn by " + subject)),
                    // "prevent all damage that [source] would deal to
                    // [target]" (Indentured Oaf, Goblin Furrier, Chameleon
                    // Blur). Captures both the source and target subjects
                    // verbatim in the free-text description.
                    sequence(
                            phrase("Prevent all damage that")
                                    .then(SubjectParsers.SUBJECT.followedBy(phrase("would deal to"))),
                            SubjectParsers.SUBJECT,
                            (src, tgt) -> new Effect.Prevent("prevent all damage dealt by " + src + " to " + tgt)),
                    // "prevent all combat damage that would be dealt
                    // to and dealt by [subject]" — both-sides form
                    // (Statecraft). Must precede the bare "dealt to"
                    // arm so the longer match wins.
                    sequence(
                            phrase("Prevent all combat damage")
                                    .followedBy(phrase("that would be dealt to and dealt by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) ->
                                    new Effect.Prevent("prevent all combat damage dealt to and dealt by " + subject)),
                    // "prevent all combat damage that would be dealt to [subject]"
                    // (Everdawn Champion).
                    sequence(
                            phrase("Prevent all combat damage").followedBy(phrase("that would be dealt to")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all combat damage dealt to " + subject)),
                    // "prevent all combat damage that would be dealt by [subject]".
                    sequence(
                            phrase("Prevent all combat damage").followedBy(phrase("that would be dealt by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all combat damage dealt by " + subject)),
                    // "prevent all combat damage [subject] would deal" —
                    // source-specific combat prevention without the "that
                    // … be dealt" passive wording (Serene Sunset: "Prevent
                    // all combat damage X target creatures would deal this
                    // turn.").
                    phrase("Prevent all combat damage")
                            .then(SubjectParsers.SUBJECT.followedBy(phrase("would deal")))
                            .map(src -> new Effect.Prevent("prevent all combat damage dealt by " + src)),
                    // "prevent all damage a source of your choice would deal
                    // [this turn]" — Pay No Heed. The source is captured
                    // verbatim so the grammar doesn't require a structured
                    // "source of X" subject yet.
                    sequence(
                            phrase("Prevent all damage"),
                            SubjectParsers.SUBJECT.followedBy(phrase("would deal")),
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt by " + subject)),
                    // "prevent all damage" (no qualifier)
                    phrase("Prevent all damage").thenReturn(new Effect.Prevent("prevent all damage")))
            // Optional trailing duration ("this turn") — Forfend.
            .optionallyFollowedBy(
                    DURATION,
                    (p, d) -> new Effect.Prevent(
                            p.description() + " [" + d.getClass().getSimpleName() + "]"));

    /// Optional "During your turn, " prefix on a prevention effect
    /// (Personal Sanctuary: "During your turn, prevent all damage that
    /// would be dealt to you.").
    static final Parser<Effect.Prevent> PREVENT =
            anyOf(sequence(DURING_YOUR_TURN, PREVENT_BODY, (d, p) -> withDuringYourTurn(p)), PREVENT_BODY);

    /// "Prevent the next \[amount\] \[combat\]? damage that would be
    /// dealt to \[subject\] \[duration\]?." — structured damage-shield
    /// (Shield of the Ages, Decorated Griffin). Emits
    /// [Effect.PreventNextDamage] with typed amount / target /
    /// duration — distinct from the free-text [Effect.Prevent] fallback.
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
