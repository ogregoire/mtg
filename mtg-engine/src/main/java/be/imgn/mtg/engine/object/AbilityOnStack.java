package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.object.internal.DefaultAbilityOnStack;

/// An ability on the stack.
public non-sealed interface AbilityOnStack extends GameObject, StackObject {

    /// Returns the ability that was activated or triggered.
    Ability ability();

    /// Returns the source of this ability.
    GameObject source();

    /// Returns a new builder for AbilityOnStack with the given ability and source.
    /// The owner and controller are derived from the source.
    static Builder from(Ability ability, GameObject source) {
        return DefaultAbilityOnStack.from(ability, source);
    }

    /// Builder for AbilityOnStack.
    non-sealed interface Builder extends GameObject.Builder<AbilityOnStack, Builder> {}
}
