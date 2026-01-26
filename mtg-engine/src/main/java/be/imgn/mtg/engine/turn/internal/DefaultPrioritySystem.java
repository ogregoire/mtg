package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PrioritySystem;

/// Default implementation of [PrioritySystem].
///
/// Tracks who has priority, which players have passed, and provides APNAP ordering.
final class DefaultPrioritySystem implements PrioritySystem {

    private final GameState gameState;
    private final Set<Player> passedPlayers;

    private @Nullable Player currentHolder;

    DefaultPrioritySystem(GameState gameState) {
        this.gameState = gameState;
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
        var order = getAPNAPOrder();
        var currentIndex = order.indexOf(player);
        var nextIndex = (currentIndex + 1) % order.size();
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

    // ===== APNAP Order =====

    @Override
    public List<Player> getAPNAPOrder() {
        return getAPNAPOrder(gameState.activePlayer());
    }

    @Override
    public List<Player> getAPNAPOrder(Player activePlayer) {
        var players = gameState.players();
        var activeIndex = players.indexOf(activePlayer);

        if (activeIndex < 0) {
            throw new IllegalArgumentException("Active player not in game: " + activePlayer);
        }

        // Build APNAP order starting from active player
        List<Player> order = new ArrayList<>(players.size());
        for (var i = 0; i < players.size(); i++) {
            order.add(players.get((activeIndex + i) % players.size()));
        }
        return List.copyOf(order);
    }
}
