package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.object.internal.DefaultPermanent;

/// A permanent on the battlefield.
public non-sealed interface Permanent extends GameObject {

    /// Returns the source that created this permanent.
    PermanentSource source();

    /// Returns the counters on this permanent.
    Counters counters();

    /// Returns true if this permanent is tapped.
    boolean isTapped();

    /// Returns true if this permanent is untapped.
    boolean isUntapped();

    /// Returns true if this permanent can tap.
    boolean canTap();

    /// Returns true if this permanent can untap.
    boolean canUntap();

    /// Taps this permanent.
    void tap();

    /// Untaps this permanent.
    void untap();

    /// Returns true if this permanent is flipped.
    boolean isFlipped();

    /// Returns true if this permanent is unflipped.
    boolean isUnflipped();

    /// Returns true if this permanent can flip.
    boolean canFlip();

    /// Returns true if this permanent can unflip.
    boolean canUnflip();

    /// Flips this permanent.
    void flip();

    /// Unflips this permanent.
    void unflip();

    /// Returns true if this permanent is face up.
    boolean isFaceUp();

    /// Returns true if this permanent is face down.
    boolean isFaceDown();

    /// Returns true if this permanent can turn face up.
    boolean canTurnFaceUp();

    /// Returns true if this permanent can turn face down.
    boolean canTurnFaceDown();

    /// Turns this permanent face up.
    void turnFaceUp();

    /// Turns this permanent face down.
    void turnFaceDown();

    /// Returns true if this permanent is phased in.
    boolean isPhasedIn();

    /// Returns true if this permanent is phased out.
    boolean isPhasedOut();

    /// Returns true if this permanent can phase in.
    boolean canPhaseIn();

    /// Returns true if this permanent can phase out.
    boolean canPhaseOut();

    /// Phases this permanent in.
    void phaseIn();

    /// Phases this permanent out.
    void phaseOut();

    /// Creates a permanent builder from a card entering the battlefield.
    static Builder fromCard(Card card, Player controller) {
        return DefaultPermanent.fromCard(card, controller);
    }

    /// Creates a permanent builder from a token entering the battlefield.
    static Builder fromToken(Token token, Player controller) {
        return DefaultPermanent.fromToken(token, controller);
    }

    /// Builder for Permanent.
    non-sealed interface Builder extends GameObject.Builder<Permanent, Builder> {}
}
