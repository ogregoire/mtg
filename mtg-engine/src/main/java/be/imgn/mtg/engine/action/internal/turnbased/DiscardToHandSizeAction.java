package be.imgn.mtg.engine.action.internal.turnbased;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Active player discards down to maximum hand size ({@mtg.rule 514.1}).
///
/// As a turn-based action during the cleanup step, if the active player has
/// more cards in hand than their maximum hand size (usually 7), they discard
/// cards of their choice until they have that many cards.
///
/// Note: This is a partial implementation. Full implementation requires:
/// - Hand zone with card count
/// - Maximum hand size tracking (default 7, modified by effects)
/// - Player choice interface for selecting cards to discard
/// - DiscardEvent for the GameEventProcessor
public final class DiscardToHandSizeAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.CLEANUP_DISCARD;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        // TODO: Implement discard to hand size
        // 1. Get active player's hand size and maximum hand size
        // 2. If hand size > max, have player choose cards to discard
        // 3. Process discard events for chosen cards
        //
        // var activePlayer = state.activePlayer();
        // var hand = state.hand(activePlayer);
        // var handSize = hand.size();
        // var maxHandSize = state.maxHandSize(activePlayer); // Default 7
        //
        // if (handSize > maxHandSize) {
        //     int toDiscard = handSize - maxHandSize;
        //
        //     // Player chooses which cards to discard
        //     var chosen = state.playerChoosesCards(
        //         activePlayer,
        //         hand.cards(),
        //         toDiscard,
        //         "Choose cards to discard to maximum hand size"
        //     );
        //
        //     // Create discard events
        //     var events = chosen.stream()
        //         .map(card -> new DiscardEvent(card.id(), activePlayer,
        //             new DiscardCause.CleanupStep()))
        //         .toList();
        //
        //     processor.processBatch(events);
        // }
    }
}
