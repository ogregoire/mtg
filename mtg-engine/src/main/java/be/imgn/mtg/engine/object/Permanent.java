package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.internal.DefaultPermanent;

/// A permanent on the battlefield ({@mtg.rule 110}).
///
/// A permanent is a card or token on the battlefield. Permanents remain on the battlefield
/// indefinitely until moved to another zone by a game rule or effect. The permanent types
/// are artifact, battle, creature, enchantment, land, and planeswalker.
///
/// Permanents have statuses that affect how they function ({@mtg.rule 110.5}):
/// - **Tapped/Untapped** ({@mtg.rule 110.5}): Tapped permanents cannot tap again
/// - **Flipped/Unflipped** ({@mtg.rule 110.5}): For flip cards with alternate characteristics
/// - **Face up/Face down** ({@mtg.rule 110.5}): Face-down permanents have no characteristics
/// - **Phased in/Phased out** ({@mtg.rule 702.26}): Phased-out permanents are treated as if they don't exist
///
/// @see Card
/// @see Token
/// @see PermanentSource
public non-sealed interface Permanent extends TypedObject {

    /// Returns the source that created this permanent.
    ///
    /// @return the [Card] or [Token] that became this permanent
    PermanentSource source();

    /// Returns the counters on this permanent.
    ///
    /// @return the counters, never null (may be empty)
    Counters counters();

    // --- Tap/Untap status ---

    /// Returns true if this permanent is tapped.
    ///
    /// @return true if tapped
    boolean isTapped();

    /// Returns true if this permanent is untapped.
    ///
    /// @return true if untapped
    boolean isUntapped();

    /// Returns true if this permanent can tap.
    ///
    /// A permanent cannot tap if it is already tapped or has summoning sickness
    /// (for creatures without haste).
    ///
    /// @return true if can tap
    boolean canTap();

    /// Returns true if this permanent can untap.
    ///
    /// @return true if can untap
    boolean canUntap();

    /// Taps this permanent.
    void tap();

    /// Untaps this permanent.
    void untap();

    // --- Flip status ---

    /// Returns true if this permanent is flipped.
    ///
    /// @return true if flipped
    boolean isFlipped();

    /// Returns true if this permanent is unflipped.
    ///
    /// @return true if unflipped
    boolean isUnflipped();

    /// Returns true if this permanent can flip.
    ///
    /// @return true if can flip
    boolean canFlip();

    /// Returns true if this permanent can unflip.
    ///
    /// @return true if can unflip
    boolean canUnflip();

    /// Flips this permanent.
    void flip();

    /// Unflips this permanent.
    void unflip();

    // --- Face up/down status ---

    /// Returns true if this permanent is face up.
    ///
    /// @return true if face up
    boolean isFaceUp();

    /// Returns true if this permanent is face down.
    ///
    /// @return true if face down
    boolean isFaceDown();

    /// Returns true if this permanent can turn face up.
    ///
    /// @return true if can turn face up
    boolean canTurnFaceUp();

    /// Returns true if this permanent can turn face down.
    ///
    /// @return true if can turn face down
    boolean canTurnFaceDown();

    /// Turns this permanent face up.
    void turnFaceUp();

    /// Turns this permanent face down.
    void turnFaceDown();

    // --- Phasing status ---

    /// Returns true if this permanent is phased in.
    ///
    /// Phased-in permanents function normally.
    ///
    /// @return true if phased in
    boolean isPhasedIn();

    /// Returns true if this permanent is phased out.
    ///
    /// Phased-out permanents are treated as though they don't exist.
    ///
    /// @return true if phased out
    boolean isPhasedOut();

    /// Returns true if this permanent can phase in.
    ///
    /// @return true if can phase in
    boolean canPhaseIn();

    /// Returns true if this permanent can phase out.
    ///
    /// @return true if can phase out
    boolean canPhaseOut();

    /// Phases this permanent in.
    void phaseIn();

    /// Phases this permanent out.
    void phaseOut();

    // --- Factory methods ---

    /// Creates a permanent builder from a card entering the battlefield.
    ///
    /// @param card the card entering the battlefield
    /// @param controller the player who will control the permanent
    /// @return a new builder initialized from the card
    static Builder fromCard(Card card, Player controller) {
        return DefaultPermanent.fromCard(card, controller);
    }

    /// Creates a permanent builder from a token entering the battlefield.
    ///
    /// @param token the token entering the battlefield
    /// @param controller the player who will control the permanent
    /// @return a new builder initialized from the token
    static Builder fromToken(Token token, Player controller) {
        return DefaultPermanent.fromToken(token, controller);
    }

    /// Builder for [Permanent].
    non-sealed interface Builder extends TypedObject.Builder<Permanent, Builder> {}
}
