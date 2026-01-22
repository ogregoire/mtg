package be.imgn.mtg.engine.mana;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Effect that adds mana of allowed types to a player's pool.
///
/// Each mana can be a different type from the allowed set, chosen independently at resolution.
///
/// Examples:
/// - "Add two mana in any combination of colors." → all 5 colors allowed
/// - "Add three mana in any combination of {R} and/or {G}." → only RED and GREEN allowed
/// - "Add X mana in any combination of {W} and/or {U}." → variable amount
///
/// @param amount the amount of mana to add
/// @param allowedTypes the set of mana types that can be chosen
public record AddManaCombinationEffect(Amount amount, Set<ManaType.Colored> allowedTypes) implements AddManaEffect {

    /// All five colors for "mana of any color" effects.
    private static final Set<ManaType.Colored> ALL_COLORS =
            Collections.unmodifiableSet(EnumSet.allOf(ManaType.Colored.class));

    /// Creates a new effect to add mana of any combination.
    public AddManaCombinationEffect {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(allowedTypes, "allowedTypes");
        if (allowedTypes.isEmpty()) {
            throw new IllegalArgumentException("allowedTypes cannot be empty");
        }
        // Defensive properly ordered copy
        allowedTypes = Collections.unmodifiableSet(EnumSet.copyOf(allowedTypes));
    }

    /// Creates an effect for "add N mana of any color" or "add N mana in any combination of colors".
    ///
    /// @param amount the amount of mana to add
    /// @return an effect allowing any of the 5 colors
    public static AddManaCombinationEffect anyColor(Amount amount) {
        return new AddManaCombinationEffect(amount, ALL_COLORS);
    }
}
