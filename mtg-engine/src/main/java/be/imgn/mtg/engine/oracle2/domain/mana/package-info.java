/// Domain types for the rule-107.4 mana-symbol catalog: [ManaType] (the
/// six types of mana per {@mtg.rule 106.1}) and the [ManaSymbol] sealed
/// hierarchy with one permitted shape per rule-107.4 category. Also
/// hosts [Mana] (a single mana instance — symbol plus optional
/// per-instance [Restriction]), [ProducedMana] (the ordered list of
/// [Mana] an [be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect]
/// produces in one resolution), and [Restriction] (rule 106.6 spend
/// restrictions, distributed per [Mana] instance).
package be.imgn.mtg.engine.oracle2.domain.mana;
