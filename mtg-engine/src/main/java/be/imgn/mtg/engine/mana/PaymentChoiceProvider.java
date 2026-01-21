package be.imgn.mtg.engine.mana;

/// Strategy interface for making payment choices during mana payment.
///
/// This is used when paying costs that have multiple payment options,
/// such as hybrid mana or Phyrexian mana.
public interface PaymentChoiceProvider {

    /// Called when a hybrid symbol needs a choice between two mana options.
    ///
    /// @param symbol the hybrid symbol being paid
    /// @param pool the current mana pool
    /// @return the chosen payment option
    HybridChoice chooseHybrid(ManaSymbol.Hybrid symbol, ManaPool pool);

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
