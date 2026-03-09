package be.imgn.mtg.engine.ability.internal.parser;

import java.util.List;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.SpellAbility;

/// Parser for spell abilities in oracle text ({@mtg.rule 113.3a}).
///
/// Spell abilities are the instructions on an instant or sorcery spell.
/// Unlike activated abilities, they have no cost prefix — the entire text
/// describes the effect.
public final class SpellAbilityParser {

    private SpellAbilityParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses a spell ability from a single line of oracle text.
    public static final Parser<SpellAbility> SPELL_ABILITY =
            EffectParser.EFFECT.map(effect -> new SpellAbility(new AbilityId(), "", List.of(effect)));

    /// Parses a spell ability from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed spell ability
    public static SpellAbility parse(String oracleText) {
        var ability = SPELL_ABILITY.parseSkipping(WHITESPACE, oracleText);
        return new SpellAbility(ability.id(), oracleText, ability.effects());
    }
}
