package be.imgn.mtg.engine.oracle2.domain;

/// A parsed ability ({@mtg.rule 113}). Sealed at the four ability
/// types of rule 113.3: [Static], [Triggered], [Activated], [Spell].
///
/// Keyword abilities ({@mtg.rule 702}) are *not* a fifth type — each
/// is a shortcut for an ability of one of the four types. Today the
/// hierarchy carries only the parameter-less keyword enums
/// ([StaticKeyword] / [TriggeredKeyword]); written-out ability
/// records and parameterised keywords (Ward, Toxic, Equip, …) will
/// land as the parser grows.
///
/// **Pure domain — no string fields, no parsing metadata.** The
/// text→enum mapping for keyword names lives in
/// `oracle2.parser.selector.AbilitySelectorParser`.
public sealed interface Ability permits Ability.Static, Ability.Triggered, Ability.Activated, Ability.Spell {

    /// Rule 702 — static keyword abilities. Sealed via the
    /// implicit-permits rule: the nested [StaticKeyword] enum is
    /// auto-permitted as the only same-file subtype today.
    sealed interface Static extends Ability {}

    /// Rule 603 / 702 — triggered keyword abilities. Sealed via the
    /// implicit-permits rule.
    sealed interface Triggered extends Ability {}

    /// Rule 602 — activated abilities. No concrete arms yet;
    /// `non-sealed` so future parameterised keywords (Equip, Cycling, …)
    /// and written-out activated abilities can implement it without
    /// touching this file.
    non-sealed interface Activated extends Ability {}

    /// Rule 113.3 — spell abilities. No concrete arms yet;
    /// `non-sealed` for the same reason as [Activated].
    non-sealed interface Spell extends Ability {}

    /// All parameter-less static keyword abilities ({@mtg.rule 702}).
    /// Each constant corresponds to one keyword. Order is
    /// alphabetical except where a longer-prefix keyword must
    /// precede a shorter one in parser dispatch — currently no such
    /// overlap exists, so plain alphabetical works.
    enum StaticKeyword implements Static {
        BANDING,
        CHANGELING,
        DEATHTOUCH,
        DECAYED,
        DEFENDER,
        DOUBLE_STRIKE,
        FEAR,
        FIRST_STRIKE,
        FLASH,
        FLYING,
        HASTE,
        HEXPROOF,
        HORSEMANSHIP,
        INDESTRUCTIBLE,
        INFECT,
        INTIMIDATE,
        LIFELINK,
        MENACE,
        REACH,
        SHADOW,
        SHROUD,
        TRAMPLE,
        VIGILANCE,
        WITHER
    }

    /// All parameter-less triggered keyword abilities ({@mtg.rule 702}).
    enum TriggeredKeyword implements Triggered {
        PROWESS
    }
}
