package be.imgn.mtg.engine.combat;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;

/// Represents a valid target for damage.
///
/// Damage can be dealt to players, creatures, planeswalkers, and battles.
/// Each target type has different effects when receiving damage.
public sealed interface DamageTarget {

    /// Returns the player affected by damage to this target.
    ///
    /// For player damage, this is the player.
    /// For permanent damage, this is the permanent's controller.
    ///
    /// @return the affected player
    Player affectedPlayer();

    /// A player receiving damage.
    ///
    /// Damage to a player causes them to lose that much life ({@mtg.rule 120.2}).
    ///
    /// @param player the player receiving damage
    record PlayerTarget(Player player) implements DamageTarget {
        @Override
        public Player affectedPlayer() {
            return player;
        }
    }

    /// A creature receiving damage.
    ///
    /// Damage to a creature is marked on it. If the damage equals or exceeds
    /// its toughness, it's destroyed as a state-based action ({@mtg.rule 120.4}).
    ///
    /// @param creature the creature receiving damage
    record CreatureTarget(Permanent creature) implements DamageTarget {
        @Override
        public Player affectedPlayer() {
            return creature.controller();
        }
    }

    /// A planeswalker receiving damage.
    ///
    /// Damage to a planeswalker removes that many loyalty counters ({@mtg.rule 120.6}).
    ///
    /// @param planeswalker the planeswalker receiving damage
    record PlaneswalkerTarget(Permanent planeswalker) implements DamageTarget {
        @Override
        public Player affectedPlayer() {
            return planeswalker.controller();
        }
    }

    /// A battle receiving damage.
    ///
    /// Damage to a battle removes that many defense counters ({@mtg.rule 120.7}).
    ///
    /// @param battle the battle receiving damage
    record BattleTarget(Permanent battle) implements DamageTarget {
        @Override
        public Player affectedPlayer() {
            return battle.controller();
        }
    }
}
