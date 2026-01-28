package be.imgn.mtg.engine.turn.internal.sba;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Guice module that provides all state-based actions in Rule 704.5 order.
public final class SbaModule extends AbstractModule {

    @Provides
    @Singleton
    List<StateBasedAction> provideStateBasedActions(GameState gameState) {
        return List.of(
                new ZeroLifeSBA(gameState), // 704.5a
                new EmptyLibraryLossSBA(gameState), // 704.5b
                new PoisonCounterLossSBA(gameState), // 704.5c
                new TokenCeasesToExistSBA(gameState), // 704.5d
                new CopyCeasesToExistSBA(gameState), // 704.5e
                new ZeroToughnessSBA(gameState), // 704.5f
                new LethalDamageSBA(gameState), // 704.5g
                new DeathtouchDamageSBA(gameState), // 704.5h
                new ZeroLoyaltySBA(gameState), // 704.5i
                new LegendRuleSBA(gameState), // 704.5j
                new WorldRuleSBA(gameState), // 704.5k
                new IllegalAuraSBA(gameState), // 704.5m
                new IllegalEquipmentSBA(gameState), // 704.5n
                new IllegalAttachmentSBA(gameState), // 704.5p
                new CounterCancellationSBA(gameState), // 704.5q
                new CounterLimitSBA(gameState), // 704.5r
                new SagaFinalChapterSBA(gameState), // 704.5s
                new DungeonCompletedSBA(gameState), // 704.5t
                new BattleZeroDefenseSBA(gameState), // 704.5v
                new BattleNoProtectorSBA(gameState), // 704.5w
                new SiegeProtectorSBA(gameState), // 704.5x
                new MultipleRolesSBA(gameState) // 704.5y
                );
    }
}
