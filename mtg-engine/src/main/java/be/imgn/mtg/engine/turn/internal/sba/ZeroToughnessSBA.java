package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// State-based action: A creature with 0 or less toughness is put into
/// its owner's graveyard ({@mtg.rule 704.5f}).
///
/// Unlike lethal damage, this puts the creature into the graveyard directly.
/// It's not destruction, so indestructible doesn't prevent it and regeneration
/// can't replace it.
///
/// Note: This is a stub implementation. Full implementation requires:
/// - Creature/permanent tracking on the battlefield
/// - Toughness calculation (may be modified by continuous effects)
public final class ZeroToughnessSBA implements StateBasedAction {

    public ZeroToughnessSBA() {}

    @Override
    public boolean appliesTo(GameState gameState) {
        // TODO: Check if any creature has 0 or less toughness
        // for (Permanent permanent : gameState.battlefield().creatures()) {
        //     if (permanent.toughness() <= 0) {
        //         return true;
        //     }
        // }
        return false;
    }

    @Override
    public void apply(GameState gameState) {
        // TODO: Implement when permanent tracking is added
        // List<Permanent> toPutInGraveyard = new ArrayList<>();
        // for (Permanent permanent : gameState.battlefield().creatures()) {
        //     if (permanent.toughness() <= 0) {
        //         toPutInGraveyard.add(permanent);
        //     }
        // }
        //
        // // Put all 0-toughness creatures into their owners' graveyards simultaneously
        // for (Permanent permanent : toPutInGraveyard) {
        //     moveToGraveyard(permanent, gameState);
        // }
    }
}
