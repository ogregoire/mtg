package be.imgn.mtg.engine.selector;

import be.imgn.mtg.engine.game.Player;

/// A query that matches [Selectable] entities (game objects and players).
///
/// Selectors form a sealed hierarchy:
/// - [ObjectSelector] — matches game objects
/// - [PlayerSelector] — matches players
/// - [CompositeSelector] — matches both game objects and players
public sealed interface Selector permits ObjectSelector, PlayerSelector, CompositeSelector {

    /// Returns the quantifier specifying how many entities to select.
    Quantifier quantifier();

    /// Returns whether the given selectable matches this selector.
    ///
    /// @param selectable the entity to test
    /// @param perspective the player from whose perspective the match is evaluated
    /// @return true if the selectable matches
    boolean matches(Selectable selectable, Player perspective);
}
