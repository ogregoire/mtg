package be.imgn.mtg.engine.oracle2.domain.effect;

/// Root of the effect hierarchy: anything an oracle-text sentence can
/// instruct the game to do. Each arm is a single primitive game
/// action (Destroy, Draw, …) or a composition of them
/// ([SharedSubjectEffect]).
public sealed interface Effect
        permits AddCounterEffect,
                AddManaEffect,
                CantAttackEffect,
                CantBeBlockedEffect,
                CantBeCounteredEffect,
                CantBlockEffect,
                CantCastEffect,
                CantCycleEffect,
                CantSearchLibraryEffect,
                ChoiceEffect,
                CounterEffect,
                DamageEffect,
                DestroyEffect,
                DiscardEffect,
                DrawEffect,
                ExileEffect,
                FlipEffect,
                GainLifeEffect,
                LegendRuleEffect,
                LookAtEffect,
                LoseLifeEffect,
                MayPlayLandFromZoneEffect,
                MillEffect,
                MustBeBlockedEffect,
                PlayWithHandsRevealedEffect,
                RegenerateEffect,
                RingTemptsEffect,
                SacrificeEffect,
                ScryEffect,
                SharedSubjectEffect,
                SetLifeTotalEffect,
                ShuffleEffect,
                SkipStepEffect,
                TakeExtraTurnEffect,
                TapEffect,
                TransformEffect,
                UntapEffect {}
