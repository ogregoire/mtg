package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// "X attaches to Y" — selects the attaching object X (Aura,
/// Equipment, or Fortification) given a selector for what it's
/// attached to. The bearer `to` may be an object (creature, land, …)
/// or a player (a Curse Aura attached to a player), so its type is
/// the broad [Selector]. Examples: "Aura attached to a creature you
/// control", "Equipment attached to a green creature", "Aura
/// attached to a player".
public record AttachesToSelector(Selector to) implements ObjectPropertySelector {
    public AttachesToSelector {
        requireNonNull(to);
    }
}
