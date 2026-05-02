package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

import be.imgn.mtg.engine.oracle.domain2.CombatStatus;

/// Selects an object by its [CombatStatus] (CR 506–509) — "attacking
/// creature", "blocked creature", "unblocked attacking creature",
/// "battle being attacked", etc. Combat status isn't a characteristic
/// (CR 109.3) and isn't permanent status (CR 110.5).
public record CombatStatusSelector(CombatStatus status) implements ObjectPropertySelector {
    public CombatStatusSelector {
        Objects.requireNonNull(status);
    }
}
