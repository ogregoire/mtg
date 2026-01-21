package be.imgn.mtg.engine.mana;

import java.util.List;

import be.imgn.mtg.engine.characteristics.CostContext;

/// A player's mana pool ({@mtg.rule 106.4}).
///
/// A mana pool is where mana is stored after being produced. Mana in the pool
/// can be spent to pay costs. The pool empties at the end of each step and phase.
public interface ManaPool {

    /// Returns all mana currently in this pool.
    ///
    /// @return an unmodifiable list of mana
    List<Mana> contents();

    /// Returns the number of mana of the given type in this pool.
    ///
    /// @param type the mana type to count
    /// @return the count
    int count(ManaType type);

    /// Returns the total amount of mana in this pool.
    ///
    /// @return the total count
    int totalCount();

    /// Returns true if this pool contains any snow mana.
    ///
    /// @return true if snow mana is present
    boolean containsSnow();

    /// Returns true if this pool is empty.
    ///
    /// @return true if no mana is in the pool
    boolean isEmpty();

    /// Adds mana to this pool.
    ///
    /// @param mana the mana to add
    void add(Mana mana);

    /// Adds multiple mana to this pool.
    ///
    /// @param manaList the mana to add
    void addAll(List<Mana> manaList);

    /// Removes specific mana from this pool.
    ///
    /// @param mana the mana to remove
    /// @throws IllegalArgumentException if the mana is not in the pool
    void remove(Mana mana);

    /// Empties this mana pool ({@mtg.rule 106.4}).
    ///
    /// Called at the end of each step and phase.
    void empty();

    /// Checks if this pool can pay the given mana cost.
    ///
    /// Returns a detailed result indicating which symbols can and cannot be paid.
    /// Prioritizes using restricted mana when it matches the payment requirements.
    /// Restricted mana is only considered if it can be spent on the context's source.
    ///
    /// @param cost the mana cost to check
    /// @param context the cost context containing the source for restriction checks
    /// @return the payment result
    ManaPoolPaymentResult canPay(ManaCost cost, CostContext context);

    /// Pays the given mana cost from this pool.
    ///
    /// This method assumes canPay was already called and returned a payable result.
    /// It removes the appropriate mana from the pool.
    ///
    /// @param cost the cost to pay
    /// @param assignments the mana assignments to use (from canPay result)
    /// @throws IllegalStateException if the cost cannot be paid
    void pay(ManaCost cost, List<ManaAssignment> assignments);

    /// Checks if the given mana cost can be fully paid.
    ///
    /// This method considers both mana in the pool and the ability to pay
    /// Phyrexian mana with life. Restricted mana is only considered if it
    /// can be spent on the context's source.
    ///
    /// @param cost the mana cost to check
    /// @param context the cost context containing source and player (for life total)
    /// @return true if the cost can be fully paid
    boolean canPayFully(ManaCost cost, CostContext context);

    /// Pays the given mana cost from this pool.
    ///
    /// Removes mana from the pool and returns how much life was paid for Phyrexian symbols.
    /// Restricted mana is only used if it can be spent on the context's source.
    ///
    /// @param cost the mana cost to pay
    /// @param context the cost context containing source and player
    /// @return the result of the payment
    PaymentResult payFully(ManaCost cost, CostContext context);
}
