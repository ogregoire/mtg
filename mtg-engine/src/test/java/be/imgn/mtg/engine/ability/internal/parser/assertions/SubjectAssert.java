package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.TypeMatcher;

/// Assertion class for Subject.
public class SubjectAssert extends AbstractAssert<SubjectAssert, Subject> {

    protected SubjectAssert(Subject actual) {
        super(actual, SubjectAssert.class);
    }

    public static SubjectAssert assertThat(Subject actual) {
        return new SubjectAssert(actual);
    }

    /// Verifies that the subject is a Select with an ObjectSelector and returns a specialized assert.
    public ObjectSelectorAssert isSelect() {
        isNotNull();
        if (!(actual instanceof Subject.Select select)) {
            throw failure(
                    "Expected subject to be a Select but was <%s>",
                    actual.getClass().getSimpleName());
        }
        if (!(select.selector() instanceof ObjectSelector objectSelector)) {
            throw failure(
                    "Expected selector to be an ObjectSelector but was <%s>",
                    select.selector().getClass().getSimpleName());
        }
        return new ObjectSelectorAssert(objectSelector);
    }

    /// Verifies that the subject is a Pronoun with the expected type.
    public SubjectAssert isPronoun(PronounType expected) {
        isNotNull();
        if (!(actual instanceof Subject.Pronoun pronoun)) {
            failWithMessage(
                    "Expected subject to be a Pronoun but was <%s>",
                    actual.getClass().getSimpleName());
            return this;
        }
        if (!pronoun.pronoun().equals(expected)) {
            failWithMessage("Expected pronoun to be <%s> but was <%s>", expected, pronoun.pronoun());
        }
        return this;
    }

    /// Verifies that the subject is the "it" pronoun.
    public SubjectAssert isIt() {
        return isPronoun(PronounType.IT);
    }

    /// Verifies that the subject is a ThatObject.
    public SubjectAssert isThatObject() {
        isNotNull();
        if (!(actual instanceof Subject.ThatObject)) {
            failWithMessage(
                    "Expected subject to be a ThatObject but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return this;
    }

    /// Assertion class for ObjectSelector.
    public static class ObjectSelectorAssert extends AbstractAssert<ObjectSelectorAssert, ObjectSelector> {

        protected ObjectSelectorAssert(ObjectSelector actual) {
            super(actual, ObjectSelectorAssert.class);
        }

        /// Verifies that the selector is a target.
        public ObjectSelectorAssert isTarget() {
            isNotNull();
            if (!actual.qualifiers().stream().anyMatch(q -> q instanceof Qualifier.Target)) {
                failWithMessage("Expected selector to be a target but was not");
            }
            return this;
        }

        /// Verifies that the selector is not a target.
        public ObjectSelectorAssert isNotTarget() {
            isNotNull();
            if (actual.qualifiers().stream().anyMatch(q -> q instanceof Qualifier.Target)) {
                failWithMessage("Expected selector to not be a target but was");
            }
            return this;
        }

        /// Verifies that the selector matches the given type.
        public ObjectSelectorAssert hasTypeMatcher(TypeMatcher expected) {
            isNotNull();
            if (!actual.typeMatcher().equals(expected)) {
                failWithMessage("Expected type matcher to be <%s> but was <%s>", expected, actual.typeMatcher());
            }
            return this;
        }

        /// Verifies that the selector matches a single type.
        public ObjectSelectorAssert hasSingleType(Type expected) {
            return hasTypeMatcher(new TypeMatcher.Single(expected));
        }

        /// Verifies that the selector matches any permanent.
        public ObjectSelectorAssert matchesAnyPermanent() {
            return hasTypeMatcher(new TypeMatcher.Permanent());
        }

        /// Verifies that the selector matches any spell.
        public ObjectSelectorAssert matchesAnySpell() {
            return hasTypeMatcher(new TypeMatcher.Spell());
        }
    }
}
