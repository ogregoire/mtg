package be.imgn.mtg.engine.ability.internal.parser.effect;

/// Represents a parsed effect from oracle text.
///
/// This is a sealed interface that permits specific effect types.
public sealed interface Effect
        permits DestroyEffect,
                ExileEffect,
                SacrificeEffect,
                ReturnToHandEffect,
                PutOnLibraryEffect,
                MillEffect,
                DealDamageEffect,
                GainLifeEffect,
                LoseLifeEffect,
                DrawEffect,
                DiscardEffect,
                ScryEffect,
                SearchLibraryEffect,
                TapEffect,
                UntapEffect,
                AddCountersEffect,
                RemoveCountersEffect,
                GainAbilityEffect,
                ModifyPowerToughnessEffect,
                GainControlEffect,
                CreateTokenEffect,
                CounterSpellEffect,
                FightEffect,
                AddManaEffect {}
