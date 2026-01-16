package be.imgn.mtg.engine.object;

/// A source that can become a permanent on the battlefield.
public sealed interface PermanentSource permits Card, Token {}
