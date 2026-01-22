package be.imgn.mtg.engine.mana;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Effect that adds mana of allowed types to a player's pool.
///
/// Each mana can be a different type from the allowed set, chosen independently at resolution.
///
/// Examples:
/// - "Add two mana in any combination of colors." → all 5 colors allowed
/// - "Add three mana in any combination of {R} and/or {G}." → only RED and GREEN allowed
///
/// @param amount the amount of mana to add
/// @param allowedTypes the set of mana types that can be chosen
public record AddManaCombinationEffect(int amount, Set<ManaType.Colored> allowedTypes) implements AddManaEffect {

    /// All five colors for "mana of any color" effects.
    private static final Set<ManaType.Colored> ALL_COLORS =
            Collections.unmodifiableSet(EnumSet.allOf(ManaType.Colored.class));

    /// Creates a new effect to add mana of any combination.
    public AddManaCombinationEffect {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
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
    public static AddManaCombinationEffect anyColor(int amount) {
        return new AddManaCombinationEffect(amount, ALL_COLORS);
    }

    /// Resolves the effect, adding mana to the controller's pool.
    ///
    /// Each mana's type is chosen separately from the allowed types.
    ///
    /// @param controller the player receiving the mana
    /// @param source the object producing the mana
    /// @param choiceProvider the strategy for choosing types (called for each mana)
    public void resolve(Player controller, GameObject source, ManaColorChoiceProvider choiceProvider) {
        for (int i = 0; i < amount; i++) {
            var chosenType = choiceProvider.chooseColor();
            if (!allowedTypes.contains(chosenType)) {
                throw new IllegalStateException(
                        "Chosen mana type " + chosenType + " is not in allowed types: " + allowedTypes);
            }
            controller.manaPool().add(Mana.of(chosenType, source));
        }
    }
}
