package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.INTEGER;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.Map;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Property;
import be.imgn.mtg.engine.oracle.domain.Selector;
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

    /// "the damage \[already|so far\]? dealt to \[subject\] \[so far\]? this
    /// turn \[by \[source\]\]?" — turn-history damage amount (Final
    /// Punishment; Reverse Polarity: "the damage dealt to you so far
    /// this turn by artifacts."; Whipkeeper: "the damage already dealt
    /// to it this turn"). Declared early so [#DEAL_DAMAGE_TRAILING_AMOUNT]
    /// and [#WHERE_X_IS_WITH_DAMAGE] can reference it without a forward
    /// reference.
    static final Parser<Amount.DamageDealtThisTurn> DAMAGE_DEALT_THIS_TURN = phrase("the damage")
            .then(anyOf(phrase("already dealt to"), phrase("so far dealt to"), phrase("dealt to")))
            .then(SubjectParsers.SUBJECT)
            .optionallyFollowedBy(phrase("so far"), (s, _) -> s)
            .followedBy(phrase("this turn"))
            .map(Amount.DamageDealtThisTurn::new)
            .optionallyFollowedBy(word("by").then(SubjectParsers.SUBJECT), Amount.DamageDealtThisTurn::withBy);

    /// "[source] deals damage to [target] equal to [amount]." — amount
    /// trails the target (e.g., Solar Blaze: "Each creature deals damage
    /// to itself equal to its power."; Whipkeeper: "equal to the damage
    /// already dealt to it this turn"). [#DAMAGE_DEALT_THIS_TURN] is
    /// listed first so "the damage already dealt …" is matched before
    /// [CountOfParsers#PROPERTY_OF_AMOUNT] tries "the damage" as a
    /// possessive subject and fails on the following adverb.
    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_TRAILING_AMOUNT = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("deal(s) damage to")),
            SubjectParsers.SUBJECT,
            phrase("equal to").then(anyOf(DAMAGE_DEALT_THIS_TURN, CountOfParsers.PROPERTY_OF_AMOUNT, AMOUNT)),
            (source, target, amount) -> new Effect.DealDamage(source, amount, target));

    /// "[source] deals damage equal to [amount] to [target]" — amount-
    /// first variant (Spikeshot Goblin: "This creature deals damage equal
    /// to its power to any target."). Complements
    /// [#DEAL_DAMAGE_TRAILING_AMOUNT] where the amount trails the
    /// target.
    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_AMOUNT_FIRST = sequence(
            SubjectParsers.SUBJECT
                    .followedBy(anyOf(phrase("each deal(s)"), phrase("deal(s)")))
                    .followedBy(word("damage")),
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
            Parser.<Selector>anyOf(word("targets").thenReturn(null), SelectorParsers.SELECTOR),
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
            .optionallyFollowedBy(phrase("chosen at random"), (dd, _) -> dd.asRandom())
            // Optional "for each X" multiplier on the damage amount
            // (Baki's Curse: "~ deals 2 damage to each creature for
            // each Aura attached to that creature."). Replaces the
            // base amount with the count-of expression.
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.DealDamage::withAmount)
            // Optional ", where X is …" — binds the X in a variable
            // damage amount (Gates Ablaze: "deals X damage to each
            // creature, where X is the number of Gates you control.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.DealDamage::withXDefinition);

    /// "\[subject\] deals double that damage." — damage-multiplying replacement
    /// body (Fire Servant: "it deals double that damage instead."). The
    /// subject is consumed to handle the pronoun ("it") but not stored;
    /// the surrounding [Effect.Replace] event already names the source.
    static final Parser<Effect.DamageDealtMultiplier> DEAL_DAMAGE_DOUBLE = SubjectParsers.SUBJECT
            .followedBy(phrase("deals double that damage"))
            .thenReturn(new Effect.DamageDealtMultiplier(2));

    // ── Gain life ─────────────────────────────────────────────────────

    /// Amount after "gain life" — either `N` followed by `life`, or the
    /// longer form `life equal to X's power` where the amount trails.
    /// Also accepts "life equal to the life lost this way" (Blood
    /// Tithe, Exsanguinate) as a back-reference to the damage/life
    /// total of the prior effect in the same resolution.
    private static final Parser<Amount> GAIN_LIFE_AMOUNT = anyOf(
            phrase("life equal to the life lost this way").thenReturn(Amount.reference("life lost this way")),
            phrase("life equal to the damage dealt this way").thenReturn(Amount.reference("damage dealt this way")),
            phrase("life equal to the result").thenReturn(Amount.reference("the result")),
            word("life")
                    .then(phrase("equal to"))
                    .then(CountOfParsers.PROPERTY_OF_AMOUNT)
                    .optionallyFollowedBy(word("plus").then(CountOfParsers.PROPERTY_OF_AMOUNT), Amount.Plus::new),
            AMOUNT.followedBy(word("life")));

    static final Parser<Amount> GAIN_LIFE_NO_PLAYER = each(phrase("gain(s)"))
            .then(GAIN_LIFE_AMOUNT)
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, e) -> e);

    /// Local "where X is …" — extends [CountOfParsers#WHERE_X_IS] with a
    /// damage-dealt base for Reverse Polarity ("where X is twice the
    /// damage dealt to you so far this turn by artifacts."). Declared
    /// here to avoid the static-init cycle that would arise from putting
    /// [#DAMAGE_DEALT_THIS_TURN] into the shared CountOfParsers.
    private static final Parser<Amount> WHERE_X_IS_WITH_DAMAGE = string(",")
            .then(phrase("where X is"))
            .then(anyOf(
                    sequence(word("twice").thenReturn(2), DAMAGE_DEALT_THIS_TURN, Amount.Times::new),
                    DAMAGE_DEALT_THIS_TURN.<Amount>map(d -> d),
                    CountOfParsers.PROPERTY_OF_AMOUNT
                            .optionallyFollowedBy(word("plus").then(AmountParsers.ATOMIC_AMOUNT), Amount.Plus::new)
                            .optionallyFollowedBy(word("minus").then(AmountParsers.ATOMIC_AMOUNT), Amount.Minus::new),
                    AMOUNT));

    static final Parser<Effect.GainLife> GAIN_LIFE = anyOf(
                    sequence(SubjectParsers.PLAYER_SUBJECTS, GAIN_LIFE_NO_PLAYER, Effect.GainLife::new),
                    GAIN_LIFE_NO_PLAYER.map(a -> new Effect.GainLife(YOU, a)))
            // Optional ", where X is <def>" — binds the X in a
            // variable amount (An-Havva Inn: "You gain X plus 1
            // life, where X is the number of green creatures on
            // the battlefield."; Reverse Polarity: "where X is
            // twice the damage dealt to you so far this turn by
            // artifacts.").
            .optionallyFollowedBy(WHERE_X_IS_WITH_DAMAGE, Effect.GainLife::withXDefinition);

    // ── Lose life ─────────────────────────────────────────────────────

    /// `half [possessive] life[, rounded up/down]` — an Amount for "lose
    /// half your life" style phrases (Cruel Bargain, Infernal Contract).
    /// Consumes the literal "life" word; default rounding is UP (the sole
    /// form used by current cards is "rounded up").
    static final Parser<Amount.Half> HALF_LIFE = phrase("half [your|their|its] life")
            .thenReturn(
                    new Amount.Half(new Amount.PropertyOf(Subject.player(Subject.PlayerRef.YOU), Property.LIFE_TOTAL)))
            .optionallyFollowedBy(CountOfParsers.ROUNDING_DIRECTION, Amount.Half::withRounding);

    private static final Parser<Amount.Third> A_THIRD_LIFE = phrase("a third of [your|their|its] life")
            .thenReturn(
                    new Amount.Third(new Amount.PropertyOf(Subject.player(Subject.PlayerRef.YOU), Property.LIFE_TOTAL)))
            .optionallyFollowedBy(CountOfParsers.ROUNDING_DIRECTION, Amount.Third::withRounding);

    static final Parser<Amount> LOSE_LIFE_NO_PLAYER = each(phrase("lose(s)"))
            .then(anyOf(
                    A_THIRD_LIFE,
                    HALF_LIFE,
                    // "life equal to the damage [already]? dealt to
                    // [subject] this turn" — Final Punishment.
                    word("life").then(phrase("equal to")).then(DAMAGE_DEALT_THIS_TURN),
                    word("life").then(phrase("equal to")).then(CountOfParsers.PROPERTY_OF_AMOUNT),
                    // "N life for each M life [player] gained" — scaled
                    // loss equal to factor × life-gain-event amount (False
                    // Cure: "loses 2 life for each 1 life they gained").
                    // The per-unit denominator M is consumed and discarded
                    // (always 1 in current oracle text).
                    sequence(
                            INTEGER.followedBy(word("life"))
                                    .followedBy(phrase("for each"))
                                    .followedBy(INTEGER)
                                    .followedBy(word("life")),
                            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("gained")),
                            (factor, who) -> new Amount.Times(factor, new Amount.LifeGainedThisWay(who))),
                    AMOUNT.followedBy(word("life")).optionallyFollowedBy(CountOfParsers.FOR_EACH, (base, e) -> e)));

    static final Parser<Effect.LoseLife> LOSE_LIFE = anyOf(
                    sequence(SubjectParsers.PLAYER_SUBJECTS, LOSE_LIFE_NO_PLAYER, Effect.LoseLife::new),
                    LOSE_LIFE_NO_PLAYER.map(a -> new Effect.LoseLife(YOU, a)))
            // Optional ", where X is <def>" — binds the X in a
            // variable amount (Minions' Murmurs: "You draw X cards
            // and you lose X life, where X is the number of
            // creatures you control.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.LoseLife::withXDefinition);
}
