package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultTypes;

/// The type line characteristic of a game object ({@mtg.rule 205.2}).
///
/// The type line contains the card type(s), and may also contain supertypes and/or subtypes.
/// Some objects can have more than one card type.
public interface Types extends Characteristics<Type> {

    /// Returns an empty Types collection.
    ///
    /// @return an empty collection
    static Types empty() {
        return DefaultTypes.empty();
    }

    /// Returns a Types collection containing the specified types.
    ///
    /// @param types the types to include
    /// @return a collection containing the types
    static Types of(Type... types) {
        return DefaultTypes.of(types);
    }

    /// Returns a new builder for Types.
    ///
    /// @return a new builder
    static Builder builder() {
        return DefaultTypes.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains Artifact.
    ///
    /// @return true if artifact is present
    default boolean isArtifact() {
        return contains(Type.ARTIFACT);
    }

    /// Returns true if this contains Battle.
    ///
    /// @return true if battle is present
    default boolean isBattle() {
        return contains(Type.BATTLE);
    }

    /// Returns true if this contains Creature.
    ///
    /// @return true if creature is present
    default boolean isCreature() {
        return contains(Type.CREATURE);
    }

    /// Returns true if this contains Enchantment.
    ///
    /// @return true if enchantment is present
    default boolean isEnchantment() {
        return contains(Type.ENCHANTMENT);
    }

    /// Returns true if this contains Instant.
    ///
    /// @return true if instant is present
    default boolean isInstant() {
        return contains(Type.INSTANT);
    }

    /// Returns true if this contains Land.
    ///
    /// @return true if land is present
    default boolean isLand() {
        return contains(Type.LAND);
    }

    /// Returns true if this contains Planeswalker.
    ///
    /// @return true if planeswalker is present
    default boolean isPlaneswalker() {
        return contains(Type.PLANESWALKER);
    }

    /// Returns true if this contains Sorcery.
    ///
    /// @return true if sorcery is present
    default boolean isSorcery() {
        return contains(Type.SORCERY);
    }

    /// Returns true if any type is a permanent type.
    ///
    /// @return true if any permanent type is present
    default boolean isPermanentType() {
        return stream().anyMatch(Type::isPermanentType);
    }

    /// Returns true if any type is a spell type.
    ///
    /// @return true if any spell type is present
    default boolean isSpellType() {
        return stream().anyMatch(Type::isSpellType);
    }

    /// Builder for Types.
    interface Builder extends Characteristics.Builder<Type, Types, Builder> {}
}
