package be.imgn.mtg.engine.characteristics;

import java.util.Locale;

/// A custom counter type not defined in the standard rules.
///
/// Magic allows cards to create counters with arbitrary names. This record represents
/// counter types that are not part of [StandardCounterType].
///
/// @param text the counter type text (e.g., "hourglass", "filibuster")
public record CustomCounterType(String text) implements CounterType {

    /// Creates a custom counter type.
    ///
    /// @param text the counter type text, will be normalized to lowercase
    public CustomCounterType {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Counter type text cannot be null or blank");
        }
        text = text.toLowerCase(Locale.ROOT);
    }
}
