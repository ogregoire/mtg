package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// Describes the kind of land that a landwalk ability (rule 702.14) refers to.
///
/// Rule 702.14a: the keyword appears as "\[type\]walk", where `[type]` is a basic
/// land type (e.g. `forestwalk`), or the card type `land` optionally combined
/// with supertypes (`legendary landwalk`, `snow landwalk`), the `nonbasic`
/// negation, or another card type (`artifact landwalk`). A basic land subtype
/// may also be qualified (`snow swampwalk`).
///
/// Fields are nullable / defaulting so the selector captures only the
/// qualifiers actually present in the keyword.
public record LandSelector(
        @Nullable Supertype supertype,
        boolean nonbasic,
        @Nullable CardType cardType,
        @Nullable LandType subtype) {

    /// A plain basic-land-type walk, e.g., `forestwalk`.
    public static LandSelector ofSubtype(LandType subtype) {
        return new LandSelector(null, false, null, subtype);
    }

    /// Generic `landwalk` with no qualifier (walks any land).
    public static LandSelector land() {
        return new LandSelector(null, false, null, null);
    }
}
