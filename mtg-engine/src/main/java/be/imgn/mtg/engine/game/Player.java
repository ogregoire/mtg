package be.imgn.mtg.engine.game;

import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.CostContext;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

/// A player in the game ({@mtg.rule 102}).
///
/// A player is one of the participants in the game. Each player starts with a life total
/// of 20 (in most formats), a hand of seven cards, and a library containing their deck.
///
/// Players can own cards ({@mtg.rule 108.3}), control objects ({@mtg.rule 108.4}), receive
/// priority to cast spells and activate abilities ({@mtg.rule 117}), and make game choices.
/// A player loses when their life total is 0 or less, they attempt to draw from an empty
/// library, or they have 10 or more poison counters ({@mtg.rule 104.3}).
public interface Player {

    /// Returns the initial data for this player.
    ///
    /// @return the player data containing id and team
    PlayerData data();

    /// Returns this player's library.
    ///
    /// @return the library zone
    Library library();

    /// Returns this player's hand.
    ///
    /// @return the hand zone
    Hand hand();

    /// Returns this player's graveyard.
    ///
    /// @return the graveyard zone
    Graveyard graveyard();

    /// Returns this player's mana pool ({@mtg.rule 106.4}).
    ///
    /// @return the mana pool
    ManaPool manaPool();

    /// Returns this player's current life total ({@mtg.rule 119}).
    ///
    /// @return the life total
    int lifeTotal();

    /// Gains life ({@mtg.rule 119.3}).
    ///
    /// @param amount the amount of life to gain (must be positive)
    void gainLife(int amount);

    /// Loses life ({@mtg.rule 119.4}).
    ///
    /// Used for costs and effects, not damage.
    ///
    /// @param amount the amount of life to lose (must be positive)
    void loseLife(int amount);

    /// Sets this player's life total directly ({@mtg.rule 119.5}).
    ///
    /// @param amount the new life total
    void setLifeTotal(int amount);

    /// Checks if this player can pay the given cost.
    ///
    /// @param cost the cost to check
    /// @return true if the cost can be paid
    boolean canPay(Cost cost);

    /// Pays the given cost.
    ///
    /// @param cost the cost to pay
    /// @throws IllegalStateException if the cost cannot be paid
    void pay(Cost cost);

    /// Checks if this player can pay the given mana cost in the given context.
    ///
    /// Restricted mana can only be used if the restriction allows spending on the context's source.
    ///
    /// @param cost the mana cost to check
    /// @param context the cost context containing the source (for checking mana restrictions)
    /// @return true if the cost can be paid
    boolean canPay(ManaCost cost, CostContext context);

    /// Pays the given mana cost in the given context.
    ///
    /// Restricted mana can only be used if the restriction allows spending on the context's source.
    ///
    /// @param cost the mana cost to pay
    /// @param context the cost context containing the source (for checking mana restrictions)
    /// @throws IllegalStateException if the cost cannot be paid
    void pay(ManaCost cost, CostContext context);
}
