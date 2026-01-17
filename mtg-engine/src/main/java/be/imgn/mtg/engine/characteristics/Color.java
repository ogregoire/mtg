package be.imgn.mtg.engine.characteristics;

/// The five colors of mana in Magic ({@mtg.rule 105}).
///
/// The colors are white, blue, black, red, and green. Objects can be one or more of these colors,
/// or colorless. Colorless is not a color.
public enum Color {
    /// White mana, associated with plains.
    WHITE,
    /// Blue mana, associated with islands.
    BLUE,
    /// Black mana, associated with swamps.
    BLACK,
    /// Red mana, associated with mountains.
    RED,
    /// Green mana, associated with forests.
    GREEN
}
