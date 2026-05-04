package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Picks players other than `than`. Covers oracle phrasings like
/// "another player" (≡ `Other(PlayerRelationSelector(YOU))`) and
/// the inter-sentence reference in "target player discards a card.
/// Other players draw a card.", where `than` resolves to the
/// previously selected target player. Relational rather than enum-
/// valued because "other" only has meaning relative to a specific
/// reference player.
public record OtherPlayerSelector(PlayerSelector than) implements PlayerSelector {
    public OtherPlayerSelector {
        requireNonNull(than);
    }
}
