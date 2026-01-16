package be.imgn.mtg.engine.game;

/// An object that has a controller ({@mtg.rule 108.4}).
///
/// The controller of a permanent is the player who has control of it. By default, the
/// controller is the player who put it onto the battlefield. Control can change due to
/// effects (e.g., "gain control of target creature").
///
/// The controller of a spell is the player who cast it. The controller of an ability on
/// the stack is the player who controlled the source when the ability was activated or
/// triggered (or the player defined by the ability's text).
///
/// @see Owned
public interface Controlled {

    /// Returns the player who controls this object.
    ///
    /// @return the controlling player, never null
    Player controller();
}
