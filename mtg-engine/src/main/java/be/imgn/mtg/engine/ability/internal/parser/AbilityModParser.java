package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.single;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Optional;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.effect.GainAbilityEffect;
import be.imgn.mtg.engine.effect.ModifyPowerToughnessEffect;

/// Parser for ability modification effects in oracle text.
public final class AbilityModParser {

    private AbilityModParser() {}

    /// Parses common keyword abilities.
    private static final Parser<String> KEYWORD_ABILITY = anyOf(
            word("flying").thenReturn("flying"),
            string("first strike").thenReturn("first strike"),
            string("double strike").thenReturn("double strike"),
            word("deathtouch").thenReturn("deathtouch"),
            word("haste").thenReturn("haste"),
            word("hexproof").thenReturn("hexproof"),
            word("indestructible").thenReturn("indestructible"),
            word("lifelink").thenReturn("lifelink"),
            word("menace").thenReturn("menace"),
            word("reach").thenReturn("reach"),
            word("trample").thenReturn("trample"),
            word("vigilance").thenReturn("vigilance"),
            word("defender").thenReturn("defender"),
            word("flash").thenReturn("flash"),
            word("shroud").thenReturn("shroud"),
            string("protection from")
                    .then(anyOf(
                            word("black"),
                            word("blue"),
                            word("green"),
                            word("red"),
                            word("white"),
                            word("everything"),
                            word("creatures")))
                    .map(color -> "protection from " + color));

    /// Parses "Target creature gains flying until end of turn."
    ///
    /// Pattern: subject "gains" ability ["until end of turn"] ["."]
    public static final Parser<GainAbilityEffect> GAIN_ABILITY_EFFECT = sequence(
                    ReferenceParser.SUBJECT,
                    word("gains").then(KEYWORD_ABILITY),
                    (subject, ability) -> new GainAbilityEffect(subject, ability, Optional.empty()))
            .optionallyFollowedBy(
                    DurationParser.DURATION,
                    (effect, duration) ->
                            new GainAbilityEffect(effect.subject(), effect.ability(), Optional.of(duration)))
            .optionallyFollowedBy(".");

    /// Parses a sign (+/-) followed by digits.
    private static final Parser<Integer> SIGNED_NUMBER = sequence(
            single(CharPredicate.is('+').or('-'), "+/-").map(String::valueOf),
            digits(),
            (sign, num) -> Integer.parseInt(sign + num));

    /// Parses a P/T modification like "+2/+2", "-1/-1", "+3/+0".
    private static final Parser<int[]> PT_MODIFICATION = sequence(
            SIGNED_NUMBER, string("/").then(SIGNED_NUMBER), (power, toughness) -> new int[] {power, toughness});

    /// Parses "Target creature gets +2/+2 until end of turn."
    ///
    /// Pattern: subject "gets" P/T ["until end of turn"] ["."]
    public static final Parser<ModifyPowerToughnessEffect> MODIFY_PT_EFFECT = sequence(
                    ReferenceParser.SUBJECT,
                    word("gets").then(PT_MODIFICATION),
                    (subject, pt) -> new ModifyPowerToughnessEffect(subject, pt[0], pt[1], Optional.empty()))
            .optionallyFollowedBy(
                    DurationParser.DURATION,
                    (effect, duration) -> new ModifyPowerToughnessEffect(
                            effect.subject(), effect.powerMod(), effect.toughnessMod(), Optional.of(duration)))
            .optionallyFollowedBy(".");
}
