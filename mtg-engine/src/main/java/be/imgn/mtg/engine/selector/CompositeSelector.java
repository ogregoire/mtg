package be.imgn.mtg.engine.selector;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Selects both game objects and players by delegating to an [ObjectSelector] and a [PlayerSelector].
///
/// Used for effects like "any target" that can target both creatures/planeswalkers and players.
///
/// @param quantifier how many entities to select
/// @param objectSelector the selector for game objects
/// @param playerSelector the selector for players
public record CompositeSelector(Quantifier quantifier, ObjectSelector objectSelector, PlayerSelector playerSelector)
        implements Selector {

    @Override
    public boolean matches(Selectable selectable, Player perspective) {
        return switch (selectable) {
            case GameObject obj -> objectSelector.matches(obj, perspective);
            case Player player -> playerSelector.matches(player, perspective);
        };
    }
}
