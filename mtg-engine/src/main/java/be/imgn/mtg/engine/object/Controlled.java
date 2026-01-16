package be.imgn.mtg.engine.object;

/// An object that has a controller.
public interface Controlled {

    /// Returns the player who controls this object.
    Player controller();
}
