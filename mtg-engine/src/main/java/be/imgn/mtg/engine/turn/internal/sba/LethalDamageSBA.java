package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// State-based action: A creature with lethal damage is destroyed ({@mtg.rule 704.5g}).
///
/// A creature has lethal damage if it has damage marked on it equal to or greater
/// than its toughness. Such a creature is destroyed.
///
/// Note: This SBA doesn't apply to creatures with indestructible.
///
/// Note: This is a stub implementation. Full implementation requires:
/// - Creature/permanent tracking on the battlefield
/// - Damage marked on permanents
/// - Toughness calculation (may be modified by effects)
/// - Indestructible ability checking
public final class LethalDamageSBA implements StateBasedAction {

    public LethalDamageSBA() {}

    @Override
    public boolean appliesTo(GameState gameState) {
        // TODO: Check if any creature has lethal damage
        // for (Permanent permanent : gameState.battlefield().creatures()) {
        //     if (!permanent.hasAbility(Ability.INDESTRUCTIBLE)) {
        //         int damage = permanent.damageMarked();
        //         int toughness = permanent.toughness();
        //         if (damage >= toughness && toughness > 0) {
        //             return true;
        //         }
        //     }
        // }
        return false;
    }

    @Override
    public void apply(GameState gameState) {
        // TODO: Implement when permanent/damage tracking is added
        // List<Permanent> toDestroy = new ArrayList<>();
        // for (Permanent permanent : gameState.battlefield().creatures()) {
        //     if (!permanent.hasAbility(Ability.INDESTRUCTIBLE)) {
        //         int damage = permanent.damageMarked();
        //         int toughness = permanent.toughness();
        //         if (damage >= toughness && toughness > 0) {
        //             toDestroy.add(permanent);
        //         }
        //     }
        // }
        //
        // // Destroy all creatures with lethal damage simultaneously
        // for (Permanent permanent : toDestroy) {
        //     destroy(permanent, gameState);
        // }
    }
}
