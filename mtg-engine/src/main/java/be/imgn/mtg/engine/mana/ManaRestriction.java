package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.object.TypedObject;

/// A restriction on how mana can be spent ({@mtg.rule 106.12}).
///
/// Some effects produce mana with restrictions on how it can be spent.
/// For example, "Add {C}. Spend this mana only to cast artifact spells."
public sealed interface ManaRestriction {

    /// Returns true if this mana can be spent on the given game object.
    ///
    /// @param target the game object being paid for
    /// @return true if this mana can be spent on that object
    boolean canSpendOn(TypedObject target);

    /// A restriction that limits spending to objects with a specific card type.
    ///
    /// @param type the type that this mana can be spent on
    record TypeRestriction(Type type) implements ManaRestriction {
        @Override
        public boolean canSpendOn(TypedObject target) {
            return target.types().contains(this.type);
        }
    }
}
