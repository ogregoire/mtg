package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.object.internal.DefaultToken;

/// A token in Magic.
public non-sealed interface Token extends GameObject, PermanentSource {

    /// Returns a new builder for Token.
    static Builder builder() {
        return DefaultToken.builder();
    }

    /// Builder for Token.
    non-sealed interface Builder extends GameObject.Builder<Token, Builder> {

        Builder owner(Player owner);

        Builder controller(Player controller);
    }
}
