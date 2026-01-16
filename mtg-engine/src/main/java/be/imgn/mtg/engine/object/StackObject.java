package be.imgn.mtg.engine.object;

/// An object that can exist on the stack.
///
/// This sealed interface represents objects that use the stack:
/// - [Spell]: A card or copy being cast
/// - [AbilityOnStack]: An activated or triggered ability
///
/// Stack objects can be responded to and may be countered.
public sealed interface StackObject permits Spell, AbilityOnStack {}
