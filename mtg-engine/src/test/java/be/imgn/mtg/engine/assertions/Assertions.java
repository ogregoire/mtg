package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.characteristics.Abilities;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Costs;
import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.CardCopy;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.Token;

/// Entry point for all MTG engine assertions.
public final class Assertions {

    private Assertions() {}

    // Game object assertions

    public static CardAssert assertThat(Card actual) {
        return CardAssert.assertThat(actual);
    }

    public static TokenAssert assertThat(Token actual) {
        return TokenAssert.assertThat(actual);
    }

    public static PermanentAssert assertThat(Permanent actual) {
        return PermanentAssert.assertThat(actual);
    }

    public static SpellAssert assertThat(Spell actual) {
        return SpellAssert.assertThat(actual);
    }

    public static CardCopyAssert assertThat(CardCopy actual) {
        return CardCopyAssert.assertThat(actual);
    }

    public static AbilityOnStackAssert assertThat(AbilityOnStack actual) {
        return AbilityOnStackAssert.assertThat(actual);
    }

    // Characteristic assertions

    public static ColorsAssert assertThat(Colors actual) {
        return ColorsAssert.assertThat(actual);
    }

    public static TypesAssert assertThat(Types actual) {
        return TypesAssert.assertThat(actual);
    }

    public static SupertypesAssert assertThat(Supertypes actual) {
        return SupertypesAssert.assertThat(actual);
    }

    public static SubtypesAssert assertThat(Subtypes actual) {
        return SubtypesAssert.assertThat(actual);
    }

    public static AbilitiesAssert assertThat(Abilities actual) {
        return AbilitiesAssert.assertThat(actual);
    }

    public static CostsAssert assertThat(Costs actual) {
        return CostsAssert.assertThat(actual);
    }

    public static CountersAssert assertThat(Counters actual) {
        return CountersAssert.assertThat(actual);
    }
}
