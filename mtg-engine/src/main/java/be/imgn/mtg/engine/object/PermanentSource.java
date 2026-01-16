package be.imgn.mtg.engine.object;

/// A source that can become a [Permanent] on the battlefield.
///
/// This sealed interface represents objects that can enter the battlefield as permanents:
/// - [Card]: A physical or digital card that enters the battlefield
/// - [Token]: A token created by a spell or ability
public sealed interface PermanentSource permits Card, Token {}
