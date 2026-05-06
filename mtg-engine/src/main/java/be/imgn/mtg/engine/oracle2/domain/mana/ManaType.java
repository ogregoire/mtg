package be.imgn.mtg.engine.oracle2.domain.mana;

/// The six types of mana ({@mtg.rule 106.1}): the five colors plus
/// colorless. Snow is *not* a mana type ({@mtg.rule 107.4h}); it's a
/// quality of mana produced by a snow source, represented on the symbol
/// side by [ManaSymbol.Snow].
public enum ManaType {
    /// White mana.
    WHITE("{W}"),
    /// Blue mana.
    BLUE("{U}"),
    /// Black mana.
    BLACK("{B}"),
    /// Red mana.
    RED("{R}"),
    /// Green mana.
    GREEN("{G}"),
    /// Colorless mana. Colorless is not a color ({@mtg.rule 105.4}) but it
    /// is a mana type.
    COLORLESS("{C}");

    private final String notation;

    ManaType(String notation) {
        this.notation = notation;
    }

    /// The bracketed notation for this mana type, e.g. `{W}` or `{C}`.
    ///
    /// @return the notation
    public String notation() {
        return notation;
    }
}
