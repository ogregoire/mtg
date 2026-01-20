package be.imgn.mtg.engine.turn.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.APNAPOrder;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Default implementation of [PrioritySystem].
///
/// Tracks who has priority and which players have passed.
final class DefaultPrioritySystem implements PrioritySystem {

    private final GameState gameState;
    private final APNAPOrder apnapOrder;
    private final Set<Player> passedPlayers;

    private @Nullable Player currentHolder;

    DefaultPrioritySystem(GameState gameState, APNAPOrder apnapOrder) {
        this.gameState = gameState;
        this.apnapOrder = apnapOrder;
        this.passedPlayers = new HashSet<>();
    }

    @Override
    public @Nullable Player currentPriorityHolder() {
        return currentHolder;
    }

    @Override
    public void pass(Player player) {
        if (!player.equals(currentHolder)) {
            throw new IllegalStateException("Player " + player + " tried to pass but doesn't have priority");
        }

        passedPlayers.add(player);

        // Move priority to next player in APNAP order
        List<Player> order = apnapOrder.getOrder(gameState);
        int currentIndex = order.indexOf(player);
        int nextIndex = (currentIndex + 1) % order.size();
        currentHolder = order.get(nextIndex);
    }

    @Override
    public boolean allPassed() {
        return passedPlayers.containsAll(gameState.players());
    }

    @Override
    public void reset() {
        passedPlayers.clear();
    }

    @Override
    public void givePriority(Player player) {
        currentHolder = player;
    }

    @Override
    public void clearPriority() {
        currentHolder = null;
    }
}
