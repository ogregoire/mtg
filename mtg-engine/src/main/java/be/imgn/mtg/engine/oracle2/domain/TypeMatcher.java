package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;

/// Boolean predicate over an object's static characteristics. Unifies
/// the type axes — card type ([CardType]), subtype ([Subtype]),
/// supertype ([Supertype]) — with colour and mana-value predicates,
/// plus the boolean combinators ([Not], [AllOf], [AnyOf]) into one
/// matcher tree.
///
/// Used wherever a typed predicate slot needs to express "X is a
/// creature spell", "X is a colourless Eldrazi", "X has mana value 4
/// or greater", etc. — without the [be.imgn.mtg.engine.oracle2.domain.selector.Selector]
/// hierarchy's quantifier / zone / target wrappers, which are
/// semantically wrong for a pure type-predicate slot.
///
/// Mirrors oracle's `oracle.domain.TypeMatcher`. The `HasManaValue`
/// arm is an extension over oracle's pure-type design — mana value
/// composes naturally with the type axes here ("spells with mana
/// value 4 or greater" → `AllOf([<spell-axis>, HasManaValue(AtLeast(4))])`).
public sealed interface TypeMatcher {

    /// "is a \<card-type\>" — e.g., "creature", "artifact", "land".
    record IsCardType(CardType type) implements TypeMatcher {
        public IsCardType {
            requireNonNull(type);
        }
    }

    /// "is a \<subtype\>" — e.g., "Eldrazi", "Goblin", "Chandra"
    /// (planeswalker subtype), "Elemental".
    record IsSubtype(Subtype subtype) implements TypeMatcher {
        public IsSubtype {
            requireNonNull(subtype);
        }
    }

    /// "is \<supertype\>" — e.g., "legendary", "snow", "basic".
    record IsSupertype(Supertype supertype) implements TypeMatcher {
        public IsSupertype {
            requireNonNull(supertype);
        }
    }

    /// "is \<colour\>" — e.g., "red", "white", "blue". Single-colour
    /// match; "colourless" is the separate [#COLORLESS] marker because
    /// the [Color] enum doesn't include it (rule 105.2c — colourless
    /// is the absence of colour, not a sixth colour).
    record IsColor(Color color) implements TypeMatcher {
        public IsColor {
            requireNonNull(color);
        }
    }

    /// "has mana value \<matcher\>" — e.g., "with mana value 4 or
    /// greater" (Sage of the Unknowable). The slot is an
    /// [AmountMatcher] so the comparator round-trips structurally.
    record HasManaValue(AmountMatcher value) implements TypeMatcher {
        public HasManaValue {
            requireNonNull(value);
        }
    }

    /// "isn't \<matcher\>" — negation. Underlies "non-creature",
    /// "non-Goblin", "non-snow", "non-red".
    record Not(TypeMatcher matcher) implements TypeMatcher {
        public Not {
            requireNonNull(matcher);
        }
    }

    /// Conjunction — every contained matcher matches. "Colourless
    /// Eldrazi" → `AllOf([COLORLESS, IsSubtype(ELDRAZI)])`. Carries
    /// ≥2 elements; singletons stay unwrapped at the call site.
    record AllOf(List<TypeMatcher> matchers) implements TypeMatcher {
        public AllOf {
            matchers = List.copyOf(matchers);
            if (matchers.size() < 2) {
                throw new IllegalArgumentException(
                        "TypeMatcher.AllOf needs at least 2 elements, got " + matchers.size());
            }
        }
    }

    /// Disjunction — at least one contained matcher matches. "An
    /// Elemental spell or a Chandra planeswalker spell" composes two
    /// `AllOf` shapes under this arm. Carries ≥2 elements.
    record AnyOf(List<TypeMatcher> matchers) implements TypeMatcher {
        public AnyOf {
            matchers = List.copyOf(matchers);
            if (matchers.size() < 2) {
                throw new IllegalArgumentException(
                        "TypeMatcher.AnyOf needs at least 2 elements, got " + matchers.size());
            }
        }
    }

    /// Singleton markers.
    /// - [#COLORLESS]: "colourless" — the [Color] enum is the five
    ///   colours only.
    /// - [#ANYTHING]: AND-identity. Matches every object; emitted by
    ///   parsers as the default when a predicate slot is grammatically
    ///   absent (bare "spells" with no type qualifier).
    enum Standard implements TypeMatcher {
        COLORLESS,
        ANYTHING
    }
}
