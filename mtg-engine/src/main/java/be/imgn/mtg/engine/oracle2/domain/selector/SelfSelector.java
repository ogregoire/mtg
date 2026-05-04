package be.imgn.mtg.engine.oracle2.domain.selector;

/// The card's self-reference — the `~` token in oracle text. Used
/// in phrases like "for each other attacking ~" (Aurochs) where a
/// card refers to itself in a type-slot position.
public enum SelfSelector implements ObjectSelector {
    SELF
}
