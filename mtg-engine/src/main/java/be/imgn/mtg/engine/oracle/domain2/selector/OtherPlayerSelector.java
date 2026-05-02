package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

/// Picks players other than `than`. Covers oracle phrasings like
/// "another player" (≡ `Other(PlayerRelationSelector(YOU))`) and
/// the inter-sentence reference in "target player discards a card.
/// Other players draw a card.", where `than` resolves to the
/// previously selected target player. Relational rather than enum-
/// valued because "other" only has meaning relative to a specific
/// reference player.
public record OtherPlayerSelector(PlayerSelector than) implements PlayerSelector {
    public OtherPlayerSelector {
        Objects.requireNonNull(than);
    }
}
