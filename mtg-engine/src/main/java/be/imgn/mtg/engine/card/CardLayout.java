package be.imgn.mtg.engine.card;

/// The layout of a Magic card, describing how its faces are arranged.
///
/// Some layouts are double-faced, meaning the card has two physically printed faces.
/// Use [#isDoubleFaced()] to check this property.
public enum CardLayout {
    /// A standard single-faced card.
    NORMAL(false),
    /// A split card with two halves (e.g., Fire // Ice).
    SPLIT(false),
    /// A flip card with two orientations (Kamigawa block).
    FLIP(false),
    /// A transforming double-faced card.
    TRANSFORM(true),
    /// A modal double-faced card.
    MODAL_DFC(true),
    /// A meld card (single-faced; the combined result is a separate entity).
    MELD(false),
    /// A leveler card with level-up abilities.
    LEVELER(false),
    /// A Class enchantment card.
    CLASS(false),
    /// A Saga enchantment card.
    SAGA(false),
    /// An adventurer card with an Adventure spell.
    ADVENTURE(false),
    /// A mutate card.
    MUTATE(false),
    /// A prototype card.
    PROTOTYPE(false),
    /// A battle card.
    BATTLE(false),
    /// A planar card (Planechase).
    PLANAR(false),
    /// A scheme card (Archenemy).
    SCHEME(false),
    /// A vanguard card.
    VANGUARD(false),
    /// A token card.
    TOKEN(false),
    /// A double-faced token.
    DOUBLE_FACED_TOKEN(true),
    /// An emblem card.
    EMBLEM(false),
    /// An augment card (Unstable).
    AUGMENT(false),
    /// A host card (Unstable).
    HOST(false),
    /// An art series card (double-faced).
    ART_SERIES(true),
    /// A reversible card (double-faced).
    REVERSIBLE_CARD(true),
    /// A Case enchantment card.
    CASE(false);

    private final boolean doubleFaced;

    CardLayout(boolean doubleFaced) {
        this.doubleFaced = doubleFaced;
    }

    /// Returns true if this layout is double-faced.
    ///
    /// Double-faced layouts have two physically printed faces: [#TRANSFORM],
    /// [#MODAL_DFC], [#DOUBLE_FACED_TOKEN], [#ART_SERIES], and [#REVERSIBLE_CARD].
    ///
    /// @return true if double-faced
    public boolean isDoubleFaced() {
        return doubleFaced;
    }
}
