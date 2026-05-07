package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Selects an object by its name ({@mtg.rule 201}). Three arms cover
/// the oracle-text shapes that actually appear in tournament-legal
/// cards:
///
/// - [Is] — "named X" / "with that name". Literal name match against
///   a string.
/// - [SharesNameWith] — "with the same name as that creature" /
///   "with the same name as another permanent you control". Relational
///   form pointing at a contextually-bound object (~119 cards).
/// - [Standard] — stateless predicates (chosen-name back-reference,
///   has-no-name).
///
/// Negation ("not named X") goes through `ObjectPropertySelector.Not(...)`.
public sealed interface NameSelector extends CharacteristicSelector
        permits NameSelector.Is, NameSelector.SharesNameWith, NameSelector.Standard {

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

    /// Stateless predicate arms — see CLAUDE.md (`Default umbrella
    /// name: Standard`).
    enum Standard implements NameSelector {
        /// "with the chosen name" — back-reference to a name chosen
        /// by a preceding `As ~ enters, choose a card name` effect
        /// (~62 cards: Pithing Needle, Runed Halo, Declaration of
        /// Naught, …). The runtime resolves the bound name from the
        /// matching `Choose` effect.
        CHOSEN,
        /// Object has no name. Face-down creatures ({@mtg.rule 707.2})
        /// are nameless until turned face up (or until an effect
        /// gives them a name).
        HAS_NO_NAME
    }
}
