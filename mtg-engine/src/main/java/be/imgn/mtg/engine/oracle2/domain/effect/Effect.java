package be.imgn.mtg.engine.oracle2.domain.effect;

/// Root of the effect hierarchy: anything an oracle-text sentence can
/// instruct the game to do. Each arm is a single primitive game
/// action (Destroy, Draw, …) or a composition of them
/// ([SharedSubjectEffect]).
public sealed interface Effect
        permits AddManaEffect,
                DestroyEffect,
                DiscardEffect,
                DrawEffect,
                ExileEffect,
                GainLifeEffect,
                LoseLifeEffect,
                SacrificeEffect,
                ScryEffect,
                SharedSubjectEffect {}
