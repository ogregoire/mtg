package be.imgn.mtg.engine.oracle.domain2.selector;

/// Picks the owner of an object (CR 108.3) — "that card's owner",
/// "owner of target permanent". Inverse of the object-side
/// [OwnedBySelector], which picks an object filtered by its owner.
public record OwnerSelector(ObjectSelector of) implements PlayerSelector {}
