package be.imgn.mtg.engine.selector;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Common interface for entities that can be queried from the game state.
///
/// Both game objects and players can be targets of effects and selections.
/// This interface unifies them for use with [Selector].
public sealed interface Selectable permits GameObject, Player {}
