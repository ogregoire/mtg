package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Effect that adds mana of any colors to a player's pool.
///
/// Each mana can be a different color, chosen independently at resolution.
/// Example: "Add two mana in any combination of colors."
///
/// @param amount the amount of mana to add
public record AddManaOfAnyColorCombination(int amount) {

    /// Creates a new effect to add mana of any color combination.
    public AddManaOfAnyColorCombination {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    /// Resolves the effect, adding mana to the controller's pool.
    ///
    /// Each mana's color is chosen separately.
    ///
    /// @param controller the player receiving the mana
    /// @param source the object producing the mana
    /// @param choiceProvider the strategy for choosing colors (called for each mana)
    public void resolve(Player controller, GameObject source, ManaColorChoiceProvider choiceProvider) {
        for (int i = 0; i < amount; i++) {
            var chosenType = choiceProvider.chooseColor();
            controller.manaPool().add(Mana.of(chosenType, source));
        }
    }
}
