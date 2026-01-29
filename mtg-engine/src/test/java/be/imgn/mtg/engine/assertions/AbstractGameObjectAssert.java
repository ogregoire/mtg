package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TypedObject;

/// Base assertion class for all game objects.
public abstract class AbstractGameObjectAssert<
                SELF extends AbstractGameObjectAssert<SELF, ACTUAL>, ACTUAL extends TypedObject>
        extends AbstractObjectAssert<SELF, ACTUAL> {

    protected AbstractGameObjectAssert(ACTUAL actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public SELF hasName(String expected) {
        isNotNull();
        if (!actual.name().equals(expected)) {
            failWithMessage("Expected name to be <%s> but was <%s>", expected, actual.name());
        }
        return myself;
    }

    public SELF hasOwner(Player expected) {
        isNotNull();
        if (!actual.owner().equals(expected)) {
            failWithMessage("Expected owner to be <%s> but was <%s>", expected, actual.owner());
        }
        return myself;
    }

    public SELF hasController(Player expected) {
        isNotNull();
        if (!actual.controller().equals(expected)) {
            failWithMessage("Expected controller to be <%s> but was <%s>", expected, actual.controller());
        }
        return myself;
    }

    public SELF hasManaValue(int expected) {
        isNotNull();
        if (actual.manaValue().value() != expected) {
            failWithMessage(
                    "Expected mana value to be <%d> but was <%d>",
                    expected, actual.manaValue().value());
        }
        return myself;
    }

    // Color assertions

    public SELF hasColor(Color color) {
        isNotNull();
        if (!actual.colors().contains(color)) {
            failWithMessage("Expected to have color <%s> but colors were <%s>", color, actual.colors());
        }
        return myself;
    }

    public SELF doesNotHaveColor(Color color) {
        isNotNull();
        if (actual.colors().contains(color)) {
            failWithMessage("Expected not to have color <%s> but colors were <%s>", color, actual.colors());
        }
        return myself;
    }

    public SELF isColorless() {
        isNotNull();
        if (!actual.colors().isColorless()) {
            failWithMessage("Expected to be colorless but colors were <%s>", actual.colors());
        }
        return myself;
    }

    public SELF isColored() {
        isNotNull();
        if (!actual.colors().isColored()) {
            failWithMessage("Expected to be colored but was colorless");
        }
        return myself;
    }

    public SELF isMonoColored() {
        isNotNull();
        if (!actual.colors().isMonoColored()) {
            failWithMessage("Expected to be mono-colored but colors were <%s>", actual.colors());
        }
        return myself;
    }

    public SELF isMultiColored() {
        isNotNull();
        if (!actual.colors().isMultiColored()) {
            failWithMessage("Expected to be multi-colored but colors were <%s>", actual.colors());
        }
        return myself;
    }

    public SELF isWhite() {
        return hasColor(Color.WHITE);
    }

    public SELF isBlue() {
        return hasColor(Color.BLUE);
    }

    public SELF isBlack() {
        return hasColor(Color.BLACK);
    }

    public SELF isRed() {
        return hasColor(Color.RED);
    }

    public SELF isGreen() {
        return hasColor(Color.GREEN);
    }

    // Type assertions

    public SELF hasType(Type type) {
        isNotNull();
        if (!actual.types().contains(type)) {
            failWithMessage("Expected to have type <%s> but types were <%s>", type, actual.types());
        }
        return myself;
    }

    public SELF doesNotHaveType(Type type) {
        isNotNull();
        if (actual.types().contains(type)) {
            failWithMessage("Expected not to have type <%s> but types were <%s>", type, actual.types());
        }
        return myself;
    }

    public SELF isCreature() {
        isNotNull();
        if (!actual.types().isCreature()) {
            failWithMessage("Expected to be a creature but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isArtifact() {
        isNotNull();
        if (!actual.types().isArtifact()) {
            failWithMessage("Expected to be an artifact but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isEnchantment() {
        isNotNull();
        if (!actual.types().isEnchantment()) {
            failWithMessage("Expected to be an enchantment but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isLand() {
        isNotNull();
        if (!actual.types().isLand()) {
            failWithMessage("Expected to be a land but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isPlaneswalker() {
        isNotNull();
        if (!actual.types().isPlaneswalker()) {
            failWithMessage("Expected to be a planeswalker but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isInstant() {
        isNotNull();
        if (!actual.types().isInstant()) {
            failWithMessage("Expected to be an instant but types were <%s>", actual.types());
        }
        return myself;
    }

    public SELF isSorcery() {
        isNotNull();
        if (!actual.types().isSorcery()) {
            failWithMessage("Expected to be a sorcery but types were <%s>", actual.types());
        }
        return myself;
    }

    // Supertype assertions

    public SELF hasSupertype(Supertype supertype) {
        isNotNull();
        if (!actual.supertypes().contains(supertype)) {
            failWithMessage("Expected to have supertype <%s> but supertypes were <%s>", supertype, actual.supertypes());
        }
        return myself;
    }

    public SELF doesNotHaveSupertype(Supertype supertype) {
        isNotNull();
        if (actual.supertypes().contains(supertype)) {
            failWithMessage(
                    "Expected not to have supertype <%s> but supertypes were <%s>", supertype, actual.supertypes());
        }
        return myself;
    }

    public SELF isLegendary() {
        isNotNull();
        if (!actual.supertypes().isLegendary()) {
            failWithMessage("Expected to be legendary but supertypes were <%s>", actual.supertypes());
        }
        return myself;
    }

    public SELF isBasic() {
        isNotNull();
        if (!actual.supertypes().isBasic()) {
            failWithMessage("Expected to be basic but supertypes were <%s>", actual.supertypes());
        }
        return myself;
    }

    // Subtype assertions

    public SELF hasSubtype(Subtype subtype) {
        isNotNull();
        if (!actual.subtypes().contains(subtype)) {
            failWithMessage("Expected to have subtype <%s> but subtypes were <%s>", subtype, actual.subtypes());
        }
        return myself;
    }

    public SELF doesNotHaveSubtype(Subtype subtype) {
        isNotNull();
        if (actual.subtypes().contains(subtype)) {
            failWithMessage("Expected not to have subtype <%s> but subtypes were <%s>", subtype, actual.subtypes());
        }
        return myself;
    }

    // Power/Toughness assertions

    public SELF hasPower(int expected) {
        isNotNull();
        if (actual.power() == null) {
            failWithMessage("Expected power to be <%d> but was null", expected);
        } else if (actual.power().value() != expected) {
            failWithMessage(
                    "Expected power to be <%d> but was <%d>",
                    expected, actual.power().value());
        }
        return myself;
    }

    public SELF hasToughness(int expected) {
        isNotNull();
        if (actual.toughness() == null) {
            failWithMessage("Expected toughness to be <%d> but was null", expected);
        } else if (actual.toughness().value() != expected) {
            failWithMessage(
                    "Expected toughness to be <%d> but was <%d>",
                    expected, actual.toughness().value());
        }
        return myself;
    }

    public SELF hasLoyalty(int expected) {
        isNotNull();
        if (actual.loyalty() == null) {
            failWithMessage("Expected loyalty to be <%d> but was null", expected);
        } else if (actual.loyalty().value() != expected) {
            failWithMessage(
                    "Expected loyalty to be <%d> but was <%d>",
                    expected, actual.loyalty().value());
        }
        return myself;
    }

    public SELF hasNoPower() {
        isNotNull();
        if (actual.power() != null) {
            failWithMessage("Expected no power but was <%d>", actual.power().value());
        }
        return myself;
    }

    public SELF hasNoToughness() {
        isNotNull();
        if (actual.toughness() != null) {
            failWithMessage(
                    "Expected no toughness but was <%d>", actual.toughness().value());
        }
        return myself;
    }

    public SELF hasNoLoyalty() {
        isNotNull();
        if (actual.loyalty() != null) {
            failWithMessage("Expected no loyalty but was <%d>", actual.loyalty().value());
        }
        return myself;
    }
}
