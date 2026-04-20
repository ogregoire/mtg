package be.imgn.mtg.engine.characteristics;

/// Counter types in Magic ({@mtg.rule 122}).
///
/// A counter is a marker placed on an object or player that modifies its characteristics or
/// interacts with a rule, ability, or effect. Counters with the same name are interchangeable.
///
/// This interface represents all counter types. Use [#of(String)] to get or create a
/// counter type by name. Standard counter types (those with special rules) are defined in
/// [StandardCounterType].
public sealed interface CounterType permits StandardCounterType, CustomCounterType {

    /// Returns the text representation of this counter type.
    ///
    /// @return the counter type text (e.g., "+1/+1", "loyalty", "charge")
    String text();

    /// Returns a counter type for the given text.
    ///
    /// If the text matches a standard counter type, that type is returned.
    /// Otherwise, a new custom counter type is created.
    ///
    /// @param text the counter type text
    /// @return the counter type
    static CounterType of(String text) {
        // First, try to find a matching standard counter type
        for (var standard : StandardCounterType.values()) {
            if (standard.text().equalsIgnoreCase(text)) {
                return standard;
            }
        }
        // Not a standard type, create a custom one
        return new CustomCounterType(text);
    }
}
