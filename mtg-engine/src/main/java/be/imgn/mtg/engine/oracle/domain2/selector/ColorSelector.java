package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle.domain2.Color;

/// Selects an object by its color ({@mtg.rule 105}, {@mtg.rule 202}).
/// Covers the common oracle-text shapes: positive single-color match
/// ([Is]), the count-based predicates ([Composition]), the
/// chosen-color back-reference ([Chosen]), and the relational
/// shares-a-color form ([SharesAColorWith]).
///
/// Negation goes through `ObjectPropertySelector.Not(...)`. Boolean
/// composition ("blue or green") goes through
/// `ObjectPropertySelector.AnyOf` / `.AllOf`. There is no
/// color-internal `Any`/`All`/`Not` — keeping composition at one
/// level (the property level) avoids a duplicate boolean tree.
public sealed interface ColorSelector extends CharacteristicSelector
        permits ColorSelector.Is, ColorSelector.Composition, ColorSelector.Chosen, ColorSelector.SharesAColorWith {

    /// "[color]" — single positive color match. Example:
    /// "blue creature" → `new Is(Color.BLUE)`.
    record Is(Color color) implements ColorSelector {
        public Is {
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
    /// Parameterless because the referent is resolved at game time
    /// from the most recent ChooseColor in the same resolution
    /// context; there is no static handle to point at.
    ///
    /// Modeled as a record (not an enum singleton) so we can add a
    /// binding field later — e.g. an explicit pointer to the
    /// ChooseColor effect, or a tag for the choice — without breaking
    /// callers.
    record Chosen() implements ColorSelector {}

    /// "shares a color with X" — at least one color in common with
    /// the referenced object (~86 cards). Distinct from "of the same
    /// color as" (which would mean *all* colors match) — oracle text
    /// doesn't actually emit the latter form.
    record SharesAColorWith(ObjectSelector with) implements ColorSelector {
        public SharesAColorWith {
            requireNonNull(with);
        }
    }

    /// "white" — shorthand for `new Is(Color.WHITE)`.
    ColorSelector WHITE = new Is(Color.WHITE);
    /// "blue" — shorthand for `new Is(Color.BLUE)`.
    ColorSelector BLUE = new Is(Color.BLUE);
    /// "black" — shorthand for `new Is(Color.BLACK)`.
    ColorSelector BLACK = new Is(Color.BLACK);
    /// "red" — shorthand for `new Is(Color.RED)`.
    ColorSelector RED = new Is(Color.RED);
    /// "green" — shorthand for `new Is(Color.GREEN)`.
    ColorSelector GREEN = new Is(Color.GREEN);
}
