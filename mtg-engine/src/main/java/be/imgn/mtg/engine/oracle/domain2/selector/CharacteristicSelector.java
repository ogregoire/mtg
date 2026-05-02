package be.imgn.mtg.engine.oracle.domain2.selector;

/// Selects an object by one of its characteristics (CR 109.3). One
/// permitted sub-interface per characteristic the rules name.
public sealed interface CharacteristicSelector extends ObjectPropertySelector
        permits CardTypeSelector,
                SupertypeSelector,
                SubtypeSelector,
                NameSelector,
                ColorSelector,
                PowerSelector,
                ToughnessSelector,
                ManaCostSelector,
                RulesTextSelector,
                AbilitySelector,
                ColorIndicatorSelector,
                LoyaltySelector,
                DefenseSelector {}
