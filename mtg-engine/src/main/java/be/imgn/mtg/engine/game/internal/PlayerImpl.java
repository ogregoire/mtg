package be.imgn.mtg.engine.game.internal;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

/// Default implementation of [Player].
class PlayerImpl implements Player {

    private final PlayerData playerData;
    private final Library library;
    private final Hand hand;
    private final Graveyard graveyard;

    PlayerImpl(PlayerData playerData, Library library, Hand hand, Graveyard graveyard) {
        this.playerData = playerData;
        this.library = library;
        this.hand = hand;
        this.graveyard = graveyard;
    }

    @Override
    public Library library() {
        return library;
    }

    @Override
    public Hand hand() {
        return hand;
    }

    @Override
    public Graveyard graveyard() {
        return graveyard;
    }
}
