package be.imgn.mtg.engine.object;

import java.util.UUID;

import be.imgn.mtg.engine.util.UUIDv7;

/// Unique identifier for a {@link GameObject} in the game.
///
/// Each game object is assigned a unique identifier when created. The identifier
/// uses UUID v7, which is time-ordered and suitable for sorted collections.
///
/// @param value the UUID v7 value
/// @see GameObject
public record ObjectId(UUID value) {

    /// Creates a new unique ObjectId using UUID v7.
    public ObjectId() {
        this(UUIDv7.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
