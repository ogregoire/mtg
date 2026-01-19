package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.StackObject;

/// Event representing a spell or ability being countered ({@mtg.rule 701.5}).
///
/// A countered spell is put into its owner's graveyard. Abilities that are countered
/// simply cease to exist (they don't go to the graveyard).
///
/// @param spell the spell or ability being countered
public record CounterEvent(StackObject spell) implements ZoneChangeEvent {

    @Override
    public ZoneType from() {
        return ZoneType.STACK;
    }

    @Override
    public ZoneType to() {
        return ZoneType.GRAVEYARD;
    }

    @Override
    public Player affectedPlayer() {
        return ((GameObject) spell).controller();
    }
}
