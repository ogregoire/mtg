package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.CardCopy;

/// Assertion class for CardCopy.
public class CardCopyAssert extends AbstractGameObjectAssert<CardCopyAssert, CardCopy> {

    protected CardCopyAssert(CardCopy actual) {
        super(actual, CardCopyAssert.class);
    }

    public static CardCopyAssert assertThat(CardCopy actual) {
        return new CardCopyAssert(actual);
    }

    public CardCopyAssert hasOriginal(Card expected) {
        isNotNull();
        if (!actual.original().equals(expected)) {
            failWithMessage("Expected original card to be <%s> but was <%s>", expected, actual.original());
        }
        return this;
    }
}
