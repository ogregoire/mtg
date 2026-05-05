package be.imgn.mtg.engine.oracle2.domain;

/// A predicate the engine can check at trigger-condition time
/// ({@mtg.rule 603.4}, intervening-if) or in the body of a
/// conditional effect.
///
/// **Placeholder.** No real arms today — the field exists on
/// [Ability.TriggeredAbility#interveningIf] so the slot has the
/// right type for the parsers and tests to come, but the parser
/// always passes `null` until concrete predicates ([PlayerControls],
/// [HasManaValue], …) land. The [Never] sentinel is the only
/// permitted subtype so the sealed declaration compiles.
public sealed interface Condition permits Condition.Never {

    /// Sentinel — never produced by any parser. Exists solely to
    /// satisfy the sealed declaration until real condition arms are
    /// added.
    enum Never implements Condition {
        NEVER
    }
}
