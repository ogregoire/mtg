package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

/// Selects an object filtered by who controls it (CR 108.4, 110.2)
/// — "creature you control", "permanent an opponent controls", etc.
/// Controller isn't a characteristic per CR 109.3. Inverse of the
/// player-side [ControllerSelector], which picks the controller
/// given an object.
public record ControlledBySelector(PlayerSelector by) implements ObjectPropertySelector {
    public ControlledBySelector {
        requireNonNull(by);
    }
}
