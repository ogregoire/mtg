package be.imgn.mtg.engine.ability;

import java.util.UUID;

import be.imgn.mtg.engine.util.UUIDv7;

/// Globally unique identifier for an ability.
///
/// Each ability has a unique identifier assigned when created (e.g., when a card is parsed
/// or a token is created). The identifier remains stable across zone changes, allowing:
/// - Tracking activations per specific ability
/// - Linking abilities ({@mtg.rule 607})
/// - Identifying delayed triggered abilities
///
/// The identifier uses UUID v7, which is time-ordered and suitable for sorted collections.
///
/// @param value the UUID v7 value
public record AbilityId(UUID value) {

    /// Creates a new unique AbilityId using UUID v7.
    public AbilityId() {
        this(UUIDv7.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
