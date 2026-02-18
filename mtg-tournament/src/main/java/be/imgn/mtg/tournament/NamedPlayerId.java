package be.imgn.mtg.tournament;

import java.util.UUID;

/// A [PlayerId] implementation with a name and UUID.
///
/// @param name the player's display name
/// @param id   the unique identifier
public record NamedPlayerId(String name, UUID id) implements PlayerId {

    /// Creates a named player ID with a random UUID.
    public NamedPlayerId(String name) {
        this(name, UUID.randomUUID());
    }

    @Override
    public String toString() {
        return name;
    }
}
