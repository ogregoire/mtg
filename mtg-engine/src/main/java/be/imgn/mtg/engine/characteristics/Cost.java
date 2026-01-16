package be.imgn.mtg.engine.characteristics;

/// A cost that must be paid to take an action ({@mtg.rule 118}).
///
/// Costs are actions or payments necessary to take another action or prevent an action from
/// being taken. Costs include mana costs, life payments, tapping, sacrificing, and discarding.
public interface Cost {

    /// Returns true if this cost can be paid in the given context.
    ///
    /// @param context the context for paying the cost
    /// @return true if the cost can be paid
    boolean canPay(CostContext context);

    /// Pays this cost.
    ///
    /// @param context the context for paying the cost
    void pay(CostContext context);

    /// Returns a description of this cost.
    String description();
}
