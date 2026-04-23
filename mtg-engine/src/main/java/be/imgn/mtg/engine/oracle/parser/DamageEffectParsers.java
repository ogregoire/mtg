package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.CARD_TYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Map;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Subject;

/// Leaf-effect parsers for damage and life totals: deal-damage variants
/// (including divided and split forms), gain-life, and lose-life.
/// Produces [Effect] values consumed by [EffectParsers#BASE_EFFECT] /
/// [EffectParsers#CLAUSE].
final class DamageEffectParsers {
    private DamageEffectParsers() {}

    private static final Subject YOU = Subject.player(Subject.PlayerRef.YOU);

    /// Optional "each" distributive prefix — "[subjects] each [verb]"
    /// (e.g., Hunters' Feast: "target players each gain 6 life").
    static Parser<String> each(Parser<String> verb) {
        return anyOf(phrase("Each").then(verb), verb);
    }

    // ── Deal damage ───────────────────────────────────────────────────

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_SUBJ = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s)")),
            AMOUNT.followedBy(phrase("damage to")),
            SubjectParsers.SUBJECT,
            Effect.DealDamage::new);

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_VERB = sequence(
            phrase("Deal").then(AMOUNT),
            phrase("damage to").then(SubjectParsers.SUBJECT),
            (amount, target) -> new Effect.DealDamage(Subject.selfRef(null), amount, target));

    /// "[source] deals N damage to A and M damage to B." — split damage to
    /// two targets from the same source (e.g., Char). Emits two
    /// [Effect.DealDamage] sharing the parsed source; the list is
    /// flattened by [OracleParser#EFFECT_SEQUENCE] so the two damage
    /// effects land side-by-side in the enclosing ability's effect list.
    static final Parser<List<Effect>> DEAL_DAMAGE_SPLIT = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s)")),
            sequence(AMOUNT.followedBy(phrase("damage to")), SubjectParsers.SUBJECT, Map::entry),
            sequence(word("and").then(AMOUNT).followedBy(phrase("damage to")), SubjectParsers.SUBJECT, Map::entry),
            (source, first, second) -> List.of(
                    new Effect.DealDamage(source, first.getKey(), first.getValue()),
                    new Effect.DealDamage(source, second.getKey(), second.getValue())));

    /// "[source] deals damage to [target] equal to [amount]." — amount
    /// trails the target (e.g., Solar Blaze: "Each creature deals damage
    /// to itself equal to its power."). Declared after
    /// [CountOfParsers#PROPERTY_OF_AMOUNT] because the amount commonly
    /// references a property (e.g., "its power").
    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_TRAILING_AMOUNT = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s) damage to")),
            SubjectParsers.SUBJECT,
            phrase("equal to").then(anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, AMOUNT)),
            (source, target, amount) -> new Effect.DealDamage(source, amount, target));

    /// "[source] deals damage equal to [amount] to [target]" — amount-
    /// first variant (Spikeshot Goblin: "This creature deals damage equal
    /// to its power to any target."). Complements
    /// [#DEAL_DAMAGE_TRAILING_AMOUNT] where the amount trails the
    /// target.
    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_AMOUNT_FIRST = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s)")).followedBy(word("damage")),
            phrase("equal to").then(anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, AMOUNT)),
            word("to").then(SubjectParsers.SUBJECT),
            (source, amount, target) -> new Effect.DealDamage(source, amount, target));

    /// "[source] deals N damage divided as you choose among [targets]." —
    /// split damage (rule 609.4, Twin Bolt: "Twin Bolt deals 2 damage
    /// divided as you choose among one or two targets."). `targets` is
    /// parsed as the count-typed "each of N targets" subject so the
    /// downstream engine sees a [Subject.EachOfTargets] split group.
    private static final Parser<Subject> AMONG_TARGETS = sequence(
            word("among").then(AMOUNT),
            anyOf(
                    word("targets").thenReturn((String) null),
                    word("target").then(CARD_TYPE).map(t -> t.name().toLowerCase() + "s")),
            Subject.EachOfTargets::new);

    static final Parser<Effect.DealDividedDamage> DEAL_DIVIDED_DAMAGE = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s)")),
            AMOUNT.followedBy(phrase("damage divided as you choose")),
            AMONG_TARGETS,
            Effect.DealDividedDamage::new);

    /// Deal-damage dispatcher. Trailing-amount variant must precede the
    /// standard "[source] deals N damage to X" shape because both share
    /// the "[source] deals" prefix.
    static final Parser<Effect.DealDamage> DEAL_DAMAGE = anyOf(
                    DEAL_DAMAGE_TRAILING_AMOUNT, DEAL_DAMAGE_AMOUNT_FIRST, DEAL_DAMAGE_SUBJ, DEAL_DAMAGE_VERB)
            // Optional "chosen at random" target-selection
            // qualifier (Goblin Test Pilot). Applied as a wither
            // so the specific DealDamage shape that matched
            // doesn't need to know about it.
            .optionallyFollowedBy(phrase("chosen at random"), (dd, _) -> dd.asRandom());

    // ── Gain life ─────────────────────────────────────────────────────

    /// Amount after "gain life" — either `N` followed by `life`, or the
    /// longer form `life equal to X's power` where the amount trails.
    /// Also accepts "life equal to the life lost this way" (Blood
    /// Tithe, Exsanguinate) as a back-reference to the damage/life
    /// total of the prior effect in the same resolution.
    private static final Parser<Amount> GAIN_LIFE_AMOUNT = anyOf(
            phrase("life equal to the life lost this way").thenReturn(Amount.reference("life lost this way")),
            word("life").then(phrase("equal to")).then(CountOfParsers.PROPERTY_OF_AMOUNT),
            AMOUNT.followedBy(word("life")));

    static final Parser<Amount> GAIN_LIFE_NO_PLAYER = each(phrase("gain(s)"))
            .then(GAIN_LIFE_AMOUNT)
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, e) -> e);

    static final Parser<Effect.GainLife> GAIN_LIFE = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, GAIN_LIFE_NO_PLAYER, Effect.GainLife::new),
            GAIN_LIFE_NO_PLAYER.map(a -> new Effect.GainLife(YOU, a)));

    // ── Lose life ─────────────────────────────────────────────────────

    /// `half [possessive] life[, rounded up/down]` — an Amount for "lose
    /// half your life" style phrases (Cruel Bargain, Infernal Contract).
    /// Consumes the literal "life" word; default rounding is UP (the sole
    /// form used by current cards is "rounded up").
    private static final Parser<Amount.Half> HALF_LIFE = phrase("half [your|their|its] life")
            .thenReturn(new Amount.Half(new Amount.PropertyOf(Subject.player(Subject.PlayerRef.YOU), "life total")))
            .optionallyFollowedBy(CountOfParsers.ROUNDING_DIRECTION, Amount.Half::withRounding);

    static final Parser<Amount> LOSE_LIFE_NO_PLAYER = each(phrase("lose(s)"))
            .then(anyOf(
                    HALF_LIFE,
                    AMOUNT.followedBy(word("life")).optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, e) -> e)));

    static final Parser<Effect.LoseLife> LOSE_LIFE = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECTS, LOSE_LIFE_NO_PLAYER, Effect.LoseLife::new),
            LOSE_LIFE_NO_PLAYER.map(a -> new Effect.LoseLife(YOU, a)));
}
