package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Types that can be negated in oracle text (e.g., "nonland", "nonblack").
public enum NegationType {
    /// Not a land (nonland).
    LAND,
    /// Not white (nonwhite).
    WHITE,
    /// Not blue (nonblue).
    BLUE,
    /// Not black (nonblack).
    BLACK,
    /// Not red (nonred).
    RED,
    /// Not green (nongreen).
    GREEN,
    /// Not a creature (noncreature).
    CREATURE,
    /// Not an artifact (nonartifact).
    ARTIFACT,
    /// Not an enchantment (nonenchantment).
    ENCHANTMENT,
    /// Not a token (nontoken).
    TOKEN
}
