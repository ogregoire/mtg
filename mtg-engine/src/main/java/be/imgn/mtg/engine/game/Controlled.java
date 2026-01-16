package be.imgn.mtg.engine.game;

/// An object that has a controller.
///
/// In Magic: The Gathering, the controller of an object is the player who currently
/// has control over it. This may differ from the [owner][Owned#owner()] for permanents
/// that have been stolen or abilities that have been redirected.
///
/// @see Owned
public interface Controlled {

    /// Returns the player who controls this object.
    ///
    /// @return the controlling player, never null
    Player controller();
}
