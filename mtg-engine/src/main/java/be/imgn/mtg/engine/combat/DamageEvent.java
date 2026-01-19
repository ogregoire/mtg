package be.imgn.mtg.engine.combat;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Event representing damage being dealt ({@mtg.rule 120}).
///
/// Damage can be dealt by sources (creatures, spells, abilities) to targets
/// (creatures, planeswalkers, players). The results of damage depend on the target:
/// - Damage to a player reduces their life total
/// - Damage to a creature is marked until end of turn (may cause death)
/// - Damage to a planeswalker removes loyalty counters
///
/// Damage events can be prevented or replaced.
///
/// @param source the object dealing the damage
/// @param target the target receiving the damage
/// @param amount the amount of damage
/// @param combat true if this is combat damage
/// @param infect true if this damage is dealt as -1/-1 counters or poison
public record DamageEvent(GameObject source, DamageTarget target, int amount, boolean combat, boolean infect)
        implements GameEvent {

    @Override
    public Player affectedPlayer() {
        return target.affectedPlayer();
    }

    /// Returns true if this is combat damage.
    ///
    /// @return true if combat damage
    public boolean isCombatDamage() {
        return combat;
    }

    /// Returns true if this is non-combat damage.
    ///
    /// @return true if not combat damage
    public boolean isNonCombatDamage() {
        return !combat;
    }

    /// Creates a DamageEvent with modified amount.
    ///
    /// @param newAmount the new damage amount
    /// @return a new DamageEvent with the modified amount
    public DamageEvent withAmount(int newAmount) {
        return new DamageEvent(source, target, newAmount, combat, infect);
    }
}
