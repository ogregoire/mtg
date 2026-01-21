package be.imgn.mtg.engine.ability;

import be.imgn.mtg.engine.trigger.TriggeredAbility;

/// An ability of a game object ({@mtg.rule 113}).
///
/// Abilities have four categories ({@mtg.rule 113.3}):
/// - [SpellAbility]: Instructions on an instant or sorcery spell ({@mtg.rule 113.3a})
/// - [ActivatedAbility]: "Cost: Effect" format, can be activated by players ({@mtg.rule 113.3b})
/// - [TriggeredAbility]: Triggers in response to game events ({@mtg.rule 113.3c})
/// - [StaticAbility]: Continuous effects that apply passively ({@mtg.rule 113.3d})
///
/// Mana abilities and loyalty abilities are not separate types but classifications
/// (checked via predicates on activated/triggered abilities).
///
/// Objects can have abilities from their rules text, from effects, or intrinsically.
///
/// @see SpellAbility
/// @see ActivatedAbility
/// @see TriggeredAbility
/// @see StaticAbility
public sealed interface Ability permits SpellAbility, ActivatedAbility, TriggeredAbility, StaticAbility {

    /// Returns the globally unique identifier for this ability.
    ///
    /// The ID remains stable across zone changes and can be used to track
    /// activations, link abilities, or identify delayed triggers.
    ///
    /// @return the ability ID, never null
    AbilityId id();

    /// Returns the oracle text of this ability.
    ///
    /// @return the ability's rules text, never null (may be empty)
    String oracleText();
}
