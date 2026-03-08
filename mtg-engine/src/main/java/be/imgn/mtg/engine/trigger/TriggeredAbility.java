package be.imgn.mtg.engine.trigger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.ZoneType;

/// A triggered ability that triggers in response to game events ({@mtg.rule 603}).
///
/// Triggered abilities have three parts:
/// 1. The trigger condition (when/whenever/at)
/// 2. Optional intervening-if clause (checked on trigger and resolution)
/// 3. The effect
///
/// Triggered abilities can only trigger from certain zones (typically battlefield,
/// but also graveyard for some abilities like "dies" triggers).
///
/// Some triggered abilities are mana abilities ({@mtg.rule 605.1b}): triggered abilities
/// that trigger from activating a mana ability and could add mana when they resolve.
///
/// @see Ability
public non-sealed interface TriggeredAbility extends Ability {

    /// Returns the condition that causes this ability to trigger.
    ///
    /// @return the trigger condition
    TriggerCondition condition();

    /// Returns the optional intervening-if condition.
    ///
    /// An intervening-if clause (e.g., "When X enters the battlefield, if Y, do Z")
    /// is checked both when the ability would trigger and when it would resolve.
    /// If false at either time, the ability doesn't trigger or does nothing.
    ///
    /// @return the intervening-if condition, or empty if none
    Optional<Predicate<GameState>> interveningIf();

    /// Returns the zones from which this ability can trigger.
    ///
    /// Most triggered abilities only work while on the battlefield, but some
    /// work from other zones (e.g., "When this card is put into a graveyard from
    /// anywhere...").
    ///
    /// @return the set of zones where this ability is active
    Set<ZoneType> triggersFrom();

    /// Returns the effects that this ability produces when resolved.
    ///
    /// @return the list of effects, never null (may be empty)
    List<Effect> effects();

    /// Returns true if this is a mana ability ({@mtg.rule 605.1b}).
    ///
    /// A triggered ability is a mana ability if it:
    /// - Triggers from the resolution of an activated mana ability or from mana being added
    /// - Could add mana when it resolves
    /// - Doesn't require a target
    ///
    /// Triggered mana abilities don't use the stack and resolve immediately.
    ///
    /// @return true if this is a triggered mana ability
    boolean isManaAbility();
}
