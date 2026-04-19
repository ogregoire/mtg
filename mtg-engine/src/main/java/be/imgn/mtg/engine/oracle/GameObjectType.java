package be.imgn.mtg.engine.oracle;

/// Game object type references in oracle text (Rule 109.2).
public enum GameObjectType {
    PERMANENT,
    SPELL,
    CARD,
    TOKEN,
    SOURCE,
    ABILITY,
    /// Rule 109.5 — a player, admitted here so object selectors can
    /// uniformly target players ("Enchant player").
    PLAYER
}
