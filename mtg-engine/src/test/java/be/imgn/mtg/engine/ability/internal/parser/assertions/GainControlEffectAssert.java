package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.GainControlEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;

/// Assertion class for GainControlEffect.
public class GainControlEffectAssert extends AbstractAssert<GainControlEffectAssert, GainControlEffect> {

    protected GainControlEffectAssert(GainControlEffect actual) {
        super(actual, GainControlEffectAssert.class);
    }

    public static GainControlEffectAssert assertThat(GainControlEffect actual) {
        return new GainControlEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }

    /// Verifies that the duration is permanent (no duration specified).
    public GainControlEffectAssert isPermanent() {
        isNotNull();
        if (actual.duration().isPresent()) {
            failWithMessage(
                    "Expected duration to be permanent but was <%s>",
                    actual.duration().get());
        }
        return this;
    }

    /// Verifies that the duration is until end of turn.
    public GainControlEffectAssert isUntilEndOfTurn() {
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
    public GainControlEffectAssert isUntilYourNextTurn() {
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
    public GainControlEffectAssert hasDuration(Duration expected) {
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
