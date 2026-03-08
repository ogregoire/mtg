package be.imgn.mtg.engine.effect;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.PowerToughness;
import be.imgn.mtg.engine.ability.internal.parser.selector.PredefinedTokenType;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;

/// An effect that creates tokens.
public sealed interface CreateTokenEffect extends Effect {

    /// How many tokens to create.
    Amount amount();

    /// Creates a token with explicit characteristics.
    ///
    /// This unified record handles both creature tokens (with P/T) and non-creature tokens (without P/T).
    /// Creature tokens have powerToughness present; non-creature tokens have it null.
    ///
    /// @param name the token's name, or null if unnamed (e.g., "Boo")
    /// @param amount how many tokens to create
    /// @param supertypes the token's supertypes (e.g., legendary)
    /// @param powerToughness the token's power and toughness, null for non-creature tokens
    /// @param colors the token's colors
    /// @param types the token's card types (e.g., Creature, Artifact Creature, Enchantment)
    /// @param subtypes the token's subtypes (e.g., Soldier, Cat Dragon, Aura Curse)
    /// @param abilities keyword abilities the token has (e.g., "flying", "haste")
    record Token(
            @Nullable String name,
            Amount amount,
            Supertypes supertypes,
            @Nullable PowerToughness powerToughness,
            Colors colors,
            Types types,
            Subtypes subtypes,
            List<String> abilities)
            implements CreateTokenEffect {

        /// Returns true if this is a creature token (has power and toughness).
        public boolean isCreature() {
            return powerToughness != null;
        }
    }

    /// Creates a predefined token as defined in rule 111.10.
    ///
    /// Predefined tokens have specific characteristics that are automatically applied.
    /// This includes artifact tokens (Treasure, Food, etc.), enchantment tokens (Shard),
    /// Role tokens (Cursed, Monster, etc.), creature tokens (Walker), and special tokens (Incubator).
    ///
    /// @param amount how many tokens to create
    /// @param type the predefined token type
    record Predefined(Amount amount, PredefinedTokenType type) implements CreateTokenEffect {}

    /// Creates a token by referencing an existing card name, as defined in rule 111.11.
    ///
    /// The token's characteristics are determined by looking up the named card in the Oracle
    /// card reference. This is used for effects like "Create a Tarmogoyf token."
    ///
    /// @param amount how many tokens to create
    /// @param cardName the name of the card to copy characteristics from
    record ByCardName(Amount amount, String cardName) implements CreateTokenEffect {}
}
