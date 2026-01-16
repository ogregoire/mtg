package be.imgn.mtg.engine.game.internal;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;

/// Default implementation of [Player].
class PlayerImpl implements Player {

    private final PlayerData playerData;

    PlayerImpl(PlayerData playerData) {
        this.playerData = playerData;
    }
}
