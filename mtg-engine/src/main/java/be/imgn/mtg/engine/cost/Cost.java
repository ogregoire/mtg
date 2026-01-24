package be.imgn.mtg.engine.cost;

import be.imgn.mtg.engine.cost.internal.CompoundCost;
import be.imgn.mtg.engine.cost.internal.DiscardCost;
import be.imgn.mtg.engine.cost.internal.EnergyCost;
import be.imgn.mtg.engine.cost.internal.ExileCost;
import be.imgn.mtg.engine.cost.internal.LifeCost;
import be.imgn.mtg.engine.cost.internal.LoyaltyCost;
import be.imgn.mtg.engine.cost.internal.MillCost;
import be.imgn.mtg.engine.cost.internal.RemoveCountersCost;
import be.imgn.mtg.engine.cost.internal.ReturnToHandCost;
import be.imgn.mtg.engine.cost.internal.RevealCost;
import be.imgn.mtg.engine.cost.internal.SacrificeCost;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.cost.internal.TapPermanentsCost;
import be.imgn.mtg.engine.cost.internal.UntapCost;
import be.imgn.mtg.engine.mana.ManaCost;

/// A cost that must be paid to take an action ({@mtg.rule 118}).
///
/// Costs are actions or payments necessary to take another action or prevent an action from
/// being taken. Costs include mana costs, life payments, tapping, sacrificing, and discarding.
public sealed interface Cost
        permits TapCost,
                UntapCost,
                LifeCost,
                SacrificeCost,
                DiscardCost,
                ExileCost,
                RemoveCountersCost,
                EnergyCost,
                MillCost,
                ReturnToHandCost,
                RevealCost,
                TapPermanentsCost,
                LoyaltyCost,
                CompoundCost,
                ManaCost {

    /// Returns a description of this cost.
    ///
    /// @return the cost description
    String description();

    /// Returns true if this cost involves loyalty counters ({@mtg.rule 606}).
    ///
    /// @return true if this is a loyalty cost
    default boolean isLoyaltyCost() {
        return false;
    }
}
