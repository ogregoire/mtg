package be.imgn.mtg.engine.state.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.result.GameResult;
import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.selector.Selector;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.LocatedObject;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.Zone;

/// Default implementation of [GameState].
///
/// Aggregates all zones and provides lookup functionality.
public final class DefaultGameState implements GameState {

    private final ObjectStore store;
    private final Battlefield battlefield;
    private final Stack stack;
    private final Exile exile;
    private final CommandZone commandZone;
    private final LastKnownInformation lki;
    private final List<Player> players;

    private final Map<Player, Library> libraries;
    private final Map<Player, Hand> hands;
    private final Map<Player, Graveyard> graveyards;

    private @Nullable Player activePlayer;
    private @Nullable GameResult result;

    /// Creates a new game state with the given zones.
    ///
    /// @param store the central object store
    /// @param battlefield the battlefield zone
    /// @param stack the stack zone
    /// @param exile the exile zone
    /// @param commandZone the command zone
    /// @param lki the last known information tracker
    /// @param players the players in the game
    public DefaultGameState(
            ObjectStore store,
            Battlefield battlefield,
            Stack stack,
            Exile exile,
            CommandZone commandZone,
            LastKnownInformation lki,
            List<Player> players) {
        this.store = store;
        this.battlefield = battlefield;
        this.stack = stack;
        this.exile = exile;
        this.commandZone = commandZone;
        this.lki = lki;
        this.players = List.copyOf(players);

        this.libraries = new HashMap<>();
        this.hands = new HashMap<>();
        this.graveyards = new HashMap<>();
        for (var player : players) {
            libraries.put(player, player.library());
            hands.put(player, player.hand());
            graveyards.put(player, player.graveyard());
        }
    }

    @Override
    public Battlefield battlefield() {
        return battlefield;
    }

    @Override
    public Stack stack() {
        return stack;
    }

    @Override
    public Exile exile() {
        return exile;
    }

    @Override
    public CommandZone commandZone() {
        return commandZone;
    }

    @Override
    public Library library(Player player) {
        var library = libraries.get(player);
        if (library == null) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
        return library;
    }

    @Override
    public Hand hand(Player player) {
        var hand = hands.get(player);
        if (hand == null) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
        return hand;
    }

    @Override
    public Graveyard graveyard(Player player) {
        var graveyard = graveyards.get(player);
        if (graveyard == null) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
        return graveyard;
    }

    @Override
    public Stream<LocatedObject> objects() {
        return store.stream();
    }

    @Override
    public Optional<Zone<?>> findZone(GameObject object) {
        return store.findZone(object);
    }

    @Override
    public Stream<Selectable> select(Selector selector, Player perspective) {
        Stream<Selectable> objects = store.stream().map(lo -> lo.object());
        Stream<Selectable> playerStream = players.stream().map(Selectable.class::cast);
        return Stream.concat(objects, playerStream).filter(s -> selector.matches(s, perspective));
    }

    @Override
    public LastKnownInformation lastKnownInformation() {
        return lki;
    }

    @Override
    public Player activePlayer() {
        if (activePlayer == null) {
            throw new IllegalStateException("Active player not yet set");
        }
        return activePlayer;
    }

    @Override
    public void setActivePlayer(Player player) {
        if (!players.contains(player)) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
        this.activePlayer = player;
    }

    @Override
    public List<Player> players() {
        return players;
    }

    @Override
    public Player nextPlayerInTurnOrder(Player current) {
        var index = players.indexOf(current);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown player: " + current);
        }
        return players.get((index + 1) % players.size());
    }

    @Override
    public boolean isGameOver() {
        return result != null;
    }

    @Override
    public Optional<GameResult> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public void setResult(GameResult result) {
        this.result = result;
    }

    @Override
    public void emptyManaPools() {
        // TODO: Implement mana pool emptying when mana pools are added
        // For now, this is a no-op
    }
}
