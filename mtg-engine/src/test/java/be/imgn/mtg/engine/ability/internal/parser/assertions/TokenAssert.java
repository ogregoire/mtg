package be.imgn.mtg.engine.ability.internal.parser.assertions;

import java.util.Arrays;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.CreateTokenEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.PowerToughness;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Assertion class for CreateTokenEffect.Token.
public class TokenAssert extends AbstractAssert<TokenAssert, CreateTokenEffect.Token> {

    protected TokenAssert(CreateTokenEffect.Token actual) {
        super(actual, TokenAssert.class);
    }

    public static TokenAssert assertThat(CreateTokenEffect.Token actual) {
        return new TokenAssert(actual);
    }

    // Identity assertions

    /// Verifies that the token has the expected name.
    public TokenAssert hasName(String expected) {
        isNotNull();
        if (!expected.equals(actual.name())) {
            failWithMessage("Expected name to be <%s> but was <%s>", expected, actual.name());
        }
        return this;
    }

    /// Verifies that the token has no name.
    public TokenAssert hasNoName() {
        isNotNull();
        if (actual.name() != null) {
            failWithMessage("Expected no name but had <%s>", actual.name());
        }
        return this;
    }

    /// Verifies that the token amount equals the expected value.
    public TokenAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the token amount is variable (X).
    public TokenAssert hasVariableAmount() {
        isNotNull();
        if (!(actual.amount() instanceof Amount.XValue)) {
            failWithMessage("Expected variable amount (X) but was <%s>", actual.amount());
        }
        return this;
    }

    // Supertype assertions

    /// Verifies that the token has exactly the expected supertypes.
    public TokenAssert hasSupertypes(Supertype... expected) {
        isNotNull();
        var expectedSupertypes = Supertypes.of(expected);
        if (!actual.supertypes().equals(expectedSupertypes)) {
            failWithMessage("Expected supertypes to be <%s> but was <%s>", expectedSupertypes, actual.supertypes());
        }
        return this;
    }

    /// Verifies that the token has no supertypes.
    public TokenAssert hasNoSupertypes() {
        isNotNull();
        if (!actual.supertypes().isEmpty()) {
            failWithMessage("Expected no supertypes but had <%s>", actual.supertypes());
        }
        return this;
    }

    /// Verifies that the token is legendary.
    public TokenAssert isLegendary() {
        return hasSupertypes(Supertype.LEGENDARY);
    }

    // Power/Toughness assertions

    /// Verifies that the token has the expected exact power and toughness.
    public TokenAssert hasPowerAndToughness(int power, int toughness) {
        isNotNull();
        var expected = PowerToughness.of(power, toughness);
        if (actual.powerToughness() == null) {
            failWithMessage("Expected power/toughness to be <%d/%d> but token has no P/T", power, toughness);
        } else if (!actual.powerToughness().equals(expected)) {
            failWithMessage(
                    "Expected power/toughness to be <%s> but was <%s>",
                    formatPT(expected), formatPT(actual.powerToughness()));
        }
        return this;
    }

    /// Verifies that the token has variable (X/X) power and toughness.
    public TokenAssert hasVariablePowerAndToughness() {
        isNotNull();
        if (actual.powerToughness() == null) {
            failWithMessage("Expected variable power/toughness but token has no P/T");
        } else if (!(actual.powerToughness().power() instanceof Amount.XValue)
                || !(actual.powerToughness().toughness() instanceof Amount.XValue)) {
            failWithMessage("Expected variable power/toughness (X/X) but was <%s>", formatPT(actual.powerToughness()));
        }
        return this;
    }

    /// Verifies that the token has no power/toughness (is not a creature).
    public TokenAssert hasNoPowerToughness() {
        isNotNull();
        if (actual.powerToughness() != null) {
            failWithMessage("Expected no power/toughness but had <%s>", formatPT(actual.powerToughness()));
        }
        return this;
    }

    /// Verifies that this is a creature token (has power and toughness).
    public TokenAssert isCreature() {
        isNotNull();
        if (!actual.isCreature()) {
            failWithMessage("Expected a creature token but had no power/toughness");
        }
        return this;
    }

    /// Verifies that this is not a creature token (has no power and toughness).
    public TokenAssert isNotCreature() {
        isNotNull();
        var pt = actual.powerToughness();
        if (pt != null) {
            failWithMessage("Expected a non-creature token but had power/toughness <%s>", formatPT(pt));
        }
        return this;
    }

    private static String formatPT(PowerToughness pt) {
        return formatAmount(pt.power()) + "/" + formatAmount(pt.toughness());
    }

    private static String formatAmount(Amount amount) {
        if (amount instanceof Amount.Exact exact) {
            return String.valueOf(exact.value());
        } else if (amount instanceof Amount.XValue) {
            return "X";
        } else {
            return amount.toString();
        }
    }

    // Color assertions

    /// Verifies that the token has exactly the expected colors.
    public TokenAssert hasColors(Color... expected) {
        isNotNull();
        var expectedColors = Colors.of(expected);
        if (!actual.colors().equals(expectedColors)) {
            failWithMessage("Expected colors to be <%s> but was <%s>", expectedColors, actual.colors());
        }
        return this;
    }

    /// Verifies that the token is colorless.
    public TokenAssert isColorless() {
        isNotNull();
        if (!actual.colors().isColorless()) {
            failWithMessage("Expected token to be colorless but had colors <%s>", actual.colors());
        }
        return this;
    }

    /// Verifies that the token has all five colors (WUBRG).
    public TokenAssert isAllColors() {
        isNotNull();
        var allColors = Colors.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);
        if (!actual.colors().equals(allColors)) {
            failWithMessage("Expected token to have all colors but had <%s>", actual.colors());
        }
        return this;
    }

    // Type assertions

    /// Verifies that the token has exactly the expected card types.
    public TokenAssert hasTypes(Type... expected) {
        isNotNull();
        var expectedTypes = Types.of(expected);
        if (!actual.types().equals(expectedTypes)) {
            failWithMessage("Expected types to be <%s> but was <%s>", expectedTypes, actual.types());
        }
        return this;
    }

    /// Verifies that the token has exactly the expected subtypes.
    public TokenAssert hasSubtypes(Subtype... expected) {
        isNotNull();
        var expectedSubtypes = Subtypes.of(expected);
        if (!actual.subtypes().equals(expectedSubtypes)) {
            failWithMessage("Expected subtypes to be <%s> but was <%s>", expectedSubtypes, actual.subtypes());
        }
        return this;
    }

    // Ability assertions

    /// Verifies that the token has exactly the expected abilities.
    public TokenAssert hasAbilities(String... expected) {
        isNotNull();
        var expectedList = Arrays.asList(expected);
        if (!actual.abilities().equals(expectedList)) {
            failWithMessage("Expected abilities to be <%s> but was <%s>", expectedList, actual.abilities());
        }
        return this;
    }

    /// Verifies that the token has no abilities.
    public TokenAssert hasNoAbilities() {
        isNotNull();
        if (!actual.abilities().isEmpty()) {
            failWithMessage("Expected no abilities but had <%s>", actual.abilities());
        }
        return this;
    }
}
