package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Effect that adds mana of any one color to a player's pool.
///
/// All mana produced is the same color, chosen at resolution.
/// Example: "Add four mana of any one color."
///
/// @param amount the amount of mana to add
public record AddManaOfAnyOneColor(int amount) {

    public AddManaOfAnyOneColor {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    /// Resolves the effect, adding mana to the controller's pool.
    ///
    /// @param controller the player receiving the mana
    /// @param source the object producing the mana
    /// @param choiceProvider the strategy for choosing the color
    public void resolve(Player controller, GameObject source, ManaColorChoiceProvider choiceProvider) {
        var chosenType = choiceProvider.chooseColor();
        if (!chosenType.isColored()) {
            throw new IllegalStateException("Must choose a colored mana type, got: " + chosenType);
        }

        for (int i = 0; i < amount; i++) {
            controller.manaPool().add(Mana.of(chosenType, source));
        }
    }
}
