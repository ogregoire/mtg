package be.imgn.mtg.engine.game;

/// An element of Magic: The Gathering that has an owner.
///
/// In Magic, the owner of a card is the player whose deck it started in, or who
/// brought it into the game. Ownership never changes during the game, unlike
/// [control][Controlled#controller()].
///
/// @see Controlled
public interface Owned {

    /// Returns the player who owns this object.
    ///
    /// @return the owning player, never null
    Player owner();
}
