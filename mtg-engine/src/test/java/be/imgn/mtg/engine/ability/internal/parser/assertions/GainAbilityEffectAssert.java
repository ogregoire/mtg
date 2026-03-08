package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;
import be.imgn.mtg.engine.effect.GainAbilityEffect;

/// Assertion class for GainAbilityEffect.
public class GainAbilityEffectAssert extends AbstractAssert<GainAbilityEffectAssert, GainAbilityEffect> {

    protected GainAbilityEffectAssert(GainAbilityEffect actual) {
        super(actual, GainAbilityEffectAssert.class);
    }

    public static GainAbilityEffectAssert assertThat(GainAbilityEffect actual) {
        return new GainAbilityEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }

    /// Verifies that the ability equals the expected value.
    public GainAbilityEffectAssert hasAbility(String expected) {
        isNotNull();
        if (!actual.ability().equals(expected)) {
            failWithMessage("Expected ability to be <%s> but was <%s>", expected, actual.ability());
        }
        return this;
    }

    /// Verifies that the duration is permanent (no duration specified).
    public GainAbilityEffectAssert isPermanent() {
        isNotNull();
        if (actual.duration().isPresent()) {
            failWithMessage(
                    "Expected duration to be permanent but was <%s>",
                    actual.duration().get());
        }
        return this;
    }

    /// Verifies that the duration is until end of turn.
    public GainAbilityEffectAssert isUntilEndOfTurn() {
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
    public GainAbilityEffectAssert isUntilYourNextTurn() {
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
    public GainAbilityEffectAssert hasDuration(Duration expected) {
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
