package be.imgn.mtg.engine.oracle.domain2.selector;

/// Picks the controller of an object (CR 108.4, 110.2) — "the
/// controller of target permanent", "that spell's controller",
/// "controller of the chosen creature". Inverse of the object-side
/// [ControlledBySelector], which picks an object filtered by its
/// controller.
public record ControllerSelector(ObjectSelector of) implements PlayerSelector {}
