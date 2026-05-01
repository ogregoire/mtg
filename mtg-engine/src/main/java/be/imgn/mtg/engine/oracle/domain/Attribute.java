package be.imgn.mtg.engine.oracle.domain;

/// A card-level attribute that travels with the card across all zones.
///
/// Distinct from a [characteristic][be.imgn.mtg.engine.characteristics] (which
/// describes the in-game object) and from a permanent's status or designation
/// (which lives only on the battlefield). An attribute belongs to the card
/// itself and persists regardless of the zone the card is in.
///
/// {@mtg.rule 109.3} for what is and isn't a characteristic.
public enum Attribute {
    /// Designates a card as its deck's commander.
    ///
    /// {@mtg.rule 903.3}: "This designation is not a characteristic of the
    /// object represented by the card; rather, it is an attribute of the card
    /// itself. The card retains this designation even when it changes zones."
    COMMANDER
}
