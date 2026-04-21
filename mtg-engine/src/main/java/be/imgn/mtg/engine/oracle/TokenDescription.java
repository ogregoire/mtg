package be.imgn.mtg.engine.oracle;

import java.util.List;

/// Description of a token to be created.
public sealed interface TokenDescription {
    record Predefined(PredefinedToken name) implements TokenDescription {}

    record Custom(
            PtValue pt,
            List<Color> colors,
            List<Supertype> supertypes,
            List<CardType> types,
            List<Subtype> subtypes,
            List<Ability> abilities)
            implements TokenDescription {
        Custom(PtValue pt, List<Color> colors, List<CardType> types) {
            this(pt, colors, List.of(), types, List.of(), List.of());
        }

        public Custom withAbilities(List<Ability> abilities) {
            return new Custom(pt, colors, supertypes, types, subtypes, abilities);
        }
    }

    /// "a token that's a copy of [source]" — the token takes its
    /// characteristics from another permanent (e.g., Myr Propagator:
    /// "Create a token that's a copy of this creature.").
    record CopyOf(Subject source) implements TokenDescription {}

    /// Creates a [Predefined] token description.
    static TokenDescription predefined(PredefinedToken name) {
        return new Predefined(name);
    }

    /// Creates a [Custom] token description.
    static TokenDescription custom(
            PtValue pt,
            List<Color> colors,
            List<Supertype> supertypes,
            List<CardType> types,
            List<Subtype> subtypes,
            List<Ability> abilities) {
        return new Custom(pt, colors, supertypes, types, subtypes, abilities);
    }
}
