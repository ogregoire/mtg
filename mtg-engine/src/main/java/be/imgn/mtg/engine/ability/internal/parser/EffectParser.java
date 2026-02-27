package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;
import be.imgn.mtg.engine.mana.internal.ManaParser;

/// Master parser for all MTG effect types.
public final class EffectParser {

    private EffectParser() {}

    /// The skip pattern for whitespace.
    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses any effect type.
    ///
    /// This parser tries all effect parsers in order and returns the first match.
    /// The order is significant - more specific patterns should come before general ones.
    @SuppressWarnings("unchecked")
    public static final Parser<Effect> EFFECT = anyOf(
            // Removal effects
            DestroyParser.DESTROY_EFFECT.map(e -> (Effect) e),
            ExileParser.EXILE_EFFECT.map(e -> (Effect) e),
            SacrificeParser.SACRIFICE_EFFECT.map(e -> (Effect) e),
            ReturnParser.RETURN_TO_HAND_EFFECT.map(e -> (Effect) e),
            ReturnParser.PUT_ON_LIBRARY_EFFECT.map(e -> (Effect) e),
            MillParser.MILL_EFFECT.map(e -> (Effect) e),

            // Damage and life effects
            DamageParser.DEAL_DAMAGE_EFFECT.map(e -> (Effect) e),
            DamageParser.GAIN_LIFE_EFFECT.map(e -> (Effect) e),
            DamageParser.LOSE_LIFE_EFFECT.map(e -> (Effect) e),

            // Card manipulation
            DrawParser.DRAW_EFFECT.map(e -> (Effect) e),
            DrawParser.DISCARD_EFFECT.map(e -> (Effect) e),
            DrawParser.SCRY_EFFECT.map(e -> (Effect) e),
            DrawParser.SEARCH_LIBRARY_EFFECT.map(e -> (Effect) e),

            // Tap/untap
            TapParser.TAP_EFFECT.map(e -> (Effect) e),
            TapParser.UNTAP_EFFECT.map(e -> (Effect) e),

            // Counters
            CounterParser.ADD_COUNTERS_EFFECT.map(e -> (Effect) e),
            CounterParser.REMOVE_COUNTERS_EFFECT.map(e -> (Effect) e),

            // Ability modifications (must come before fight since "Target creature" is common)
            AbilityModParser.GAIN_ABILITY_EFFECT.map(e -> (Effect) e),
            AbilityModParser.MODIFY_PT_EFFECT.map(e -> (Effect) e),

            // Control change
            ControlChangeParser.GAIN_CONTROL_EFFECT.map(e -> (Effect) e),

            // Token creation
            TokenParser.CREATE_TOKEN_EFFECT.map(e -> (Effect) e),

            // Counterspell
            CounterspellParser.COUNTER_SPELL_EFFECT.map(e -> (Effect) e),

            // Fight
            FightParser.FIGHT_EFFECT.map(e -> (Effect) e),

            // Mana
            ManaParser.ADD_MANA_EFFECT.map(e -> (Effect) e));

    /// Parses an effect from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed effect
    public static Effect parse(String oracleText) {
        return EFFECT.parseSkipping(WHITESPACE, oracleText);
    }
}
