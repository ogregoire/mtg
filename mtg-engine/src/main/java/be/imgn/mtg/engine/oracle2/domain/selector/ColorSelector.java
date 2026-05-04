package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Color;

/// Selects an object by its color ({@mtg.rule 105}, {@mtg.rule 202}).
/// Covers the common oracle-text shapes: positive single-color match
/// ([Is]), single-color negation ([IsNot] — "nonblue"), the
/// count-based predicates ([Composition]), the chosen-color
/// back-reference ([Chosen]), and the relational shares-a-color form
/// ([SharesAColorWith]).
///
/// Boolean composition ("blue or green") goes through
/// `ObjectPropertySelector.AnyOf` / `.AllOf`. There is no
/// color-internal `Any`/`All` — keeping composition at one level
/// (the property level) avoids a duplicate boolean tree.
public sealed interface ColorSelector extends CharacteristicSelector
        permits ColorSelector.Is,
                ColorSelector.IsNot,
                ColorSelector.Composition,
                ColorSelector.Chosen,
                ColorSelector.SharesAColorWith {

    /// "[color]" — single positive color match. Example:
    /// "blue creature" → `new Is(Color.BLUE)`.
    record Is(Color color) implements ColorSelector {
        public Is {
            requireNonNull(color);
        }
    }

    /// "non[color]" — single negative color match. Example:
    /// "nonblue creature" →
    /// `AllOf(new CardTypeSelector.Is(CardType.CREATURE), new IsNot(Color.BLUE))`.
    record IsNot(Color color) implements ColorSelector {
        public IsNot {
            requireNonNull(color);
        }
    }

    /// Count-based color predicates. The rules don't give a single
    /// umbrella name ({@mtg.rule 105.2a}, {@mtg.rule 105.2b},
    /// {@mtg.rule 105.2c} describe each individually);
    /// "Composition" reads as "this card's color composition".
    enum Composition implements ColorSelector {
        /// "colorless" — no colors at all ({@mtg.rule 105.2c}).
        COLORLESS,
        /// "monocolored" — exactly one color ({@mtg.rule 105.2a}).
        MONOCOLORED,
        /// "multicolored" — two or more colors ({@mtg.rule 105.2b}).
        MULTICOLORED,
        /// "all colors" — every color (Crackleburr, Niv-Mizzet
        /// Reborn).
        ALL_COLORS
    }

    /// "the chosen color" — back-reference to a preceding
    /// ChooseColor effect (Painter's Servant, "as ~ enters, choose a
    /// color", and the ~97 cards that match this pattern in oracle
    /// text).
    ///
    /// `slot` is the literal noun phrase from the oracle text that
    /// names the choice ("color"). The runtime uses it to look up
    /// the matching binding produced by the corresponding `Choose`
    /// effect.
    record Chosen(String slot) implements ColorSelector {
        public Chosen {
            requireNonNull(slot);
        }
    }

    /// "shares a color with X" — at least one color in common with
    /// the referenced object (~86 cards). Distinct from "of the same
    /// color as" (which would mean *all* colors match) — oracle text
    /// doesn't actually emit the latter form.
    record SharesAColorWith(ObjectSelector with) implements ColorSelector {
        public SharesAColorWith {
            requireNonNull(with);
        }
    }
}
