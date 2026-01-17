package be.imgn.mtg.tooling.card.model;

import org.jspecify.annotations.Nullable;

/// Represents a card from the database query.
///
/// @param cardId unique card identifier
/// @param name card name
/// @param layout card layout (normal, transform, modal_dfc, etc.)
/// @param manaValue converted mana cost
/// @param colorIdentity color identity (comma-separated: W,U,B,R,G)
/// @param colors card colors (comma-separated)
/// @param manaCost mana cost string
/// @param oracleText oracle text
/// @param power power (for creatures)
/// @param toughness toughness (for creatures)
/// @param typeLine type line
/// @param loyalty loyalty (for planeswalkers)
/// @param defense defense (for battles)
/// @param face1Name front face name (for DFCs)
/// @param face1ManaCost front face mana cost
/// @param face1OracleText front face oracle text
/// @param face1TypeLine front face type line
/// @param face1Power front face power
/// @param face1Toughness front face toughness
/// @param face1Loyalty front face loyalty
/// @param face2Name back face name (for DFCs)
/// @param face2ManaCost back face mana cost
/// @param face2OracleText back face oracle text
/// @param face2TypeLine back face type line
/// @param face2Power back face power
/// @param face2Toughness back face toughness
/// @param face2Loyalty back face loyalty
public record CardResult(
        long cardId,
        String name,
        @Nullable String layout,
        double manaValue,
        @Nullable String colorIdentity,
        @Nullable String colors,
        @Nullable String manaCost,
        @Nullable String oracleText,
        @Nullable String power,
        @Nullable String toughness,
        @Nullable String typeLine,
        @Nullable String loyalty,
        @Nullable String defense,
        @Nullable String face1Name,
        @Nullable String face1ManaCost,
        @Nullable String face1OracleText,
        @Nullable String face1TypeLine,
        @Nullable String face1Power,
        @Nullable String face1Toughness,
        @Nullable String face1Loyalty,
        @Nullable String face2Name,
        @Nullable String face2ManaCost,
        @Nullable String face2OracleText,
        @Nullable String face2TypeLine,
        @Nullable String face2Power,
        @Nullable String face2Toughness,
        @Nullable String face2Loyalty) {

    /// Returns true if this card has multiple faces (DFC, MDFC, etc.).
    ///
    /// @return true if card has multiple faces
    public boolean isDoubleFaced() {
        return face1Name != null && face2Name != null;
    }
}
