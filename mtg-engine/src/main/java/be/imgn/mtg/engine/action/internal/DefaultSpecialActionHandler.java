package be.imgn.mtg.engine.action.internal;

import java.util.List;

import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;

/// Default implementation of the special action handler.
///
/// Handles special actions that don't use the stack, such as playing lands
/// and other Rule 116 actions.
@SuppressWarnings("unused") // TODO: Remove when special actions are fully implemented
final class DefaultSpecialActionHandler implements SpecialActionHandler {

    private final GameEventProcessor eventProcessor;

    DefaultSpecialActionHandler(GameEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public ExecutionResult playLand(PlayerAction.PlayLand action, GameState state) {
        // TODO: Implement land playing
        // 1. Get the card from hand
        // 2. Create EntersBattlefieldEvent
        // 3. Process through eventProcessor
        // 4. Record land drop used
        //
        // var card = state.hand(action.player()).findCard(action.landId()).orElseThrow();
        // var event = new EntersBattlefieldEvent(
        //     card.asPermanent(action.player()),
        //     ZoneType.HAND,
        //     new EtbCause.LandPlayed()
        // );
        // eventProcessor.process(event);
        // state.recordLandPlayed(action.player());

        // Stub: return success with empty events
        return new ExecutionResult.Success(List.of());
    }

    @Override
    public ExecutionResult execute(PlayerAction.SpecialAction action, GameState state) {
        return switch (action.actionType()) {
            case PLAY_LAND ->
                // This shouldn't happen - PlayLand actions use the dedicated playLand method
                new ExecutionResult.Illegal("Use PlayLand action to play lands");
            case TURN_FACE_UP -> turnFaceUp(action, state);
            case SUSPEND -> suspend(action, state);
            case COMPANION -> companion(action, state);
            case FORETELL -> foretell(action, state);
        };
    }

    private ExecutionResult turnFaceUp(PlayerAction.SpecialAction action, GameState state) {
        // TODO: Implement morph turn face up
        // 1. Find the face-down permanent
        // 2. Verify the player controls it
        // 3. Pay the morph cost if applicable
        // 4. Turn it face up
        // 5. This doesn't use the stack
        return new ExecutionResult.Success(List.of());
    }

    private ExecutionResult suspend(PlayerAction.SpecialAction action, GameState state) {
        // TODO: Implement suspend
        // 1. Verify the card is in hand and has suspend
        // 2. Pay the suspend cost
        // 3. Exile the card with time counters
        return new ExecutionResult.Success(List.of());
    }

    private ExecutionResult companion(PlayerAction.SpecialAction action, GameState state) {
        // TODO: Implement companion
        // 1. Verify a companion was declared for the game
        // 2. Verify companion hasn't been used yet
        // 3. Pay 3 generic mana
        // 4. Put companion into hand
        return new ExecutionResult.Success(List.of());
    }

    private ExecutionResult foretell(PlayerAction.SpecialAction action, GameState state) {
        // TODO: Implement foretell
        // 1. Verify the card is in hand
        // 2. Pay 2 generic mana
        // 3. Exile the card face-down
        return new ExecutionResult.Success(List.of());
    }
}
