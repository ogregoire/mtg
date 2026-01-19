package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.PutOnLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.LibraryPosition;

/// Assertion class for PutOnLibraryEffect.
public class PutOnLibraryEffectAssert extends AbstractAssert<PutOnLibraryEffectAssert, PutOnLibraryEffect> {

    protected PutOnLibraryEffectAssert(PutOnLibraryEffect actual) {
        super(actual, PutOnLibraryEffectAssert.class);
    }

    public static PutOnLibraryEffectAssert assertThat(PutOnLibraryEffect actual) {
        return new PutOnLibraryEffectAssert(actual);
    }

    /// Returns an assertion on the subject.
    public SubjectAssert subject() {
        isNotNull();
        return new SubjectAssert(actual.subject());
    }

    /// Verifies that the position is TOP.
    public PutOnLibraryEffectAssert isTop() {
        isNotNull();
        if (actual.position() != LibraryPosition.TOP) {
            failWithMessage("Expected position to be TOP but was <%s>", actual.position());
        }
        return this;
    }

    /// Verifies that the position is BOTTOM.
    public PutOnLibraryEffectAssert isBottom() {
        isNotNull();
        if (actual.position() != LibraryPosition.BOTTOM) {
            failWithMessage("Expected position to be BOTTOM but was <%s>", actual.position());
        }
        return this;
    }

    /// Verifies that the position equals the expected value.
    public PutOnLibraryEffectAssert hasPosition(LibraryPosition expected) {
        isNotNull();
        if (actual.position() != expected) {
            failWithMessage("Expected position to be <%s> but was <%s>", expected, actual.position());
        }
        return this;
    }
}
