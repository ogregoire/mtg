package be.imgn.mtg.engine.action.internal;

import java.util.List;

import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PlayerAction;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Default implementation of the action executor.
///
/// Dispatches player actions to appropriate handlers and manages priority reset.
final class DefaultActionExecutor implements ActionExecutor {

    private final PrioritySystem prioritySystem;
    private final SpecialActionHandler specialActionHandler;

    DefaultActionExecutor(PrioritySystem prioritySystem, SpecialActionHandler specialActionHandler) {
        this.prioritySystem = prioritySystem;
        this.specialActionHandler = specialActionHandler;
    }

    @Override
    public ExecutionResult execute(PlayerAction action, GameState state) {
        var result = doExecute(action, state);

        // On successful non-Pass actions, reset priority to clear pass state
        if (result instanceof ExecutionResult.Success && !(action instanceof PlayerAction.Pass)) {
            prioritySystem.reset();
        }

        return result;
    }

    private ExecutionResult doExecute(PlayerAction action, GameState state) {
        return switch (action) {
            case PlayerAction.Pass pass -> executePass(pass);
            case PlayerAction.PlayLand playLand -> specialActionHandler.playLand(playLand, state);
            case PlayerAction.SpecialAction special -> specialActionHandler.execute(special, state);
            case PlayerAction.CastSpell cast -> executeCastSpell(cast, state);
            case PlayerAction.ActivateAbility activate -> executeActivateAbility(activate, state);
        };
    }

    private ExecutionResult executePass(PlayerAction.Pass pass) {
        prioritySystem.pass(pass.player());
        return new ExecutionResult.Success(List.of());
    }

    private ExecutionResult executeCastSpell(PlayerAction.CastSpell cast, GameState state) {
        // TODO: Implement spell casting (Rule 601)
        // This is future work - requires SpellCastingProcess implementation
        throw new UnsupportedOperationException("Spell casting not yet implemented");
    }

    private ExecutionResult executeActivateAbility(PlayerAction.ActivateAbility activate, GameState state) {
        // TODO: Implement ability activation (Rule 602)
        // This is future work - requires AbilityActivationProcess implementation
        throw new UnsupportedOperationException("Ability activation not yet implemented");
    }
}
