package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.APNAPOrder;

/// Default implementation of [APNAPOrder].
///
/// Returns players in Active Player, Non-Active Player order.
final class DefaultAPNAPOrder implements APNAPOrder {

    DefaultAPNAPOrder() {}

    @Override
    public List<Player> getOrder(GameState gameState) {
        return getOrder(gameState, gameState.activePlayer());
    }

    @Override
    public List<Player> getOrder(GameState gameState, Player activePlayer) {
        List<Player> players = gameState.players();
        int activeIndex = players.indexOf(activePlayer);

        if (activeIndex < 0) {
            throw new IllegalArgumentException("Active player not in game: " + activePlayer);
        }

        // Build APNAP order starting from active player
        List<Player> order = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            order.add(players.get((activeIndex + i) % players.size()));
        }
        return List.copyOf(order);
    }
}
