package be.imgn.mtg.engine.format;

import java.util.List;

import be.imgn.mtg.engine.game.PlayerData;

/// A Magic: The Gathering game format.
///
/// Formats define the rules for deck construction, banned/restricted lists,
/// starting life totals, and other game configuration. Examples include
/// Standard, Modern, Legacy, Commander, and Limited formats.
public interface Format {
    /// Validates that the players are valid for this format.
    ///
    /// @param playersData the player data to validate
    /// @throws IllegalArgumentException if the players are not valid for this format
    void checkPlayers(List<PlayerData> playersData);
}
