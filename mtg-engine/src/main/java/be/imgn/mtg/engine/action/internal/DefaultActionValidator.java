package be.imgn.mtg.engine.action.internal;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.action.ActionValidator;
import be.imgn.mtg.engine.action.IllegalActionType;
import be.imgn.mtg.engine.action.LandPlayedEvent;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.ValidationResult;
import be.imgn.mtg.engine.action.ValidationResult.ValidationError;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
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
            case PlayerAction.CastSpell cast -> validateCastSpell(cast, state);
            case PlayerAction.ActivateAbility activate -> validateActivateAbility(activate, state);
        };
    }

    private ValidationResult validatePass(PlayerAction.Pass pass) {
        return requirePriority(pass.player());
    }

    private ValidationResult validatePlayLand(PlayerAction.PlayLand playLand, GameState state) {
        return ValidationResult.merge(
                requirePriority(playLand.player()),
                requireActivePlayer(playLand.player()),
                requirePhase(Phase.MAIN),
                requireEmptyStack(state),
                requireInHand(playLand.land(), playLand.player(), state),
                requireLandDropAvailable(playLand.player()));
    }

    private ValidationResult validateSpecialAction(PlayerAction.SpecialAction special) {
        // Check priority requirement based on action type
        if (special.actionType().requiresPriority()) {
            return requirePriority(special.player());
        }

        // TODO: Add specific validation for each SpecialActionType
        // - TURN_FACE_UP: Check if the permanent is face-down and controlled by player
        // - SUSPEND: Check if card has suspend and timing is correct
        // - COMPANION: Check if companion was declared and not already used
        // - FORETELL: Check timing and if card is in hand

        return new ValidationResult.Legal();
    }

    private ValidationResult validateCastSpell(PlayerAction.CastSpell cast, GameState state) {
        var card = cast.card();
        var player = cast.player();

        // Instants can be cast anytime with priority; non-instants need sorcery-speed timing
        if (card.types().isInstant()) {
            return ValidationResult.merge(requirePriority(player), requireInHand(card, player, state));
        }

        return ValidationResult.merge(
                requirePriority(player),
                requireInHand(card, player, state),
                requireActivePlayer(player),
                requirePhase(Phase.MAIN),
                requireEmptyStack(state));
    }

    private ValidationResult validateActivateAbility(PlayerAction.ActivateAbility activate, GameState state) {
        return findActivatedAbility(activate, state)
                .map(found -> validateActivation(found, activate.player(), state))
                .orElseGet(() -> findActivatedAbilityError(activate, state));
    }

    // -- Reusable check methods --

    private ValidationResult requirePriority(Player player) {
        if (!turnTracker.hasPriority(player)) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Player does not have priority", IllegalActionType.NO_PRIORITY)));
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult requireActivePlayer(Player player) {
        if (!turnTracker.activePlayer().equals(player)) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Not the active player's turn", IllegalActionType.WRONG_TIMING)));
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult requirePhase(Phase phase) {
        if (turnTracker.currentPhase() != phase) {
            return new ValidationResult.Illegal(List.of(
                    new ValidationError("Can only be done during a main phase", IllegalActionType.WRONG_TIMING)));
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult requireEmptyStack(GameState state) {
        if (!state.stack().all().isEmpty()) {
            return new ValidationResult.Illegal(List.of(new ValidationError(
                    "Cannot be done while the stack is not empty", IllegalActionType.WRONG_TIMING)));
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult requireInHand(Card card, Player player, GameState state) {
        if (!state.hand(player).contains(card)) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Card is not in player's hand", IllegalActionType.NOT_IN_ZONE)));
        }
        return new ValidationResult.Legal();
    }

    private ValidationResult requireLandDropAvailable(Player player) {
        var landsPlayedThisTurn = eventTracker
                .eventsFromThisTurn(LandPlayedEvent.class)
                .filter(e -> e.player().equals(player))
                .count();
        if (landsPlayedThisTurn >= 1) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("No land drops remaining", IllegalActionType.LAND_ALREADY_PLAYED)));
        }
        return new ValidationResult.Legal();
    }

    // -- Activated ability helpers --

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

    /// Returns the appropriate error for when ability extraction fails.
    private ValidationResult findActivatedAbilityError(PlayerAction.ActivateAbility activate, GameState state) {
        var source = activate.source();
        if (state.findZone(source).isEmpty()) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Source object not found", IllegalActionType.ILLEGAL_TARGET)));
        }
        var abilities = source.abilities().stream().toList();
        if (activate.abilityIndex() < 0 || activate.abilityIndex() >= abilities.size()) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Invalid ability index", IllegalActionType.ILLEGAL_TARGET)));
        }
        return new ValidationResult.Illegal(
                List.of(new ValidationError("Not an activated ability", IllegalActionType.RESTRICTION_VIOLATED)));
    }

    /// Validates that the activation is legal given the context.
    private ValidationResult validateActivation(ActivationContext ctx, Player player, GameState state) {
        // Mana abilities don't require priority (Rule 605.3a)
        if (!ctx.ability().isManaAbility() && !turnTracker.hasPriority(player)) {
            return new ValidationResult.Illegal(
                    List.of(new ValidationError("Player does not have priority", IllegalActionType.NO_PRIORITY)));
        }
        // Delegate full validation to AbilityManager (Rule 602)
        if (!abilityManager.canActivate(ctx.ability(), ctx.source(), state)) {
            return new ValidationResult.Illegal(List.of(
                    new ValidationError("Ability cannot be activated", IllegalActionType.RESTRICTION_VIOLATED)));
        }
        return new ValidationResult.Legal();
    }

    /// Holds the source object and activated ability for validation.
    private record ActivationContext(TypedObject source, ActivatedAbility ability) {}
}
