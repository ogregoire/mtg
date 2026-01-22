package be.imgn.mtg.engine.mana;

import java.util.List;

/// Strategy interface for choosing a mana option from a selection.
///
/// This is used when an effect offers multiple mana production options,
/// such as "Add {U} or {B}" or "Add {R}{R}, {R}{G}, or {G}{G}".
public interface ManaSelectionChoiceProvider {

    /// Chooses one option from the available mana selections.
    ///
    /// @param options the available options, each being a list of mana types
    /// @return the index of the chosen option (0-based)
    int chooseOption(List<List<ManaType>> options);
}
