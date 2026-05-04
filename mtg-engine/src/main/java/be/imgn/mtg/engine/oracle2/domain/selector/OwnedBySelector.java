package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Selects an object filtered by who owns it (CR 108.3) — "creature
/// you own", "card an opponent owns", etc. Owner isn't a
/// characteristic per CR 109.3 and is fixed at game start. Inverse
/// of the player-side [OwnerSelector], which picks the owner given
/// an object.
public record OwnedBySelector(PlayerSelector by) implements ObjectPropertySelector {
    public OwnedBySelector {
        requireNonNull(by);
    }
}
