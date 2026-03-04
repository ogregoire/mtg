package be.imgn.mtg.engine.action.internal;

import java.util.Optional;

import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.LandPlayedEvent;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.TurnTracker;

/// Default implementation of the action validator.
///
/// Validates player actions against game rules and current state.
final class DefaultActionValidator implements ActionValidator {

    private final TurnTracker turnTracker;
    private final AbilityManager abilityManager;
    private final EventTracker eventTracker;

    DefaultActionValidator(TurnTracker turnTracker, AbilityManager abilityManager, EventTracker eventTracker) {
        this.turnTracker = turnTracker;
        this.abilityManager = abilityManager;
        this.eventTracker = eventTracker;
    }

    @Override
    public ValidationResult validate(PlayerAction action, GameState state) {
        return switch (action) {
            case PlayerAction.Pass pass -> validatePass(pass);
            case PlayerAction.PlayLand playLand -> validatePlayLand(playLand, state);
            case PlayerAction.SpecialAction special -> validateSpecialAction(special);
            case PlayerAction.CastSpell cast -> validateCastSpell(cast);
            case PlayerAction.ActivateAbility activate -> validateActivateAbility(activate, state);
        };
    }

    private ValidationResult validatePass(PlayerAction.Pass pass) {
        // Pass always requires priority
        if (!turnTracker.hasPriority(pass.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult validatePlayLand(PlayerAction.PlayLand playLand, GameState state) {
        // Playing a land requires priority
        if (!turnTracker.hasPriority(playLand.player())) {
            return new ValidationResult.Illegal("Player does not have priority", IllegalActionType.NO_PRIORITY);
        }

        // Must be the active player's turn
        if (!turnTracker.activePlayer().equals(playLand.player())) {
            return new ValidationResult.Illegal("Not the active player's turn", IllegalActionType.WRONG_TIMING);
        }

        // Must be during a main phase with empty stack (Rule 305.1)
        if (turnTracker.currentPhase() != Phase.MAIN) {
            return new ValidationResult.Illegal(
                    "Can only play lands during a main phase", IllegalActionType.WRONG_TIMING);
        }
        if (!state.stack().all().isEmpty()) {
            return new ValidationResult.Illegal(
                    "Cannot play a land while the stack is not empty", IllegalActionType.WRONG_TIMING);
        }

        // Check if the land is in the player's hand
        var hand = state.hand(playLand.player());
        if (!hand.contains(playLand.land())) {
            return new ValidationResult.Illegal("Land is not in player's hand", IllegalActionType.NOT_IN_ZONE);
        }

        // Check if player has land drops remaining (Rule 305.2)
        var landsPlayedThisTurn = eventTracker
                .eventsFromThisTurn(LandPlayedEvent.class)
                .filter(e -> e.player().equals(playLand.player()))
                .count();
        if (landsPlayedThisTurn >= 1) {
            return new ValidationResult.Illegal("No land drops remaining", IllegalActionType.LAND_ALREADY_PLAYED);
        }

        return new ValidationResult.Legal();
    }

    private ValidationResult validateSpecialAction(PlayerAction.SpecialAction special) {
        // Check priority requirement based on action type
        if (special.actionType().requiresPriority()) {
            if (!turnTracker.hasPriority(special.player())) {
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

    private ValidationResult validateCastSpell(PlayerAction.CastSpell cast) {
        // Cast spell requires priority
        if (!turnTracker.hasPriority(cast.player())) {
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
        var source = activate.source();
        if (state.findZone(source).isEmpty()) {
            return Optional.empty();
        }
        var abilities = source.abilities().stream().toList();
        var index = activate.abilityIndex();
        if (index < 0 || index >= abilities.size()) {
            return Optional.empty();
        }
        return abilities.get(index) instanceof ActivatedAbility activated
                ? Optional.of(new ActivationContext(source, activated))
                : Optional.empty();
    }

    /// Checks if the player currently has priority.
    private boolean hasPriority(Player player) {
        return turnTracker.hasPriority(player);
    }

    /// Returns the appropriate error for when ability extraction fails.
    private ValidationResult findActivatedAbilityError(PlayerAction.ActivateAbility activate, GameState state) {
        var source = activate.source();
        if (state.findZone(source).isEmpty()) {
            return new ValidationResult.Illegal("Source object not found", IllegalActionType.ILLEGAL_TARGET);
        }
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
    private record ActivationContext(TypedObject source, ActivatedAbility ability) {}
}
