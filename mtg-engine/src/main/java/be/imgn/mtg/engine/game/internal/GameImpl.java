package be.imgn.mtg.engine.game.internal;

import java.util.List;

import be.imgn.mtg.engine.game.Game;
import be.imgn.mtg.engine.game.Player;

/// Default implementation of [Game].
final class GameImpl implements Game {

    private final List<Player> players;

    GameImpl(List<Player> players) {
        this.players = players;
    }
}
