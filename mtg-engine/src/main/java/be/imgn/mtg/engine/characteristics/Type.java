package be.imgn.mtg.engine.characteristics;

/// The card types in Magic ({@mtg.rule 205.2a}).
///
/// The card types are artifact, battle, creature, enchantment, instant, land, planeswalker,
/// and sorcery. An object's card type determines what rules apply to it.
public enum Type {
    /// Artifact card type.
    ARTIFACT("artifact", true, false),
    /// Battle card type.
    BATTLE("battle", true, false),
    /// Creature card type.
    CREATURE("creature", true, false),
    /// Enchantment card type.
    ENCHANTMENT("enchantment", true, false),
    /// Instant card type.
    INSTANT("instant", false, true),
    /// Land card type.
    LAND("land", true, false),
    /// Planeswalker card type.
    PLANESWALKER("planeswalker", true, false),
    /// Sorcery card type.
    SORCERY("sorcery", false, true);

    private final String text;
    private final boolean permanentType;
    private final boolean spellType;

    Type(String text, boolean permanentType, boolean spellType) {
        this.text = text;
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

    /// Returns the text representation of this type.
    ///
    /// @return the type text
    public String text() {
        return text;
    }
}
