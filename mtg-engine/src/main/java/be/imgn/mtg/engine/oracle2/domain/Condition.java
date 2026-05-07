package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// A predicate the engine can check at trigger-condition time
/// ({@mtg.rule 603.4}, intervening-if) or in the body of a
/// conditional effect.
///
/// One concrete arm today — [HasLife], the `<selector> [has|have] N
/// life` predicate (Felidar Sovereign, Test of Endurance).
/// Additional shapes (`PlayerControls`, `HasManaValue`, …) land as
/// new permits when the parser needs them.
public sealed interface Condition permits Condition.HasLife {

    /// `<who> [has|have] <matcher> life` — the named player's life
    /// total satisfies [#matcher]. Felidar Sovereign uses
    /// `HasLife(<you>, AtLeast(Exact(40)))`; Test of Endurance uses
    /// `HasLife(<you>, AtLeast(Exact(50)))`.
    record HasLife(Selector who, AmountMatcher matcher) implements Condition {
        public HasLife {
            requireNonNull(who);
            requireNonNull(matcher);
        }
    }
}
