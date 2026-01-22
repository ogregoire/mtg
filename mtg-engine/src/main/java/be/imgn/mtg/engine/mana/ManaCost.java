package be.imgn.mtg.engine.mana;

import java.util.List;

import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.CostContext;
import be.imgn.mtg.engine.mana.internal.DefaultManaCost;

/// The mana cost of an object ({@mtg.rule 202}).
///
/// The mana cost is indicated by mana symbols near the top of a card.
/// An object's mana cost determines its colors and its mana value
/// (the total amount of mana regardless of color).
public interface ManaCost extends Cost {

    /// Returns the mana symbols in this cost.
    ///
    /// @return an unmodifiable list of symbols
    List<ManaSymbol> symbols();

    /// Returns the mana value of this cost ({@mtg.rule 202.3}).
    ///
    /// @return the total mana value
    int manaValue();

    /// Returns the colors of this cost.
    ///
    /// @return the colors
    Colors colors();

    /// Returns the generic mana component of this cost.
    ///
    /// @return the generic mana amount
    int genericComponent();

    /// Returns true if this cost contains a variable (X).
    ///
    /// @return true if X is present
    boolean hasVariable();

    /// Returns the number of X symbols in this cost.
    ///
    /// @return the count of X symbols
    int variableCount();

    /// Returns true if this cost is empty (no symbols).
    ///
    /// @return true if empty
    boolean isEmpty();

    /// Returns a new cost that is this cost plus the other cost.
    ///
    /// @param other the cost to add
    /// @return the combined cost
    ManaCost plus(ManaCost other);

    /// Returns a new cost with the generic component reduced by the given amount.
    ///
    /// @param amount the amount to reduce
    /// @return the reduced cost (generic cannot go below 0)
    ManaCost minusGeneric(int amount);

    /// Parses a mana cost string like "{2}{W}{W}".
    ///
    /// @param cost the cost string to parse
    /// @return the parsed mana cost
    static ManaCost parse(String cost) {
        return DefaultManaCost.parse(cost);
    }

    /// Returns an empty mana cost.
    ///
    /// @return the empty cost
    static ManaCost empty() {
        return DefaultManaCost.EMPTY;
    }

    @Override
    default boolean canPay(CostContext context) {
        // Delegate to player to check if they can pay this mana cost
        return context.player().canPay(this, context);
    }

    @Override
    default void pay(CostContext context) {
        // Delegate to player to pay this mana cost
        context.player().pay(this, context);
    }
}
