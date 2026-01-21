package be.imgn.mtg.engine.action;

/// Reasons why a player action is illegal.
///
/// When an action fails validation, the specific type of illegality
/// helps the UI provide appropriate feedback to the player.
public enum IllegalActionType {

    /// The player doesn't have priority.
    NO_PRIORITY,

    /// The action cannot be taken at this time (timing restriction).
    ///
    /// For example, casting a sorcery when it's not the player's main phase
    /// or when the stack is not empty.
    WRONG_TIMING,

    /// One or more targets are illegal.
    ILLEGAL_TARGET,

    /// The player cannot pay the required costs.
    CANNOT_PAY_COST,

    /// A restriction effect prevents this action.
    ///
    /// For example, "You can't cast creature spells" or
    /// "Activated abilities can't be activated."
    RESTRICTION_VIOLATED,

    /// The player has already played their maximum lands this turn.
    LAND_ALREADY_PLAYED,

    /// The card or object is not in the expected zone.
    NOT_IN_ZONE,

    /// A spell with split second is on the stack.
    ///
    /// While a spell with split second is on the stack, players can't
    /// cast spells or activate abilities that aren't mana abilities.
    SPLIT_SECOND
}
