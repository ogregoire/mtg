package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Permanent that's neither an Aura, Equipment, nor Fortification but is attached
/// to another permanent or player becomes unattached ({@mtg.rule 704.5p}).
///
/// This handles Battles and other permanents that may become illegally attached.
// TODO Implement IllegalAttachmentSBA
final class IllegalAttachmentSBA implements StateBasedAction {

    private final GameState gameState;

    IllegalAttachmentSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
