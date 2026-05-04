package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Selects an object by its name ({@mtg.rule 201}). Four arms cover
/// the oracle-text shapes that actually appear in tournament-legal
/// cards:
///
/// - [Is] — "named X" / "with that name". Literal name match against
///   a string.
/// - [SharesNameWith] — "with the same name as that creature" /
///   "with the same name as another permanent you control". Relational
///   form pointing at a contextually-bound object (~119 cards: Bile
///   Blight, Deputy of Detention, Ripple, …).
/// - [Chosen] — "with the chosen name". Back-reference to a name
///   stored on the permanent by an enters-with-choice effect (~46
///   cards: Pithing Needle, Runed Halo, Declaration of Naught, …).
/// - [HasNoName] — `name` is empty. Face-down creatures
///   ({@mtg.rule 707.2}) have no name, and a few cards filter on that
///   directly.
///
/// Negation ("not named X") goes through `ObjectPropertySelector.Not(...)`.
public sealed interface NameSelector extends CharacteristicSelector
        permits NameSelector.Is, NameSelector.SharesNameWith, NameSelector.Chosen, NameSelector.HasNoName {

    /// "named X" — literal name match. Example: "destroy target
    /// creature named Squee" → `new Is("Squee")`.
    record Is(String name) implements NameSelector {
        public Is {
            requireNonNull(name);
        }
    }

    /// "with the same name as X" — at least one name in common with
    /// the referenced object. The referent is typically a contextually-
    /// bound object ("that creature", "this spell"); pass the
    /// appropriate [ObjectSelector] for the binding site.
    record SharesNameWith(ObjectSelector with) implements NameSelector {
        public SharesNameWith {
            requireNonNull(with);
        }
    }

    /// "with the chosen name" — back-reference to a name chosen by a
    /// preceding `As ~ enters, choose a card name` effect. Resolved
    /// at game time from the matching binding slot on the
    /// referencing permanent.
    ///
    /// `slot` is the literal noun phrase from the oracle text
    /// ("name"). The runtime uses it to look up the matching binding
    /// produced by the corresponding `Choose` effect.
    record Chosen(String slot) implements NameSelector {
        public Chosen {
            requireNonNull(slot);
        }
    }

    /// Object has no name. Face-down creatures ({@mtg.rule 707.2})
    /// are nameless until turned face up (or until an effect gives
    /// them a name).
    record HasNoName() implements NameSelector {}
}
