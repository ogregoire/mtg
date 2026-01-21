package be.imgn.mtg.engine.ability;

import java.util.List;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.object.AbilityOnStack;

/// Result of attempting to activate an ability.
///
/// The result is a sealed type with three possible outcomes:
/// - [Success]: The ability was put on the stack successfully
/// - [ManaAbilitySuccess]: A mana ability was activated and resolved immediately
/// - [Illegal]: The activation was illegal (no costs paid, no state change)
public sealed interface ActivationResult {

    /// Successful activation of a non-mana ability.
    ///
    /// The ability has been put on the stack and costs have been paid.
    ///
    /// @param abilityOnStack the ability object on the stack
    /// @param events events generated during activation (cost payment, etc.)
    record Success(AbilityOnStack abilityOnStack, List<GameEvent> events) implements ActivationResult {}

    /// Successful activation of a mana ability.
    ///
    /// Mana abilities don't use the stack and resolve immediately.
    ///
    /// @param events events generated during activation and resolution
    record ManaAbilitySuccess(List<GameEvent> events) implements ActivationResult {}

    /// The activation was illegal and did not occur.
    ///
    /// No costs were paid and no state change occurred.
    ///
    /// @param reason the reason the activation was illegal
    record Illegal(String reason) implements ActivationResult {}
}
