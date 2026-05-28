package be.imgn.mtg.engine.oracle2.domain.mana;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.TypeMatcher;

/// A spend restriction on a [Mana] payload ({@mtg.rule 106.6}). Every
/// arm is typed — there is no free-text fallback. New oracle shapes
/// land as new permitted records.
///
/// Four arms today:
///
/// - [ToCast] — "to cast \<spell\>". The spell argument is a
///   [TypeMatcher] (no quantifier / zone wrapping); the implicit
///   "spell" noun is the slot's semantics, not the AST.
/// - [ToActivateAbility] — "to activate abilities [of \<source\>]".
///   Optional source narrows the restriction to abilities of objects
///   matching the predicate; `null` for the bare form.
/// - [OnCostsContaining] — "on costs that contain \<symbol\>". Keyed
///   off a single mana symbol.
/// - [OneOf] — composes two or more verb-clause restrictions joined
///   by `or` ("to cast an artifact spell or activate an ability").
public sealed interface Restriction {

    /// "to cast \<spell\>" (Eldrazi Temple, Sage of the Unknowable,
    /// Automated Artificer, Cultivator Drone).
    record ToCast(TypeMatcher spell) implements Restriction {
        public ToCast {
            requireNonNull(spell);
        }
    }

    /// "to activate abilities" (Omen Hawker; `source = null`) or "to
    /// activate abilities of \<source\>" (Cultivator Drone, Eldrazi
    /// Temple, Oaken Siren).
    record ToActivateAbility(@Nullable TypeMatcher source) implements Restriction {}

    /// "on costs that contain \<symbol\>" (typically `{X}`).
    record OnCostsContaining(ManaSymbol symbol) implements Restriction {
        public OnCostsContaining {
            requireNonNull(symbol);
        }
    }

    /// Composes two or more verb-clause restrictions joined by `or`.
    /// Carries ≥2 elements; singleton composition collapses to the
    /// bare arm at parse time.
    record OneOf(List<Restriction> alternatives) implements Restriction {
        public OneOf {
            alternatives = List.copyOf(alternatives);
            if (alternatives.size() < 2) {
                throw new IllegalArgumentException(
                        "Restriction.OneOf needs at least 2 elements, got " + alternatives.size());
            }
        }
    }
}
