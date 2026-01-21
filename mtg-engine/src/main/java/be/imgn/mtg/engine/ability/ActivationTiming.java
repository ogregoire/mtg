package be.imgn.mtg.engine.ability;

/// Timing restrictions for when an ability can be activated.
///
/// Different abilities have different timing restrictions:
/// - Mana abilities can be activated during mana payment
/// - Most abilities require the player to have priority
/// - Some abilities can only be activated at certain times
///
/// @see ActivatedAbility
public enum ActivationTiming {

    /// Can be activated any time the player has priority (instant speed).
    INSTANT,

    /// Can only be activated when the player could cast a sorcery:
    /// during their main phase with an empty stack and priority.
    SORCERY,

    /// Can only be activated during combat (e.g., certain ninjutsu abilities).
    COMBAT,

    /// Can only be activated during the player's turn.
    YOUR_TURN,

    /// Mana ability - can be activated during mana payment without priority.
    MANA_ABILITY
}
