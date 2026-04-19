package be.imgn.mtg.engine.oracle;

/// What a {@link Effect.Discard} effect discards: either a number of cards
/// (`"discard two cards"`) or the entire hand (`"discard your hand"`).
public sealed interface Discarded {

    /// Discard a specific number of cards. {@code atRandom} is true when the
    /// oracle text specifies "at random".
    record Cards(Amount amount, boolean atRandom) implements Discarded {
        public Cards(Amount amount) {
            this(amount, false);
        }
    }

    /// Discard the player's whole hand.
    enum Hand implements Discarded {
        HAND
    }

    /// Discard every card matching a selector (e.g., Trapfinder's Trick:
    /// "discards all Trap cards").
    record Matching(Selector selector) implements Discarded {}
}
