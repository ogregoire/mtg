package be.imgn.mtg.engine.characteristics;

/// The card types in Magic ({@mtg.rule 205.2a}).
///
/// The card types are artifact, battle, creature, enchantment, instant, land, planeswalker,
/// and sorcery. An object's card type determines what rules apply to it.
public enum Type {
    /// Artifact card type.
    ARTIFACT(true, false),
    /// Battle card type.
    BATTLE(true, false),
    /// Creature card type.
    CREATURE(true, false),
    /// Enchantment card type.
    ENCHANTMENT(true, false),
    /// Instant card type.
    INSTANT(false, true),
    /// Land card type.
    LAND(true, false),
    /// Planeswalker card type.
    PLANESWALKER(true, false),
    /// Sorcery card type.
    SORCERY(false, true);

    private final boolean permanentType;
    private final boolean spellType;

    Type(boolean permanentType, boolean spellType) {
        this.permanentType = permanentType;
        this.spellType = spellType;
    }

    /// Returns true if this is a permanent type.
    ///
    /// @return true if permanent type
    public boolean isPermanentType() {
        return permanentType;
    }

    /// Returns true if this is a spell type (instant or sorcery).
    ///
    /// @return true if spell type
    public boolean isSpellType() {
        return spellType;
    }
}
