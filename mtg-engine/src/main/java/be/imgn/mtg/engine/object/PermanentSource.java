package be.imgn.mtg.engine.object;

/// An object that can enter the battlefield as a [Permanent] ({@mtg.rule 110}).
///
/// When a permanent spell resolves, the card enters the battlefield. Tokens are created
/// directly on the battlefield. Both cards and tokens can become permanents.
///
/// A permanent retains a reference to its source for tracking purposes such as
/// determining the original characteristics and for effects that reference "this card."
public sealed interface PermanentSource permits Card, Token {}
