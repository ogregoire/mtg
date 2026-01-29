package be.imgn.mtg.engine.object;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.cost.Costs;

/// A game object with characteristics ({@mtg.rule 109}).
///
/// TypedObject represents game objects that have the full set of characteristics:
/// name, mana cost, color, card type, subtype, supertype, abilities, power,
/// toughness, and loyalty. This includes cards, permanents, spells, tokens,
/// and card copies.
///
/// Objects that exist in the game but do not have the full set of characteristics
/// (such as [AbilityOnStack] and [Emblem]) extend [GameObject] directly instead.
///
/// @see Card
/// @see Permanent
/// @see Spell
/// @see Token
/// @see CardCopy
public sealed interface TypedObject extends GameObject permits Card, Permanent, Spell, Token, CardCopy {

    /// Returns the name of this object.
    ///
    /// @return the name, never null
    String name();

    /// Returns the costs to cast or activate this object.
    ///
    /// @return the costs, never null (may be empty)
    Costs costs();

    /// Returns the mana value of this object.
    ///
    /// The mana value is the total amount of mana in the mana cost, regardless of color.
    ///
    /// @return the mana value
    Value manaValue();

    /// Returns the colors of this object.
    ///
    /// A card's colors are determined by its mana cost and/or color indicator.
    ///
    /// @return the colors, never null (may be empty for colorless)
    Colors colors();

    /// Returns the types of this object.
    ///
    /// Types include creature, instant, sorcery, artifact, enchantment, land, and planeswalker.
    ///
    /// @return the types, never null
    Types types();

    /// Returns the supertypes of this object.
    ///
    /// Supertypes include basic, legendary, snow, and world.
    ///
    /// @return the supertypes, never null (may be empty)
    Supertypes supertypes();

    /// Returns the subtypes of this object.
    ///
    /// Subtypes include creature types (Human, Goblin), land types (Forest, Island),
    /// spell types (Arcane, Trap), and other subtypes.
    ///
    /// @return the subtypes, never null (may be empty)
    Subtypes subtypes();

    /// Returns the abilities of this object.
    ///
    /// @return the abilities, never null (may be empty)
    Abilities abilities();

    /// Returns the power of this object (for creatures).
    ///
    /// @return the power, or null if not a creature
    @Nullable
    Value power();

    /// Returns the toughness of this object (for creatures).
    ///
    /// @return the toughness, or null if not a creature
    @Nullable
    Value toughness();

    /// Returns the loyalty of this object (for planeswalkers).
    ///
    /// @return the loyalty, or null if not a planeswalker
    @Nullable
    Value loyalty();

    /// Base builder interface for all typed game objects.
    ///
    /// Provides fluent methods for setting common characteristics shared by all typed objects.
    ///
    /// @param <T> the type of game object being built
    /// @param <B> the concrete builder type for method chaining
    sealed interface Builder<T extends TypedObject, B extends Builder<T, B>>
            permits Card.Builder, Permanent.Builder, Spell.Builder, Token.Builder, CardCopy.Builder {

        /// Sets the name of the game object.
        ///
        /// @param name the name
        /// @return this builder
        B name(String name);

        /// Sets all colors of the game object, replacing any existing colors.
        ///
        /// @param colors the colors
        /// @return this builder
        B colors(Colors colors);

        /// Adds a color to the game object.
        ///
        /// @param color the color to add
        /// @return this builder
        B addColor(Color color);

        /// Sets a single color, replacing any existing colors.
        ///
        /// @param color the color
        /// @return this builder
        B color(Color color);

        /// Sets colors from varargs, replacing any existing colors.
        ///
        /// @param colors the colors
        /// @return this builder
        B colors(Color... colors);

        /// Adds multiple colors to the game object.
        ///
        /// @param colors the colors to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addColors(Color... colors) {
            for (var color : colors) {
                addColor(color);
            }
            return (B) this;
        }

        /// Sets all types, replacing any existing types.
        ///
        /// @param types the types
        /// @return this builder
        B types(Types types);

        /// Adds a type to the game object.
        ///
        /// @param type the type to add
        /// @return this builder
        B addType(Type type);

        /// Sets a single type, replacing any existing types.
        ///
        /// @param type the type
        /// @return this builder
        B type(Type type);

        /// Sets types from varargs, replacing any existing types.
        ///
        /// @param types the types
        /// @return this builder
        B types(Type... types);

        /// Adds multiple types to the game object.
        ///
        /// @param types the types to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addTypes(Type... types) {
            for (var type : types) {
                addType(type);
            }
            return (B) this;
        }

        /// Sets all supertypes, replacing any existing supertypes.
        ///
        /// @param supertypes the supertypes
        /// @return this builder
        B supertypes(Supertypes supertypes);

        /// Adds a supertype to the game object.
        ///
        /// @param supertype the supertype to add
        /// @return this builder
        B addSupertype(Supertype supertype);

        /// Sets a single supertype, replacing any existing supertypes.
        ///
        /// @param supertype the supertype
        /// @return this builder
        B supertype(Supertype supertype);

        /// Sets supertypes from varargs, replacing any existing supertypes.
        ///
        /// @param supertypes the supertypes
        /// @return this builder
        B supertypes(Supertype... supertypes);

        /// Adds multiple supertypes to the game object.
        ///
        /// @param supertypes the supertypes to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addSupertypes(Supertype... supertypes) {
            for (var supertype : supertypes) {
                addSupertype(supertype);
            }
            return (B) this;
        }

        /// Sets all subtypes, replacing any existing subtypes.
        ///
        /// @param subtypes the subtypes
        /// @return this builder
        B subtypes(Subtypes subtypes);

        /// Adds a subtype to the game object.
        ///
        /// @param subtype the subtype to add
        /// @return this builder
        B addSubtype(Subtype subtype);

        /// Sets a single subtype, replacing any existing subtypes.
        ///
        /// @param subtype the subtype
        /// @return this builder
        B subtype(Subtype subtype);

        /// Sets subtypes from varargs, replacing any existing subtypes.
        ///
        /// @param subtypes the subtypes
        /// @return this builder
        B subtypes(Subtype... subtypes);

        /// Adds multiple subtypes to the game object.
        ///
        /// @param subtypes the subtypes to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addSubtypes(Subtype... subtypes) {
            for (var subtype : subtypes) {
                addSubtype(subtype);
            }
            return (B) this;
        }

        /// Sets all abilities, replacing any existing abilities.
        ///
        /// @param abilities the abilities
        /// @return this builder
        B abilities(Abilities abilities);

        /// Adds an ability to the game object.
        ///
        /// @param ability the ability to add
        /// @return this builder
        B addAbility(Ability ability);

        /// Sets a single ability, replacing any existing abilities.
        ///
        /// @param ability the ability
        /// @return this builder
        B ability(Ability ability);

        /// Sets abilities from varargs, replacing any existing abilities.
        ///
        /// @param abilities the abilities
        /// @return this builder
        B abilities(Ability... abilities);

        /// Adds multiple abilities to the game object.
        ///
        /// @param abilities the abilities to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addAbilities(Ability... abilities) {
            for (var ability : abilities) {
                addAbility(ability);
            }
            return (B) this;
        }

        /// Sets the power (for creatures).
        ///
        /// @param power the power value, or null
        /// @return this builder
        B power(@Nullable Value power);

        /// Sets the toughness (for creatures).
        ///
        /// @param toughness the toughness value, or null
        /// @return this builder
        B toughness(@Nullable Value toughness);

        /// Sets the loyalty (for planeswalkers).
        ///
        /// @param loyalty the loyalty value, or null
        /// @return this builder
        B loyalty(@Nullable Value loyalty);

        /// Sets all costs, replacing any existing costs.
        ///
        /// @param costs the costs
        /// @return this builder
        B costs(Costs costs);

        /// Adds a cost to the game object.
        ///
        /// @param cost the cost to add
        /// @return this builder
        B addCost(Cost cost);

        /// Sets a single cost, replacing any existing costs.
        ///
        /// @param cost the cost
        /// @return this builder
        B cost(Cost cost);

        /// Sets costs from varargs, replacing any existing costs.
        ///
        /// @param costs the costs
        /// @return this builder
        B costs(Cost... costs);

        /// Adds multiple costs to the game object.
        ///
        /// @param costs the costs to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        default B addCosts(Cost... costs) {
            for (var cost : costs) {
                addCost(cost);
            }
            return (B) this;
        }

        /// Builds and returns the game object.
        ///
        /// @return the constructed game object
        T build();
    }
}
