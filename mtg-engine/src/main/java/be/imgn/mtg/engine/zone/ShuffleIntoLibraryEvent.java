package be.imgn.mtg.engine.zone;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Event representing cards being shuffled into a library ({@mtg.rule 701.20}).
///
/// Cards can be shuffled into a library from various zones (graveyard, exile, hand, etc.).
/// The library is then randomized. Multiple cards shuffled at once are a single event.
///
/// @param cards the cards being shuffled into the library
/// @param from the zone they're coming from
/// @param owner the player whose library they're being shuffled into
public record ShuffleIntoLibraryEvent(List<Card> cards, ZoneType from, Player owner) implements ZoneChangeEvent {

    /// Creates a ShuffleIntoLibraryEvent with a defensive copy of the cards list.
    public ShuffleIntoLibraryEvent {
        cards = List.copyOf(cards);
    }

    @Override
    public ZoneType to() {
        return ZoneType.LIBRARY;
    }

    @Override
    public Player affectedPlayer() {
        return owner;
    }
}
