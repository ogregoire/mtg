package be.imgn.mtg.engine.action.internal;

import java.util.Optional;

import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Default implementation of the action validator.
///
/// Validates player actions against game rules and current state.
final class DefaultActionValidator implements ActionValidator {

    private final PrioritySystem prioritySystem;
    private final AbilityManager abilityManager;

    DefaultActionValidator(PrioritySystem prioritySystem, AbilityManager abilityManager) {
        this.prioritySystem = prioritySystem;
        this.abilityManager = abilityManager;
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
        return findActivatedAbility(activate, state)
                .map(found -> validateActivation(found, activate.player(), state))
                .orElseGet(() -> findActivatedAbilityError(activate, state));
    }

    /// Finds the source object and activated ability from the action.
    private Optional<ActivationContext> findActivatedAbility(PlayerAction.ActivateAbility activate, GameState state) {
        return state.findObject(activate.sourceId()).flatMap(source -> {
            var abilities = source.abilities().stream().toList();
            var index = activate.abilityIndex();
            if (index < 0 || index >= abilities.size()) {
                return Optional.empty();
            }
            return abilities.get(index) instanceof ActivatedAbility activated
                    ? Optional.of(new ActivationContext(source, activated))
                    : Optional.empty();
        });
    }

    /// Checks if the player currently has priority.
    private boolean hasPriority(Player player) {
        var holder = prioritySystem.currentPriorityHolder();
        return holder != null && holder.equals(player);
    }

    /// Returns the appropriate error for when ability extraction fails.
    private ValidationResult findActivatedAbilityError(PlayerAction.ActivateAbility activate, GameState state) {
        if (state.findObject(activate.sourceId()).isEmpty()) {
            return new ValidationResult.Illegal("Source object not found", IllegalActionType.ILLEGAL_TARGET);
        }
        var source = state.findObject(activate.sourceId()).get();
        var abilities = source.abilities().stream().toList();
        if (activate.abilityIndex() < 0 || activate.abilityIndex() >= abilities.size()) {
            return new ValidationResult.Illegal("Invalid ability index", IllegalActionType.ILLEGAL_TARGET);
        }
        return new ValidationResult.Illegal("Not an activated ability", IllegalActionType.RESTRICTION_VIOLATED);
    }

    /// Validates that the activation is legal given the context.
    private ValidationResult validateActivation(ActivationContext ctx, Player player, GameState state) {
        // Mana abilities don't require priority (Rule 605.3a)
        if (!ctx.ability().isManaAbility() && !hasPriority(player)) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }
        // Delegate full validation to AbilityManager (Rule 602)
        if (!abilityManager.canActivate(ctx.ability(), ctx.source(), state)) {
            return new ValidationResult.Illegal("Ability cannot be activated", IllegalActionType.RESTRICTION_VIOLATED);
        }
        return new ValidationResult.Legal();
    }

    /// Holds the source object and activated ability for validation.
    private record ActivationContext(GameObject source, ActivatedAbility ability) {}
}
