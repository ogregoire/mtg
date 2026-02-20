package be.imgn.mtg.engine.selector;

import be.imgn.mtg.engine.game.Player;

/// Selects players based on their relationship to the perspective player.
///
/// @param quantifier how many players to select
/// @param criterion the player matching criterion
public record PlayerSelector(Quantifier quantifier, PlayerCriterion criterion) implements Selector {

    @Override
    public boolean matches(Selectable selectable, Player perspective) {
        return selectable instanceof Player player && matches(player, perspective);
    }

    /// Returns whether the given player matches the criterion.
    ///
    /// @param player the player to test
    /// @param perspective the player from whose perspective the match is evaluated
    /// @return true if the player matches
    public boolean matches(Player player, Player perspective) {
        return switch (criterion) {
            case ANY -> true;
            case OPPONENT -> !player.equals(perspective);
            case YOU -> player.equals(perspective);
        };
    }
}
