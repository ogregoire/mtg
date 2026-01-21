package be.imgn.mtg.engine.mana;

/// Strategy interface for making payment choices during mana payment.
///
/// This is used when paying costs that have multiple payment options,
/// such as hybrid mana or Phyrexian mana.
public interface PaymentChoiceProvider {

    /// Called when a two-color hybrid symbol needs a choice between two colors.
    ///
    /// @param symbol the hybrid symbol being paid
    /// @param pool the current mana pool
    /// @return the chosen payment option
    HybridChoice chooseHybrid(ManaSymbol.Hybrid symbol, ManaPool pool);

    /// Called when a mono-color hybrid symbol needs a choice between color or generic.
    ///
    /// @param symbol the mono-color hybrid symbol being paid
    /// @param pool the current mana pool
    /// @return the chosen payment option
    MonoColorHybridChoice chooseMonoColorHybrid(ManaSymbol.MonoColorHybrid symbol, ManaPool pool);

    /// Called when a colorless hybrid symbol needs a choice between color or colorless.
    ///
    /// @param symbol the colorless hybrid symbol being paid
    /// @param pool the current mana pool
    /// @return the chosen payment option
    ColorlessHybridChoice chooseColorlessHybrid(ManaSymbol.ColorlessHybrid symbol, ManaPool pool);

    /// Called when a Phyrexian symbol can be paid with mana or life.
    ///
    /// @param symbol the Phyrexian symbol being paid
    /// @param pool the current mana pool
    /// @param currentLife the player's current life total
    /// @return the chosen payment option
    PhyrexianChoice choosePhyrexian(ManaSymbol.Phyrexian symbol, ManaPool pool, int currentLife);

    /// Called when a hybrid Phyrexian symbol needs a choice (three options).
    ///
    /// @param symbol the hybrid Phyrexian symbol being paid
    /// @param pool the current mana pool
    /// @param currentLife the player's current life total
    /// @return the chosen payment option
    HybridPhyrexianChoice chooseHybridPhyrexian(ManaSymbol.HybridPhyrexian symbol, ManaPool pool, int currentLife);
}
