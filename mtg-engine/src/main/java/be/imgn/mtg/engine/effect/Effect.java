package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.mana.AddManaEffect;

/// Represents a parsed effect from oracle text.
///
/// This is a sealed interface that permits specific effect types.
public sealed interface Effect
        permits DestroyEffect,
                ExileEffect,
                SacrificeEffect,
                ReturnToHandEffect,
                PutOnLibraryEffect,
                MillEffect,
                DealDamageEffect,
                GainLifeEffect,
                LoseLifeEffect,
                DrawEffect,
                DiscardEffect,
                ScryEffect,
                SearchLibraryEffect,
                TapEffect,
                UntapEffect,
                AddCountersEffect,
                RemoveCountersEffect,
                GainAbilityEffect,
                ModifyPowerToughnessEffect,
                GainControlEffect,
                CreateTokenEffect,
                CounterSpellEffect,
                FightEffect,
                AddManaEffect,
                CompoundEffect {

    /// Returns whether this effect could add mana to a player's mana pool ({@mtg.rule 605.1a}).
    default boolean addsMana() {
        return false;
    }

    /// Returns whether this effect requires a target ({@mtg.rule 605.1a}).
    ///
    /// @return true if this effect requires a target
    default boolean requiresTarget() {
        return false;
    }

    /// Returns whether this effect qualifies as a mana ability effect ({@mtg.rule 605.1a}).
    ///
    /// An effect is a mana ability effect if it could add mana and does not require a target.
    /// For compound effects, this means at least one sub-effect could add mana and no
    /// sub-effect requires a target.
    ///
    /// @return true if this effect qualifies as a mana ability effect
    default boolean isManaAbilityEffect() {
        return addsMana() && !requiresTarget();
    }
}
