package be.imgn.mtg.engine.mana;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Effect that adds mana from a selection of options to a player's pool.
///
/// The player chooses one option from the available selections, and all mana
/// in that option is added to their pool.
///
/// Examples:
/// - "Add {U} or {B}." → `[[BLUE], [BLACK]]`
/// - "Add {R}{R}, {R}{G}, or {G}{G}." → `[[RED, RED], [RED, GREEN], [GREEN, GREEN]]`
/// - "Add two mana of any one color." → `[[WHITE, WHITE], [BLUE, BLUE], ...]`
///
/// @param options the available mana options to choose from
public record AddManaSelectionEffect(List<List<ManaType>> options) implements AddManaEffect {

    /// Creates a new effect to add mana from a selection.
    public AddManaSelectionEffect {
        Objects.requireNonNull(options, "options");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options cannot be empty");
        }
        for (var option : options) {
            if (option.isEmpty()) {
                throw new IllegalArgumentException("Each option must contain at least one mana type");
            }
        }
        // Defensive copy
        options = options.stream().map(List::copyOf).toList();
    }

    /// Resolves the effect, adding mana to the controller's pool.
    ///
    /// @param controller the player receiving the mana
    /// @param source the object producing the mana
    /// @param choiceProvider the strategy for choosing which option
    public void resolve(Player controller, GameObject source, ManaSelectionChoiceProvider choiceProvider) {
        var chosenIndex = choiceProvider.chooseOption(options);
        if (chosenIndex < 0 || chosenIndex >= options.size()) {
            throw new IllegalStateException("Invalid option index: " + chosenIndex);
        }
        var chosenOption = options.get(chosenIndex);
        for (var type : chosenOption) {
            controller.manaPool().add(Mana.of(type, source));
        }
    }

    /// Creates a selection for "add N mana of any one color".
    ///
    /// @param amount the amount of mana to add
    /// @return a selection with one option per color, each containing N mana of that color
    public static AddManaSelectionEffect anyOneColor(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
        var options = List.of(
                Collections.nCopies(amount, (ManaType) ManaType.WHITE),
                Collections.nCopies(amount, (ManaType) ManaType.BLUE),
                Collections.nCopies(amount, (ManaType) ManaType.BLACK),
                Collections.nCopies(amount, (ManaType) ManaType.RED),
                Collections.nCopies(amount, (ManaType) ManaType.GREEN));
        return new AddManaSelectionEffect(options);
    }
}
