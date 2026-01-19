package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.effect.DealDamageEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.GainLifeEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.LoseLifeEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.parse.Parser;

/// Parser for damage and life effects in oracle text.
public final class DamageParser {

    private DamageParser() {}

    /// Parses a player reference.
    private static final Parser<PlayerReference> PLAYER_REF = anyOf(
            word("Target player").thenReturn(PlayerReference.TARGET_PLAYER),
            word("target player").thenReturn(PlayerReference.TARGET_PLAYER),
            word("Each player").thenReturn(PlayerReference.EACH_PLAYER),
            word("each player").thenReturn(PlayerReference.EACH_PLAYER),
            word("Each opponent").thenReturn(PlayerReference.EACH_OPPONENT),
            word("each opponent").thenReturn(PlayerReference.EACH_OPPONENT),
            word("that player").thenReturn(PlayerReference.THAT_PLAYER));

    /// Parses "Deal 3 damage to any target." or "Deal X damage to target creature."
    ///
    /// Pattern: "Deal" amount "damage to" target ["."]
    public static final Parser<DealDamageEffect> DEAL_DAMAGE_EFFECT = word("Deal")
            .then(sequence(
                    AmountParser.AMOUNT, string("damage to").then(ReferenceParser.SUBJECT), DealDamageEffect::new))
            .optionallyFollowedBy(".");

    /// Parses "You gain 3 life." or "Target player gains 5 life."
    ///
    /// Pattern: `[Player] "gain"/"gains" amount "life" ["."]`
    public static final Parser<GainLifeEffect> GAIN_LIFE_EFFECT = anyOf(
                    // "You gain 3 life"
                    word("You")
                            .then(word("gain"))
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
                    word("You")
                            .then(word("lose"))
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
