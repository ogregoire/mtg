package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

/// Selects an object filtered by who owns it (CR 108.3) — "creature
/// you own", "card an opponent owns", etc. Owner isn't a
/// characteristic per CR 109.3 and is fixed at game start. Inverse
/// of the player-side [OwnerSelector], which picks the owner given
/// an object.
public record OwnedBySelector(PlayerSelector by) implements ObjectPropertySelector {
    public OwnedBySelector {
        Objects.requireNonNull(by);
    }
}
