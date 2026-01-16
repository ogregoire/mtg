package be.imgn.mtg.engine.characteristics;

/// The card types in Magic ({@mtg.rule 205.2a}).
///
/// The card types are artifact, battle, creature, enchantment, instant, land, planeswalker,
/// and sorcery. An object's card type determines what rules apply to it.
public enum Type {
    ARTIFACT(true, false),
    BATTLE(true, false),
    CREATURE(true, false),
    ENCHANTMENT(true, false),
    INSTANT(false, true),
    LAND(true, false),
    PLANESWALKER(true, false),
    SORCERY(false, true);

    private final boolean permanentType;
    private final boolean spellType;

    Type(boolean permanentType, boolean spellType) {
        this.permanentType = permanentType;
        this.spellType = spellType;
    }

    /// Returns true if this is a permanent type.
    public boolean isPermanentType() {
        return permanentType;
    }

    /// Returns true if this is a spell type (instant or sorcery).
    public boolean isSpellType() {
        return spellType;
    }
}
