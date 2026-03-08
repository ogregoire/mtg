package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.spell.SpellContext;

/// Event representing a card being cast as a spell ({@mtg.rule 601}).
///
/// Casting a spell moves the card from its current zone (typically hand, but can be
/// graveyard with flashback, exile with cascade, etc.) to the stack.
///
/// @param card the card being cast
/// @param from the zone it's being cast from
/// @param caster the player casting the spell
/// @param context the spell context with targets and other casting decisions
public record CastEvent(Card card, ZoneType from, Player caster, SpellContext context) implements ZoneChangeEvent {

    @Override
    public ZoneType to() {
        return ZoneType.STACK;
    }

    @Override
    public Player affectedPlayer() {
        return caster;
    }
}
