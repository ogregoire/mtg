package be.imgn.mtg.engine.object;

/// An element of Magic: the Gathering that has an owner.
public interface Owned {

    /// Returns the player who owns this object.
    Player owner();
}
