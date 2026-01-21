package be.imgn.mtg.engine.mana;

import java.util.List;

/// Result of checking if a mana pool can pay a mana cost.
///
/// Indicates which symbols can be paid from the pool and which cannot.
/// The caller (Player) may check if unpayable symbols include Phyrexian mana
/// that can be paid with life.
public sealed interface ManaPoolPaymentResult {

    /// All symbols can be paid from the mana pool.
    ///
    /// @param assignments the mana assignments for each symbol
    record FullyPayable(List<ManaAssignment> assignments) implements ManaPoolPaymentResult {}

    /// Some symbols can be paid, but not all.
    ///
    /// The caller should check if unpayable symbols are Phyrexian mana
    /// that can alternatively be paid with life.
    ///
    /// @param payable the symbols that can be paid
    /// @param unpayable the symbols that cannot be paid from the pool
    /// @param partialAssignments the assignments for the payable symbols
    record PartiallyPayable(
            List<ManaSymbol> payable, List<ManaSymbol> unpayable, List<ManaAssignment> partialAssignments)
            implements ManaPoolPaymentResult {}

    /// No symbols can be paid (pool is empty or no matching mana).
    record NotPayable() implements ManaPoolPaymentResult {}
}
