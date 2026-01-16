package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.object.Card;

/// Assertion class for Card.
public class CardAssert extends AbstractGameObjectAssert<CardAssert, Card> {

    protected CardAssert(Card actual) {
        super(actual, CardAssert.class);
    }

    public static CardAssert assertThat(Card actual) {
        return new CardAssert(actual);
    }

    public CardAssert hasManaCost() {
        isNotNull();
        if (actual.manaCost() == null) {
            failWithMessage("Expected to have a mana cost but was null");
        }
        return this;
    }

    public CardAssert hasNoManaCost() {
        isNotNull();
        if (actual.manaCost() != null) {
            failWithMessage("Expected no mana cost but was <%s>", actual.manaCost());
        }
        return this;
    }

    public CardAssert hasRulesText(String expected) {
        isNotNull();
        if (!actual.rulesText().equals(expected)) {
            failWithMessage("Expected rules text to be <%s> but was <%s>", expected, actual.rulesText());
        }
        return this;
    }

    public CardAssert hasEmptyRulesText() {
        isNotNull();
        if (!actual.rulesText().isEmpty()) {
            failWithMessage("Expected empty rules text but was <%s>", actual.rulesText());
        }
        return this;
    }
}
