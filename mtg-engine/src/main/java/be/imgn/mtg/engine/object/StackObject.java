package be.imgn.mtg.engine.object;

/// An object that can exist on the stack.
public sealed interface StackObject permits Spell, AbilityOnStack {}
