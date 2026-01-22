package be.imgn.mtg.engine.game;

import java.util.List;

/// A choice presented to a player during the game.
///
/// Choices are the unified mechanism for all player decisions in MTG:
/// - Modal spells ("Choose one —")
/// - Target selection
/// - Mana payment options (hybrid, Phyrexian)
/// - Card selection (discard, sacrifice)
/// - Mana production choices
/// - Any other player decision
///
/// Internally always stores Option<T> for consistent UI handling.
/// Factory methods allow passing raw values (auto-wrapped) or explicit Options.
///
/// @param <T> the type of the underlying values
/// @param options available options (always wrapped in Option<T>)
/// @param count how many must be selected
/// @param description human-readable description of the choice
public record Choice<T>(List<Option<T>> options, SelectionCount count, String description) {

    /// Creates a new Choice.
    public Choice {
        options = List.copyOf(options);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Choice must have at least one option");
        }
    }

    /// Creates a choice where exactly one option must be selected.
    ///
    /// Uses toString() of each value as the display description.
    ///
    /// @param values the available values to choose from
    /// @param description human-readable description of the choice
    /// @param <T> the type of the values
    /// @return a new Choice requiring exactly one selection
    public static <T> Choice<T> oneOf(List<T> values, String description) {
        return new Choice<>(wrap(values), SelectionCount.exactly(1), description);
    }

    /// Creates a choice where exactly n options must be selected.
    ///
    /// Uses toString() of each value as the display description.
    ///
    /// @param values the available values to choose from
    /// @param n the number of options that must be selected
    /// @param description human-readable description of the choice
    /// @param <T> the type of the values
    /// @return a new Choice requiring exactly n selections
    public static <T> Choice<T> nOf(List<T> values, int n, String description) {
        return new Choice<>(wrap(values), SelectionCount.exactly(n), description);
    }

    /// Creates a choice where up to max options may be selected.
    ///
    /// Uses toString() of each value as the display description.
    ///
    /// @param values the available values to choose from
    /// @param max the maximum number of options that may be selected
    /// @param description human-readable description of the choice
    /// @param <T> the type of the values
    /// @return a new Choice allowing up to max selections
    public static <T> Choice<T> upTo(List<T> values, int max, String description) {
        return new Choice<>(wrap(values), SelectionCount.upTo(max), description);
    }

    /// Creates a choice where at least min options must be selected.
    ///
    /// Uses toString() of each value as the display description.
    ///
    /// @param values the available values to choose from
    /// @param min the minimum number of options that must be selected
    /// @param description human-readable description of the choice
    /// @param <T> the type of the values
    /// @return a new Choice requiring at least min selections
    public static <T> Choice<T> atLeast(List<T> values, int min, String description) {
        return new Choice<>(wrap(values), SelectionCount.atLeast(min), description);
    }

    /// Creates a choice from Options with explicit descriptions.
    ///
    /// Use this when you need custom display text for each option.
    ///
    /// @param options the available options with descriptions
    /// @param description human-readable description of the choice
    /// @param <T> the type of the underlying values
    /// @return a new Choice requiring exactly one selection
    public static <T> Choice<T> withDescriptions(List<Option<T>> options, String description) {
        return new Choice<>(options, SelectionCount.exactly(1), description);
    }

    /// Creates a choice from Options with a custom selection count.
    ///
    /// Use this when you need custom display text and a non-standard selection count.
    ///
    /// @param options the available options with descriptions
    /// @param count how many options must be selected
    /// @param description human-readable description of the choice
    /// @param <T> the type of the underlying values
    /// @return a new Choice with the specified selection count
    public static <T> Choice<T> withDescriptions(List<Option<T>> options, SelectionCount count, String description) {
        return new Choice<>(options, count, description);
    }

    /// Wraps raw values in Options using toString() as the description.
    private static <T> List<Option<T>> wrap(List<T> values) {
        return values.stream().map(v -> new Option<>(v, v.toString())).toList();
    }
}
