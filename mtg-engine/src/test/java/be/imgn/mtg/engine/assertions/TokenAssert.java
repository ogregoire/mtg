package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.object.Token;

/// Assertion class for Token.
public class TokenAssert extends AbstractGameObjectAssert<TokenAssert, Token> {

    protected TokenAssert(Token actual) {
        super(actual, TokenAssert.class);
    }

    public static TokenAssert assertThat(Token actual) {
        return new TokenAssert(actual);
    }
}
