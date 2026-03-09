package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Optional;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.effect.LoseLifeEffect;
import be.imgn.mtg.engine.selector.PlayerReference;

/// Parser for damage and life effects in oracle text.
public final class DamageParser {

    private DamageParser() {}

    /// Parses a player reference.
    private static final Parser<PlayerReference> PLAYER_REF = anyOf(
            string("Target player").thenReturn(PlayerReference.TARGET_PLAYER),
            string("target player").thenReturn(PlayerReference.TARGET_PLAYER),
            string("Each player").thenReturn(PlayerReference.EACH_PLAYER),
            string("each player").thenReturn(PlayerReference.EACH_PLAYER),
            string("Each opponent").thenReturn(PlayerReference.EACH_OPPONENT),
            string("each opponent").thenReturn(PlayerReference.EACH_OPPONENT),
            string("that player").thenReturn(PlayerReference.THAT_PLAYER));

    /// The "amount damage to target" fragment, shared by both patterns.
    private static final Parser<DealDamageEffect> DAMAGE_TO_TARGET =
            sequence(AmountParser.AMOUNT, string("damage to").then(ReferenceParser.SUBJECT), DealDamageEffect::new);

    /// Parses "Deal 3 damage to any target." or "Deal X damage to target creature.",
    /// or self-reference forms like "~ deals 3 damage to any target."
    ///
    /// Pattern 1: "Deal" amount "damage to" target ["."]
    /// Pattern 2: "~" "deals" amount "damage to" target ["."]
    public static final Parser<DealDamageEffect> DEAL_DAMAGE_EFFECT = anyOf(
                    word("Deal").then(DAMAGE_TO_TARGET), string("~ deals").then(DAMAGE_TO_TARGET))
            .optionallyFollowedBy(".");

    /// Parses "You gain 3 life." or "Target player gains 5 life."
    ///
    /// Pattern: `[Player] "gain"/"gains" amount "life" ["."]`
    public static final Parser<GainLifeEffect> GAIN_LIFE_EFFECT = anyOf(
                    // "You gain 3 life"
                    string("You gain")
                            .then(AmountParser.AMOUNT)
                            .followedBy(word("life"))
                            .map(amount -> new GainLifeEffect(Optional.of(PlayerReference.YOU), amount)),
                    // "Target player gains 3 life"
                    sequence(
                            PLAYER_REF,
                            word("gains").then(AmountParser.AMOUNT).followedBy(word("life")),
                            (player, amount) -> new GainLifeEffect(Optional.of(player), amount)),
                    // "Gain 3 life" (implicit you)
                    word("Gain")
                            .then(AmountParser.AMOUNT)
                            .followedBy(word("life"))
                            .map(amount -> new GainLifeEffect(Optional.empty(), amount)))
            .optionallyFollowedBy(".");

    /// Parses "Target player loses 3 life." or "You lose 2 life."
    ///
    /// Pattern: `[Player] "lose"/"loses" amount "life" ["."]`
    public static final Parser<LoseLifeEffect> LOSE_LIFE_EFFECT = anyOf(
                    // "You lose 3 life"
                    string("You lose")
                            .then(AmountParser.AMOUNT)
                            .followedBy(word("life"))
                            .map(amount -> new LoseLifeEffect(Optional.of(PlayerReference.YOU), amount)),
                    // "Target player loses 3 life"
                    sequence(
                            PLAYER_REF,
                            word("loses").then(AmountParser.AMOUNT).followedBy(word("life")),
                            (player, amount) -> new LoseLifeEffect(Optional.of(player), amount)),
                    // "Lose 3 life" (implicit you)
                    word("Lose")
                            .then(AmountParser.AMOUNT)
                            .followedBy(word("life"))
                            .map(amount -> new LoseLifeEffect(Optional.empty(), amount)))
            .optionallyFollowedBy(".");
}
