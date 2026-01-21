package be.imgn.mtg.engine.action.internal;

import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Default implementation of the action validator.
///
/// Validates player actions against game rules and current state.
final class DefaultActionValidator implements ActionValidator {

    private final PrioritySystem prioritySystem;

    DefaultActionValidator(PrioritySystem prioritySystem) {
        this.prioritySystem = prioritySystem;
    }

    @Override
    public ValidationResult validate(PlayerAction action, GameState state) {
        return switch (action) {
            case PlayerAction.Pass pass -> validatePass(pass);
            case PlayerAction.PlayLand playLand -> validatePlayLand(playLand, state);
            case PlayerAction.SpecialAction special -> validateSpecialAction(special, state);
            case PlayerAction.CastSpell cast -> validateCastSpell(cast, state);
            case PlayerAction.ActivateAbility activate -> validateActivateAbility(activate, state);
        };
    }

    private ValidationResult validatePass(PlayerAction.Pass pass) {
        // Pass always requires priority
        var priorityHolder = prioritySystem.currentPriorityHolder();
        if (priorityHolder == null || !priorityHolder.equals(pass.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult validatePlayLand(PlayerAction.PlayLand playLand, GameState state) {
        // Playing a land requires priority
        var priorityHolder = prioritySystem.currentPriorityHolder();
        if (priorityHolder == null || !priorityHolder.equals(playLand.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }

        // Must be during the player's main phase
        // TODO: Check if it's the player's main phase and stack is empty
        // For now, we accept it (full timing check requires TurnTracker state)

        // Check if the land is in the player's hand
        var hand = state.hand(playLand.player());
        var landObject = hand.findById(playLand.landId());
        if (landObject.isEmpty()) {
            return new ValidationResult.Illegal("Land is not in player's hand", IllegalActionType.NOT_IN_ZONE);
        }

        // TODO: Check if player has land drops remaining
        // For now, we accept it (land drop tracking not yet implemented)

        return new ValidationResult.Legal();
    }

    private ValidationResult validateSpecialAction(PlayerAction.SpecialAction special, GameState state) {
        // Check priority requirement based on action type
        if (special.actionType().requiresPriority()) {
            var priorityHolder = prioritySystem.currentPriorityHolder();
            if (priorityHolder == null || !priorityHolder.equals(special.player())) {
                return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
            }
        }

        // TODO: Add specific validation for each SpecialActionType
        // - TURN_FACE_UP: Check if the permanent is face-down and controlled by player
        // - SUSPEND: Check if card has suspend and timing is correct
        // - COMPANION: Check if companion was declared and not already used
        // - FORETELL: Check timing and if card is in hand

        return new ValidationResult.Legal();
    }

    private ValidationResult validateCastSpell(PlayerAction.CastSpell cast, GameState state) {
        // Cast spell requires priority
        var priorityHolder = prioritySystem.currentPriorityHolder();
        if (priorityHolder == null || !priorityHolder.equals(cast.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }

        // TODO: Full spell casting validation (Rule 601)
        // - Check if spell can be cast at instant speed or timing is correct
        // - Check if targets are legal
        // - Check if costs can be paid
        // - Check restriction effects

        return new ValidationResult.Legal();
    }

    private ValidationResult validateActivateAbility(PlayerAction.ActivateAbility activate, GameState state) {
        // Ability activation requires priority (except mana abilities)
        var priorityHolder = prioritySystem.currentPriorityHolder();
        if (priorityHolder == null || !priorityHolder.equals(activate.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }

        // TODO: Full ability activation validation (Rule 602)
        // - Check if ability can be activated
        // - Check if targets are legal
        // - Check if costs can be paid
        // - Check restriction effects

        return new ValidationResult.Legal();
    }
}
