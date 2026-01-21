package be.imgn.mtg.engine.mana;

import java.util.List;

/// Result of a mana payment operation.
///
/// Indicates whether payment was successful and provides details about
/// what was paid (mana removed, life lost).
public sealed interface PaymentResult {

    /// Payment was successful.
    ///
    /// @param manaSpent the mana that was removed from the pool
    /// @param lifePaid the amount of life paid (for Phyrexian mana)
    record Success(List<Mana> manaSpent, int lifePaid) implements PaymentResult {
        public Success(List<Mana> manaSpent) {
            this(manaSpent, 0);
        }
    }

    /// Payment failed - not enough mana.
    ///
    /// @param missingSymbols the symbols that could not be paid
    record InsufficientMana(List<ManaSymbol> missingSymbols) implements PaymentResult {}

    /// Payment failed - not enough life for Phyrexian payment.
    ///
    /// @param lifeRequired the life required
    /// @param lifeAvailable the life available
    record InsufficientLife(int lifeRequired, int lifeAvailable) implements PaymentResult {}
}
