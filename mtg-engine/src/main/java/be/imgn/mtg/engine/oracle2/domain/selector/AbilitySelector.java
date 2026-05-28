package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;

/// Selects an object by an ability it has ({@mtg.rule 113}).
///
/// Three arms:
/// - [Has] — the object has the named [Ability]. Today the parser
///   only fills this with [Ability.StaticKeyword] /
///   [Ability.TriggeredKeyword] constants; parameterised keywords
///   (Ward, Toxic, Protection, …) and written-out abilities will
///   slot in unchanged once the parser learns them.
/// - [HasNot] — the object does NOT have the named [Ability].
///   Oracle phrasing: "creature without flying". Per-ability
///   negation; distinct from [HasNoAbilities] which is the global
///   empty-ability-set predicate.
/// - [HasNoAbilities] — the object has no abilities at all
///   ({@mtg.rule 702 / 113.6}).
///
/// Compound `with X or Y` / `with X and Y` phrases are NOT modeled
/// here — the parser distributes those across multiple
/// `Has(...)` arms and combines them at the
/// [OneOf] / [ObjectPropertySelector.AllOf]
/// layer.
public sealed interface AbilitySelector extends CharacteristicSelector
        permits AbilitySelector.Has, AbilitySelector.HasNot, AbilitySelector.HasNoAbilities {

    record Has(Ability ability) implements AbilitySelector {
        public Has {
            requireNonNull(ability);
        }
    }

    record HasNot(Ability ability) implements AbilitySelector {
        public HasNot {
            requireNonNull(ability);
        }
    }

    record HasNoAbilities() implements AbilitySelector {}
}
