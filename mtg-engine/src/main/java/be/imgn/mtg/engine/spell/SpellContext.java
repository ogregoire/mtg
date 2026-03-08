package be.imgn.mtg.engine.spell;

/// Context for a spell on the stack, carrying casting-time decisions.
///
/// @param targets the targets chosen when the spell was cast
public record SpellContext(TargetChoices targets) {

    /// Returns an empty spell context with no targets.
    public static SpellContext empty() {
        return new SpellContext(TargetChoices.empty());
    }
}
