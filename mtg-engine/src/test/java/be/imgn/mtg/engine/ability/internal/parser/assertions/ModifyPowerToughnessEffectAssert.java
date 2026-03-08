package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;
import be.imgn.mtg.engine.effect.ModifyPowerToughnessEffect;

/// Assertion class for ModifyPowerToughnessEffect.
public class ModifyPowerToughnessEffectAssert
        extends AbstractAssert<ModifyPowerToughnessEffectAssert, ModifyPowerToughnessEffect> {

    protected ModifyPowerToughnessEffectAssert(ModifyPowerToughnessEffect actual) {
        super(actual, ModifyPowerToughnessEffectAssert.class);
    }

    public static ModifyPowerToughnessEffectAssert assertThat(ModifyPowerToughnessEffect actual) {
        return new ModifyPowerToughnessEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }

    /// Verifies that the power modification equals the expected value.
    public ModifyPowerToughnessEffectAssert hasPowerMod(int expected) {
        isNotNull();
        if (actual.powerMod() != expected) {
            failWithMessage("Expected power modification to be <%d> but was <%d>", expected, actual.powerMod());
        }
        return this;
    }

    /// Verifies that the toughness modification equals the expected value.
    public ModifyPowerToughnessEffectAssert hasToughnessMod(int expected) {
        isNotNull();
        if (actual.toughnessMod() != expected) {
            failWithMessage("Expected toughness modification to be <%d> but was <%d>", expected, actual.toughnessMod());
        }
        return this;
    }

    /// Verifies that the modification equals the expected values.
    public ModifyPowerToughnessEffectAssert hasModification(int powerMod, int toughnessMod) {
        return hasPowerMod(powerMod).hasToughnessMod(toughnessMod);
    }

    /// Verifies that the duration is permanent (no duration specified).
    public ModifyPowerToughnessEffectAssert isPermanent() {
        isNotNull();
        if (actual.duration().isPresent()) {
            failWithMessage(
                    "Expected duration to be permanent but was <%s>",
                    actual.duration().get());
        }
        return this;
    }

    /// Verifies that the duration is until end of turn.
    public ModifyPowerToughnessEffectAssert isUntilEndOfTurn() {
        isNotNull();
        if (actual.duration().isEmpty()) {
            failWithMessage("Expected duration to be until end of turn but was permanent");
            return this;
        }
        if (!(actual.duration().get() instanceof Duration.UntilEndOfTurn)) {
            failWithMessage(
                    "Expected duration to be until end of turn but was <%s>",
                    actual.duration().get().getClass().getSimpleName());
        }
        return this;
    }

    /// Verifies that the duration is until your next turn.
    public ModifyPowerToughnessEffectAssert isUntilYourNextTurn() {
        isNotNull();
        if (actual.duration().isEmpty()) {
            failWithMessage("Expected duration to be until your next turn but was permanent");
            return this;
        }
        if (!(actual.duration().get() instanceof Duration.UntilYourNextTurn)) {
            failWithMessage(
                    "Expected duration to be until your next turn but was <%s>",
                    actual.duration().get().getClass().getSimpleName());
        }
        return this;
    }

    /// Verifies that the duration equals the expected Duration.
    public ModifyPowerToughnessEffectAssert hasDuration(Duration expected) {
        isNotNull();
        if (actual.duration().isEmpty()) {
            failWithMessage("Expected duration to be <%s> but was permanent", expected);
            return this;
        }
        if (!actual.duration().get().equals(expected)) {
            failWithMessage(
                    "Expected duration to be <%s> but was <%s>",
                    expected, actual.duration().get());
        }
        return this;
    }
}
