package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Description of a token to be created.
public sealed interface TokenDescription {
    record Predefined(PredefinedToken name) implements TokenDescription {}

    /// `name` is the literal card name the token is printed with when
    /// oracle text specifies one (Tooth and Claw: "Create a 3/1 red
    /// Beast creature token named Carnivore."). One of the rare
    /// legitimate `String` fields — a card name by construction.
    record Custom(
            PtValue pt,
            List<Color> colors,
            List<Supertype> supertypes,
            List<CardType> types,
            List<Subtype> subtypes,
            List<Ability> abilities,
            @Nullable String name)
            implements TokenDescription {
        public Custom(
                PtValue pt,
                List<Color> colors,
                List<Supertype> supertypes,
                List<CardType> types,
                List<Subtype> subtypes,
                List<Ability> abilities) {
            this(pt, colors, supertypes, types, subtypes, abilities, null);
        }

        public Custom(PtValue pt, List<Color> colors, List<CardType> types) {
            this(pt, colors, List.of(), types, List.of(), List.of(), null);
        }

        public Custom withAbilities(List<Ability> abilities) {
            return new Custom(pt, colors, supertypes, types, subtypes, abilities, name);
        }

        public Custom withName(String name) {
            return new Custom(pt, colors, supertypes, types, subtypes, abilities, name);
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
