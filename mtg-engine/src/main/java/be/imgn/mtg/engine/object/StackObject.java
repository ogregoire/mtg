package be.imgn.mtg.engine.object;

/// An object on the stack ({@mtg.rule 405}).
///
/// The stack is a zone where spells and abilities wait to resolve. Objects on the stack
/// resolve in last-in, first-out (LIFO) order. Players may respond to objects on the stack
/// by casting spells or activating abilities.
///
/// The stack contains only [Spell]s and [AbilityOnStack] abilities. When the stack is
/// empty and all players pass priority, the game moves to the next step or phase.
public sealed interface StackObject permits Spell, AbilityOnStack {}
