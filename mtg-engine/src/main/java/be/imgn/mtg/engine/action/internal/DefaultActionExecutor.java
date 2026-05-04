package be.imgn.mtg.engine.action.internal;

import java.util.List;

import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.cost.CostContext;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.CastEvent;
import be.imgn.mtg.engine.zone.ZoneType;

/// Default implementation of the action executor.
///
/// Dispatches player actions to appropriate handlers and manages priority.
final class DefaultActionExecutor implements ActionExecutor {

    private final TurnTracker turnTracker;
    private final SpecialActionHandler specialActionHandler;
    private final AbilityManager abilityManager;
    private final GameEventProcessor eventProcessor;

    DefaultActionExecutor(
            TurnTracker turnTracker,
            SpecialActionHandler specialActionHandler,
            AbilityManager abilityManager,
            GameEventProcessor eventProcessor) {
        this.turnTracker = turnTracker;
        this.specialActionHandler = specialActionHandler;
        this.abilityManager = abilityManager;
        this.eventProcessor = eventProcessor;
    }

    @Override
    public ExecutionResult execute(PlayerAction action, GameState state) {
        return doExecute(action, state);
    }

    private ExecutionResult doExecute(PlayerAction action, GameState state) {
        return switch (action) {
            case PlayerAction.Pass pass -> executePass(pass);
            case PlayerAction.PlayLand playLand -> specialActionHandler.playLand(playLand, state);
            case PlayerAction.SpecialAction special -> specialActionHandler.execute(special, state);
            case PlayerAction.CastSpell cast -> executeCastSpell(cast);
            case PlayerAction.ActivateAbility activate -> executeActivateAbility(activate, state);
        };
    }

    private ExecutionResult executePass(PlayerAction.Pass pass) {
        turnTracker.passPriority(pass.player());
        return new ExecutionResult.Success(List.of());
    }

    private ExecutionResult executeCastSpell(PlayerAction.CastSpell cast) {
        var card = cast.card();
        var player = cast.player();

        // Pay mana cost
        var manaCost = card.manaCost();
        if (manaCost != null && !manaCost.isEmpty()) {
            var context = new CostContext(player, card);
            player.pay(manaCost, context);
        }

        // Process cast event — ZoneChangeResolver handles card→spell→stack
        var castEvent = new CastEvent(card, ZoneType.HAND, player, cast.context());
        eventProcessor.process(castEvent);

        return new ExecutionResult.Success(List.of(castEvent));
    }

    private ExecutionResult executeActivateAbility(PlayerAction.ActivateAbility activate, GameState state) {
        var source = activate.source();

        // Get the ability by index (validated earlier, should be valid)
        var abilities = source.abilities().stream().toList();
        if (activate.abilityIndex() < 0 || activate.abilityIndex() >= abilities.size()) {
            return new ExecutionResult.Illegal("Invalid ability index");
        }

        var ability = abilities.get(activate.abilityIndex());
        if (!(ability instanceof ActivatedAbility activated)) {
            return new ExecutionResult.Illegal("Not an activated ability");
        }

        // Create the ability context
        var context = new AbilityContext(source, activate.player(), state);

        // Activate via AbilityManager
        var result = abilityManager.activate(activated, source, context);

        return switch (result) {
            case ActivationResult.Success success -> new ExecutionResult.Success(success.events());
            case ActivationResult.ManaAbilitySuccess mana -> new ExecutionResult.Success(mana.events());
            case ActivationResult.Illegal illegal -> new ExecutionResult.Illegal(illegal.reason());
        };
    }
}
