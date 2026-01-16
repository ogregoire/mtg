package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultToken;

/// A token in Magic: The Gathering ({@mtg.rule 111}).
///
/// A token is a marker used to represent a permanent that isn't represented by a card.
/// Tokens are created by spells and abilities. A token is owned by the player under
/// whose control it entered the battlefield.
///
/// Tokens cease to exist as a state-based action when they exist in a zone other than
/// the battlefield. Tokens are [PermanentSource] objects that can become [Permanent]s.
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
