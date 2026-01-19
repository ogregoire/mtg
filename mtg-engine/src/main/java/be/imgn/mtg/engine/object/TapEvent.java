package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;

/// Event representing a permanent becoming tapped or untapped ({@mtg.rule 701.21}, {@mtg.rule 701.22}).
///
/// Tapping is commonly done to activate abilities or attack. Untapping typically happens
/// during the untap step but can occur from effects.
///
/// @param permanent the permanent changing tap status
/// @param tapping true if becoming tapped, false if becoming untapped
public record TapEvent(Permanent permanent, boolean tapping) implements GameEvent {

    @Override
    public Player affectedPlayer() {
        return permanent.controller();
    }

    /// Returns true if this is a tap event.
    ///
    /// @return true if the permanent is becoming tapped
    public boolean isTapping() {
        return tapping;
    }

    /// Returns true if this is an untap event.
    ///
    /// @return true if the permanent is becoming untapped
    public boolean isUntapping() {
        return !tapping;
    }
}
