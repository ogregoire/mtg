package be.imgn.mtg.engine.mana;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// Effect that adds mana from a selection of options to a player's pool.
///
/// The player chooses one option from the available selections, and all mana
/// in that option is added to their pool.
///
/// Examples:
/// - "Add {U} or {B}." → options=`[[BLUE], [BLACK]]`, amount=null
/// - "Add {R}{R}, {R}{G}, or {G}{G}." → options=`[[RED, RED], [RED, GREEN], [GREEN, GREEN]]`, amount=null
/// - "Add two mana of any one color." → options=`[[WHITE], [BLUE], ...]`, amount=2
/// - "Add X mana of any one color." → options=`[[WHITE], [BLUE], ...]`, amount=X
///
/// @param options the available mana options to choose from
/// @param amount for "any one color", the amount to add of the chosen color; null for explicit options
public record AddManaSelectionEffect(
        List<List<ManaType>> options, @Nullable Amount amount) implements AddManaEffect {

    /// The five color options for "any one color" effects.
    private static final List<List<ManaType>> ANY_ONE_COLOR_OPTIONS = List.of(
            List.of(ManaType.WHITE),
            List.of(ManaType.BLUE),
            List.of(ManaType.BLACK),
            List.of(ManaType.RED),
            List.of(ManaType.GREEN));

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

    /// Creates an effect for explicit mana options.
    ///
    /// @param options the available mana options
    public AddManaSelectionEffect(List<List<ManaType>> options) {
        this(options, null);
    }

    /// Creates a selection for "add N mana of any one color".
    ///
    /// @param amount the amount of mana to add
    /// @return a selection with one option per color
    public static AddManaSelectionEffect anyOneColor(Amount amount) {
        Objects.requireNonNull(amount, "amount");
        return new AddManaSelectionEffect(ANY_ONE_COLOR_OPTIONS, amount);
    }
}
