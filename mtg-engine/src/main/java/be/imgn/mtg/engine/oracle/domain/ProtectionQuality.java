package be.imgn.mtg.engine.oracle.domain;

/// Quality that a `protection from [quality]` or `hexproof from [quality]` keyword can refer to (rule 702.16a /
// 702.11d). The quality is

/// typically a color, a card type, a subtype, a player, or one of the named
/// variants (`everything`, `all colors`, `monocolored`, …).
public sealed interface ProtectionQuality {

    record OfColor(Color color) implements ProtectionQuality {}

    record OfCardType(CardType type) implements ProtectionQuality {}

    /// A specific [Subtype] (e.g., [CreatureType#DEMON],
    /// [CreatureType#GOBLIN]). The typed enum constant lets
    /// downstream code pattern-match across subtype families.
    record OfSubtype(Subtype subtype) implements ProtectionQuality {}

    /// "non-\[subtype\] \[cardtype\]" — the negated-subtype-within-cardtype
    /// quality (Spare from Evil: "protection from non-Human
    /// creatures"). The card type narrows the universe of objects;
    /// anything in that type whose subtypes include `subtype` is
    /// excluded.
    record OfNonSubtypeOfCardType(Subtype subtype, CardType cardType) implements ProtectionQuality {}

    record OfPlayer(PlayerRef player) implements ProtectionQuality {}

    /// A specific card by name — e.g., `protection from Bolas`. Oracle text
    /// sometimes names an Aura or legendary permanent here.
    record Named(String cardName) implements ProtectionQuality {}

    /// "the colors of \[subject\]" — a dynamic protection quality that
    /// resolves to the colors of the named subject (Empty-Shrine
    /// Kannushi: "protection from the colors of permanents you
    /// control.").
    record ColorsOf(Subject scope) implements ProtectionQuality {}

    /// "the color of \[chooser\]'s choice" — player-chosen color resolved
    /// when the effect creates the protection (Stave Off: "protection
    /// from the color of your choice"). `chooser` names the player
    /// making the choice.
    record ChosenColor(PlayerRef chooser) implements ProtectionQuality {}

    /// Named variants from rule 702.16j–k and 702.16 examples.
    enum Special implements ProtectionQuality {
        EVERYTHING,
        EACH_COLOR,
        MONOCOLORED,
        MULTICOLORED,
        COLORLESS,
        /// "protection from its colors" — each of the object's own colors
        /// (e.g., Earnest Fellowship).
        ITS_COLORS,
        /// "protection from the chosen color" — back-reference to a
        /// color named by a preceding [Effect.ChooseColor] effect
        /// (Prismatic Boon: "Choose a color. X target creatures gain
        /// protection from the chosen color …"). Distinct from
        /// [ChosenColor] which embeds the chooser inline.
        CHOSEN_COLOR,
        /// "protection from the chosen player" — back-reference to
        /// a player named by a preceding [Effect.Choose] over a
        /// player target (True-Name Nemesis: "As this creature
        /// enters, choose a player. This creature has protection
        /// from the chosen player.").
        CHOSEN_PLAYER
    }

    /// "protection from mana value N \[or greater | or less\]?" — rule
    /// 702.16 mana-value variant (e.g., Mistmeadow Skulk). A bare
    /// integer with no comparator is the exact-equals form
    /// ([Comparator#EQUAL_TO]).
    record ManaValue(int value, Comparator comparator) implements ProtectionQuality {
        public ManaValue(int value) {
            this(value, Comparator.EQUAL_TO);
        }

        public ManaValue withComparator(Comparator comparator) {
            return new ManaValue(value, comparator);
        }

        public enum Comparator {
            OR_GREATER,
            OR_LESS,
            EQUAL_TO
        }
    }
}
