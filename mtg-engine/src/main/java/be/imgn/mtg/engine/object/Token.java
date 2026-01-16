package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultToken;

/// A token in Magic: The Gathering.
///
/// Tokens are game objects created by spells or abilities. They are not cards,
/// but they can become permanents on the battlefield. When a token leaves the
/// battlefield, it ceases to exist.
///
/// Tokens are [PermanentSource] objects that can enter the battlefield.
///
/// @see Permanent
/// @see Card
public non-sealed interface Token extends GameObject, PermanentSource {

    /// Returns a new builder for Token.
    ///
    /// @return a new builder instance
    static Builder builder() {
        return DefaultToken.builder();
    }

    /// Builder for [Token].
    non-sealed interface Builder extends GameObject.Builder<Token, Builder> {

        /// Sets the owner of the token.
        ///
        /// The owner of a token is typically the player under whose control it entered.
        ///
        /// @param owner the owning player
        /// @return this builder
        Builder owner(Player owner);

        /// Sets the controller of the token.
        ///
        /// @param controller the controlling player
        /// @return this builder
        Builder controller(Player controller);
    }
}
