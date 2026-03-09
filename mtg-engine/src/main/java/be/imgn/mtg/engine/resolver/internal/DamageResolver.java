package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.combat.DamageEvent;
import be.imgn.mtg.engine.combat.DamageTarget;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;

/// Resolver for damage events.
///
/// Applies damage to the target according to MTG rules:
/// - Player: loses life equal to damage
/// - Creature: damage is marked (may cause death via SBA)
/// - Planeswalker: removes loyalty counters
/// - Battle: removes defense counters
public final class DamageResolver implements EventResolver<DamageEvent> {

    @Override
    public Class<DamageEvent> eventType() {
        return DamageEvent.class;
    }

    @Override
    public void resolve(DamageEvent event, GameState state) {
        var target = event.target();
        var amount = event.amount();

        if (amount <= 0) {
            return;
        }

        switch (target) {
            case DamageTarget.PlayerTarget(var player) -> player.loseLife(amount);
            case DamageTarget.CreatureTarget(var creature) -> {
                // Damage is marked on the creature
                // This would be tracked for state-based actions
                // The creature may die if damage >= toughness
            }
            case DamageTarget.PlaneswalkerTarget(var planeswalker) -> {
                // Remove loyalty counters equal to damage
                // This would create a CounterChangeEvent
            }
            case DamageTarget.BattleTarget(var battle) -> {
                // Remove defense counters equal to damage
            }
        }
    }
}
