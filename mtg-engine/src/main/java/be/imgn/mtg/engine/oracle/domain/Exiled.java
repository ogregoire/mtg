package be.imgn.mtg.engine.oracle.domain;

/// What a [Effect.Exile] effect exiles. Either specific card-level
/// selections (`"exile target creature"`, `"exile all creatures"`) or the full
/// contents of one or more zones (`"exile all graveyards"`).
public sealed interface Exiled {

    /// Exile the objects (cards, permanents, spells on the stack, …) matching
    /// a subject / selector.
    record Objects(Subject subject) implements Exiled {}

    /// Exile every card in the given zone for every player (e.g., all
    /// graveyards). The zone is the singular [ZoneName] — the parser
    /// recognizes plural forms like "graveyards" / "hands" / "libraries".
    record Zones(ZoneName zone) implements Exiled {}

    /// Exile every card in one specific player's zone (Tormod's Crypt:
    /// "Exile target player's graveyard.").
    record PlayerZone(Subject.PlayerRef player, ZoneName zone) implements Exiled {}
}
