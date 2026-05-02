package be.imgn.mtg.engine.oracle.domain2.selector;

/// Marks `selector` as a target of a spell or ability — the "target
/// X" form (CR 115.1). Targets are a relationship between the spell
/// or ability on the stack and what it will affect; they aren't a
/// designation or characteristic of the chosen object.
public record Target(Selector selector) implements Selector {}
