package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.CombatStatus;

/// Selects an object by its [CombatStatus] (CR 506–509) — "attacking
/// creature", "blocked creature", "unblocked attacking creature",
/// "battle being attacked", etc. Combat status isn't a characteristic
/// (CR 109.3) and isn't permanent status (CR 110.5).
public record CombatStatusSelector(CombatStatus status) implements ObjectPropertySelector {
    public CombatStatusSelector {
        requireNonNull(status);
    }
}
