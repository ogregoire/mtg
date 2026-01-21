package be.imgn.mtg.engine.mana;

/// Strategy for choosing mana colors at resolution time.
///
/// Used when resolving mana production effects that let the player
/// choose colors, such as "Add one mana of any color."
public interface ManaColorChoiceProvider {

    /// Choose a single color for mana production.
    ///
    /// @return the chosen mana type
    ManaType.Colored chooseColor();
}
