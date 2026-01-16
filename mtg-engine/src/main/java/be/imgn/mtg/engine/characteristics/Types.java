package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultTypes;

/// The type line characteristic of a game object ({@mtg.rule 205.2}).
///
/// The type line contains the card type(s), and may also contain supertypes and/or subtypes.
/// Some objects can have more than one card type.
public interface Types extends Characteristics<Type> {

    /// Returns an empty Types collection.
    static Types empty() {
        return DefaultTypes.empty();
    }

    /// Returns a Types collection containing the specified types.
    static Types of(Type... types) {
        return DefaultTypes.of(types);
    }

    /// Returns a new builder for Types.
    static Builder builder() {
        return DefaultTypes.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains Artifact.
    default boolean isArtifact() {
        return contains(Type.ARTIFACT);
    }

    /// Returns true if this contains Battle.
    default boolean isBattle() {
        return contains(Type.BATTLE);
    }

    /// Returns true if this contains Creature.
    default boolean isCreature() {
        return contains(Type.CREATURE);
    }

    /// Returns true if this contains Enchantment.
    default boolean isEnchantment() {
        return contains(Type.ENCHANTMENT);
    }

    /// Returns true if this contains Instant.
    default boolean isInstant() {
        return contains(Type.INSTANT);
    }

    /// Returns true if this contains Land.
    default boolean isLand() {
        return contains(Type.LAND);
    }

    /// Returns true if this contains Planeswalker.
    default boolean isPlaneswalker() {
        return contains(Type.PLANESWALKER);
    }

    /// Returns true if this contains Sorcery.
    default boolean isSorcery() {
        return contains(Type.SORCERY);
    }

    /// Returns true if any type is a permanent type.
    default boolean isPermanentType() {
        return stream().anyMatch(Type::isPermanentType);
    }

    /// Returns true if any type is a spell type.
    default boolean isSpellType() {
        return stream().anyMatch(Type::isSpellType);
    }

    /// Builder for Types.
    interface Builder extends Characteristics.Builder<Type, Types, Builder> {}
}
