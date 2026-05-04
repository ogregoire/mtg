package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.ObjectStatus;

/// Selects a permanent by its status ({@mtg.rule 110.5}) —
/// tapped/untapped, flipped/unflipped, face up/face down, phased
/// in/phased out. Status isn't a characteristic ({@mtg.rule 110.5a}).
///
/// Single arm wrapping the [ObjectStatus] enum; the parser maps each
/// oracle adjective ("tapped", "face-down", "phased out", …) to the
/// corresponding enum value.
public sealed interface StatusSelector extends ObjectPropertySelector permits StatusSelector.HasStatus {

    /// "[status] permanent" / "[status] creature" — predicate that
    /// the object has the named status.
    record HasStatus(ObjectStatus status) implements StatusSelector {
        public HasStatus {
            requireNonNull(status);
        }
    }
}
