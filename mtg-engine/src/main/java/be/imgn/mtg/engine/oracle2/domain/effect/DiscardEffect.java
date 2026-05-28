package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "X discards Y." ({@mtg.rule 701.9}). The `who` slot is the
/// discarding player; the `what` slot is what gets discarded —
/// either the entirety of a player's hand ([What.Hand]) or
/// individual cards picked by the discarding player or at random
/// ([What.Cards]).
public record DiscardEffect(Selector who, What what) implements Effect {
    public DiscardEffect {
        requireNonNull(who);
        requireNonNull(what);
    }

    /// What gets discarded. Split keeps the `atRandom` flag local to
    /// [Cards] — discarding a whole hand has no chooser, so "at
    /// random" doesn't apply.
    public sealed interface What {

        /// "Discard your hand." / "Discard their hand." — the
        /// entirety of the discarding player's hand. The hand-owner
        /// is the discarder; the parent [DiscardEffect#who] is
        /// authoritative, so this record carries no `player` field.
        enum Hand implements What {
            HAND
        }

        /// "Discard a card", "Discard two cards at random" — specific
        /// card(s) picked from a hand. The selector is typically a
        /// quantified [be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector.Hand].
        /// `atRandom` ({@mtg.rule 701.8d}): the game (not the
        /// discarding player) picks the card(s).
        record Cards(Selector selector, boolean atRandom) implements What {
            public Cards {
                requireNonNull(selector);
            }

            public Cards(Selector selector) {
                this(selector, false);
            }

            /// Wither — sets [#atRandom] to `true`. Used by the
            /// parser's `optionallyFollowedBy("at random", …)` chain.
            public Cards withAtRandom() {
                return new Cards(selector, true);
            }
        }
    }
}
