package be.imgn.mtg.engine.oracle;

/// Quality that a {@code protection from [quality]} or {@code hexproof from
/// [quality]} keyword can refer to (rule 702.16a / 702.11d). The quality is
/// typically a color, a card type, a subtype, a player, or one of the named
/// variants (`everything`, `all colors`, `monocolored`, …).
public sealed interface ProtectionQuality {

    record OfColor(Color color) implements ProtectionQuality {}

    record OfCardType(CardType type) implements ProtectionQuality {}

    /// A subtype name (e.g., `Demons`, `Goblins`, `Auras`). Uses a String
    /// because subtype names span multiple disjoint enums (CreatureType,
    /// LandType, ArtifactType, …).
    record OfSubtype(String name) implements ProtectionQuality {}

    record OfPlayer(Subject.PlayerRef player) implements ProtectionQuality {}

    /// A specific card by name — e.g., `protection from Bolas`. Oracle text
    /// sometimes names an Aura or legendary permanent here.
    record Named(String cardName) implements ProtectionQuality {}

    /// Named variants from rule 702.16j–k and 702.16 examples.
    enum Special implements ProtectionQuality {
        EVERYTHING,
        EACH_COLOR,
        MONOCOLORED,
        MULTICOLORED,
        COLORLESS,
        /// "protection from its colors" — each of the object's own colors
        /// (e.g., Earnest Fellowship).
        ITS_COLORS
    }

    /// "protection from mana value N or greater / less / equal to" — rule
    /// 702.16 mana-value variant (e.g., Mistmeadow Skulk).
    record ManaValue(int value, Comparator comparator) implements ProtectionQuality {
        public enum Comparator {
            OR_GREATER,
            OR_LESS,
            EQUAL_TO
        }
    }
}
