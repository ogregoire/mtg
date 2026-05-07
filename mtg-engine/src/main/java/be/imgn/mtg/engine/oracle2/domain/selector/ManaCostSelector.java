package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;

/// Selects an object by its mana cost ({@mtg.rule 202}). Three arms:
///
/// - [HasManaValue] — "with mana value [N | N or greater | N or less |
///   exactly N | …]". The dominant comparison form (Abrupt Decay,
///   Up the Beanstalk, Angry Rabble, As Foretold, hundreds of cards).
///   The bound is an [AmountMatcher].
/// - [Standard#CHOSEN] — "with mana value of the chosen quality".
///   Back-reference to a preceding `Choose [odd|even]` effect that
///   bound a parity at game time. Used by Ashling's Prerogative,
///   Extinction Event, Mutinous Massacre, Lavabrink Venturer.
/// - [SharesManaValueWith] — "with the same mana value as X". Used
///   by Sanguine Praetor and similar relational-equality cards.
public sealed interface ManaCostSelector extends CharacteristicSelector
        permits ManaCostSelector.HasManaValue, ManaCostSelector.Standard, ManaCostSelector.SharesManaValueWith {

    /// "with mana value [matcher]" — the bound is an [AmountMatcher]
    /// (`AtLeast`, `AtMost`, `Exactly`, `InRange`). Examples:
    /// - Abrupt Decay: "nonland permanent with mana value 3 or less"
    ///   → `HasManaValue(AtMost(Exact(3)))`.
    /// - Angry Rabble: "spell with mana value 4 or greater" →
    ///   `HasManaValue(AtLeast(Exact(4)))`.
    /// - Abiding Grace: "creature card with mana value 1" →
    ///   `HasManaValue(Exactly(Exact(1)))`.
    /// - As Foretold: "spell you cast with mana value X or less" →
    ///   `HasManaValue(AtMost(Standard.X))`.
    record HasManaValue(AmountMatcher matcher) implements ManaCostSelector {
        public HasManaValue {
            requireNonNull(matcher);
        }
    }

    /// Stateless predicate arms — see CLAUDE.md (`Default umbrella
    /// name: Standard`). Currently a single value; future no-payload
    /// mana-cost predicates land here without a new enum.
    enum Standard implements ManaCostSelector {
        /// "with mana value of the chosen quality" — back-reference
        /// to a preceding `Choose [odd|even]` effect (Ashling's
        /// Prerogative, Extinction Event, Mutinous Massacre,
        /// Lavabrink Venturer). The runtime resolves the bound parity
        /// from the matching `Choose` effect.
        CHOSEN
    }

    /// "with the same mana value as X" — at least equal mana value
    /// to the referenced object. Examples: Sanguine Praetor ("each
    /// creature with the same mana value as the sacrificed creature").
    record SharesManaValueWith(ObjectSelector with) implements ManaCostSelector {
        public SharesManaValueWith {
            requireNonNull(with);
        }
    }
}
