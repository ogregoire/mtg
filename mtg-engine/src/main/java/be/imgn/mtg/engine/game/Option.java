package be.imgn.mtg.engine.game;

/// Wraps a value with a human-readable description for display.
///
/// Used by the Choice framework to present options to players with meaningful
/// text rather than relying on toString(). UI implementations display the
/// description while game logic operates on the underlying value.
///
/// @param <T> the type of the wrapped value
/// @param value the actual value
/// @param description human-readable text for UI display
public record Option<T>(T value, String description) {

    @Override
    public String toString() {
        return description;
    }
}
