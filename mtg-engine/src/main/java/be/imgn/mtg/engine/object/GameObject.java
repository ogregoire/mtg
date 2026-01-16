package be.imgn.mtg.engine.object;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Abilities;
import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.Costs;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.characteristics.Value;

/// Base interface for all game objects in Magic.
public sealed interface GameObject extends Owned, Controlled
        permits Card, Permanent, Spell, Token, CardCopy, AbilityOnStack {

    /// Returns the name of this object.
    String name();

    /// Returns the costs to cast or activate this object.
    Costs costs();

    /// Returns the mana value of this object.
    Value manaValue();

    /// Returns the colors of this object.
    Colors colors();

    /// Returns the types of this object.
    Types types();

    /// Returns the supertypes of this object.
    Supertypes supertypes();

    /// Returns the subtypes of this object.
    Subtypes subtypes();

    /// Returns the abilities of this object.
    Abilities abilities();

    /// Returns the power of this object (for creatures).
    @Nullable
    Value power();

    /// Returns the toughness of this object (for creatures).
    @Nullable
    Value toughness();

    /// Returns the loyalty of this object (for planeswalkers).
    @Nullable
    Value loyalty();

    /// Base builder interface for all game objects.
    sealed interface Builder<T extends GameObject, B extends Builder<T, B>>
            permits Card.Builder,
                    Permanent.Builder,
                    Spell.Builder,
                    Token.Builder,
                    CardCopy.Builder,
                    AbilityOnStack.Builder {

        B name(String name);

        B colors(Colors colors);

        B addColor(Color color);

        B color(Color color);

        B colors(Color... colors);

        @SuppressWarnings("unchecked")
        default B addColors(Color... colors) {
            for (var color : colors) {
                addColor(color);
            }
            return (B) this;
        }

        B types(Types types);

        B addType(Type type);

        B type(Type type);

        B types(Type... types);

        @SuppressWarnings("unchecked")
        default B addTypes(Type... types) {
            for (var type : types) {
                addType(type);
            }
            return (B) this;
        }

        B supertypes(Supertypes supertypes);

        B addSupertype(Supertype supertype);

        B supertype(Supertype supertype);

        B supertypes(Supertype... supertypes);

        @SuppressWarnings("unchecked")
        default B addSupertypes(Supertype... supertypes) {
            for (var supertype : supertypes) {
                addSupertype(supertype);
            }
            return (B) this;
        }

        B subtypes(Subtypes subtypes);

        B addSubtype(Subtype subtype);

        B subtype(Subtype subtype);

        B subtypes(Subtype... subtypes);

        @SuppressWarnings("unchecked")
        default B addSubtypes(Subtype... subtypes) {
            for (var subtype : subtypes) {
                addSubtype(subtype);
            }
            return (B) this;
        }

        B abilities(Abilities abilities);

        B addAbility(Ability ability);

        B ability(Ability ability);

        B abilities(Ability... abilities);

        @SuppressWarnings("unchecked")
        default B addAbilities(Ability... abilities) {
            for (var ability : abilities) {
                addAbility(ability);
            }
            return (B) this;
        }

        B power(@Nullable Value power);

        B toughness(@Nullable Value toughness);

        B loyalty(@Nullable Value loyalty);

        B costs(Costs costs);

        B addCost(Cost cost);

        B cost(Cost cost);

        B costs(Cost... costs);

        @SuppressWarnings("unchecked")
        default B addCosts(Cost... costs) {
            for (var cost : costs) {
                addCost(cost);
            }
            return (B) this;
        }

        T build();
    }
}
