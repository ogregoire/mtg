package be.imgn.mtg.engine.ability.internal.parser.selector;

/// Predefined token types as defined in rule 111.10.
///
/// Each predefined token has specific characteristics that are automatically applied
/// when the token is created. The parser only needs to recognize the token type;
/// the actual characteristics are defined by the rules.
public enum PredefinedTokenType {
    // Artifact tokens (111.10a-c, 111.10f-h, 111.10s-u)
    /// Colorless Treasure artifact token with "{T}, Sacrifice: Add one mana of any color."
    TREASURE,
    /// Colorless Food artifact token with "{2}, {T}, Sacrifice: You gain 3 life."
    FOOD,
    /// Colorless Gold artifact token with "Sacrifice: Add one mana of any color."
    GOLD,
    /// Colorless Clue artifact token with "{2}, Sacrifice: Draw a card."
    CLUE,
    /// Colorless Blood artifact token with "{1}, {T}, Discard, Sacrifice: Draw a card."
    BLOOD,
    /// Colorless Powerstone artifact token with "{T}: Add {C}. Can't cast nonartifact spells."
    POWERSTONE,
    /// Colorless Map artifact token with "{1}, {T}, Sacrifice: Target creature explores."
    MAP,
    /// Colorless Junk artifact token with "{T}, Sacrifice: Exile top card, may play this turn."
    JUNK,
    /// Colorless Lander artifact token with "{2}, {T}, Sacrifice: Search for basic land."
    LANDER,

    // Enchantment token (111.10e)
    /// Colorless Shard enchantment token with "{2}, Sacrifice: Scry 1, then draw a card."
    SHARD,

    // Role tokens (111.10j-r) - Colorless Aura Role enchantment tokens with names
    /// Aura Role named Cursed with "Enchanted creature has base power and toughness 1/1."
    CURSED_ROLE,
    /// Aura Role named Monster with "Enchanted creature gets +1/+1 and has trample."
    MONSTER_ROLE,
    /// Aura Role named Royal with "Enchanted creature gets +1/+1 and has ward {1}."
    ROYAL_ROLE,
    /// Aura Role named Sorcerer with "Enchanted creature gets +1/+1 and has 'When attacks, scry 1.'"
    SORCERER_ROLE,
    /// Aura Role named Virtuous with "Enchanted creature gets +1/+1 for each enchantment you control."
    VIRTUOUS_ROLE,
    /// Aura Role named Wicked with "+1/+1 and 'When this dies, each opponent loses 1 life.'"
    WICKED_ROLE,
    /// Aura Role named Young Hero with "'When attacks, if toughness 3 or less, +1/+1 counter.'"
    YOUNG_HERO_ROLE,

    // Creature token (111.10d)
    /// 2/2 black Zombie creature token named Walker.
    WALKER,

    // Special token (111.10i)
    /// Double-faced token: front is Incubator artifact, back is 0/0 Phyrexian artifact creature.
    INCUBATOR
}
