package be.imgn.mtg.engine.game;

/// An object that has an owner ({@mtg.rule 108.3}).
///
/// The owner of a card in the game is the player who started the game with it in their
/// deck. If a card is brought into the game from outside the game, its owner is the
/// player who brought it in. The owner of a token is the player under whose control it
/// entered the battlefield.
///
/// Ownership never changes during the game. When an object goes to a graveyard, hand,
/// or library, it goes to its owner's corresponding zone.
///
/// @see Controlled
public interface Owned {

    /// Returns the player who owns this object.
    ///
    /// @return the owning player, never null
    Player owner();
}
