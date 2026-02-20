package be.imgn.mtg.engine.selector;

/// Status conditions that permanents can have.
public enum StatusType {
    /// The permanent is tapped.
    TAPPED,
    /// The permanent is untapped.
    UNTAPPED,
    /// The creature is attacking.
    ATTACKING,
    /// The creature is blocking.
    BLOCKING,
    /// The permanent has an equipment attached.
    EQUIPPED,
    /// The permanent has an aura attached.
    ENCHANTED
}
