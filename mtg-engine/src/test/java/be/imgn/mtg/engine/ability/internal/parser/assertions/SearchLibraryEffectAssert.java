package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.effect.SearchLibraryEffect;
import be.imgn.mtg.engine.selector.TypeMatcher;

/// Assertion class for SearchLibraryEffect.
public class SearchLibraryEffectAssert extends AbstractAssert<SearchLibraryEffectAssert, SearchLibraryEffect> {

    protected SearchLibraryEffectAssert(SearchLibraryEffect actual) {
        super(actual, SearchLibraryEffectAssert.class);
    }

    public static SearchLibraryEffectAssert assertThat(SearchLibraryEffect actual) {
        return new SearchLibraryEffectAssert(actual);
    }

    /// Verifies that there is no type restriction (searches for any card).
    public SearchLibraryEffectAssert hasNoTypeRestriction() {
        isNotNull();
        if (actual.cardType().isPresent()) {
            failWithMessage(
                    "Expected no type restriction but had <%s>",
                    actual.cardType().get());
        }
        return this;
    }

    /// Verifies that the card type restriction equals the expected TypeMatcher.
    public SearchLibraryEffectAssert hasCardType(TypeMatcher expected) {
        isNotNull();
        if (actual.cardType().isEmpty()) {
            failWithMessage("Expected card type to be <%s> but was unrestricted", expected);
            return this;
        }
        if (!actual.cardType().get().equals(expected)) {
            failWithMessage(
                    "Expected card type to be <%s> but was <%s>",
                    expected, actual.cardType().get());
        }
        return this;
    }
}
