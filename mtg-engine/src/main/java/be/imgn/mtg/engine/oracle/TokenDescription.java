package be.imgn.mtg.engine.oracle;

import java.util.List;

/// Description of a token to be created.
public sealed interface TokenDescription {
    record Predefined(String name) implements TokenDescription {}

    record Custom(
            PtValue pt,
            List<Color> colors,
            List<Supertype> supertypes,
            List<CardType> types,
            List<String> subtypes,
            List<String> abilities)
            implements TokenDescription {
        Custom(PtValue pt, List<Color> colors, List<CardType> types) {
            this(pt, colors, List.of(), types, List.of(), List.of());
        }
    }

    /// Creates a {@link Predefined} token description.
    static TokenDescription predefined(String name) {
        return new Predefined(name);
    }

    /// Creates a {@link Custom} token description.
    static TokenDescription custom(
            PtValue pt,
            List<Color> colors,
            List<Supertype> supertypes,
            List<CardType> types,
            List<String> subtypes,
            List<String> abilities) {
        return new Custom(pt, colors, supertypes, types, subtypes, abilities);
    }
}
