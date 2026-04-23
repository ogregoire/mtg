package be.imgn.mtg.engine.oracle.domain;

/// Named comprehensive-rules entries that oracle text can suppress via
/// "The 'X' doesn't apply." wording. Only a handful of cards reference game
/// rules by name in their oracle text, so the enum is narrow.
public enum GameRule {

    /// Rule 704.5j — the "legend rule". Referenced by Mirror Gallery.
    LEGEND_RULE
}
