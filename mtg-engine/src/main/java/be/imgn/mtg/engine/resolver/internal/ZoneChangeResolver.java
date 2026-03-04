package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;
import be.imgn.mtg.engine.zone.CastEvent;
import be.imgn.mtg.engine.zone.CounterEvent;
import be.imgn.mtg.engine.zone.DiesEvent;
import be.imgn.mtg.engine.zone.DiscardEvent;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.EntersBattlefieldEvent;
import be.imgn.mtg.engine.zone.ExileEvent;
import be.imgn.mtg.engine.zone.LeavesBattlefieldEvent;
import be.imgn.mtg.engine.zone.MillEvent;
import be.imgn.mtg.engine.zone.PutIntoGraveyardEvent;
import be.imgn.mtg.engine.zone.ReturnToHandEvent;
import be.imgn.mtg.engine.zone.ShuffleIntoLibraryEvent;
import be.imgn.mtg.engine.zone.ZoneChangeEvent;
import be.imgn.mtg.engine.zone.ZoneType;

/// Resolver for all zone change events.
///
/// Handles the common pattern of:
/// 1. Recording last known information before the object leaves its zone
/// 2. Removing the object from the source zone
/// 3. Adding the object to the destination zone
public final class ZoneChangeResolver implements EventResolver<ZoneChangeEvent> {

    private final LastKnownInformation lki;

    public ZoneChangeResolver(LastKnownInformation lki) {
        this.lki = lki;
    }

    @Override
    public Class<ZoneChangeEvent> eventType() {
        return ZoneChangeEvent.class;
    }

    @Override
    public void resolve(ZoneChangeEvent event, GameState state) {
        switch (event) {
            case DrawEvent e -> resolveDraw(e, state);
            case DiscardEvent e -> resolveDiscard(e, state);
            case MillEvent e -> resolveMill(e, state);
            case DiesEvent e -> resolveDies(e, state);
            case ExileEvent e -> resolveExile(e, state);
            case EntersBattlefieldEvent e -> resolveEntersBattlefield(e, state);
            case LeavesBattlefieldEvent e -> resolveLeavesBattlefield(e, state);
            case ReturnToHandEvent e -> resolveReturnToHand(e, state);
            case CastEvent e -> resolveCast(e, state);
            case CounterEvent e -> resolveCountered(e, state);
            case PutIntoGraveyardEvent e -> resolvePutIntoGraveyard(e, state);
            case ShuffleIntoLibraryEvent e -> resolveShuffleIntoLibrary(e, state);
        }
    }

    private void resolveDraw(DrawEvent event, GameState state) {
        var card = event.card();
        var player = event.player();

        recordLki(card, event.from());
        state.library(player).remove(card);
        state.hand(player).add(card);
    }

    private void resolveDiscard(DiscardEvent event, GameState state) {
        var card = event.card();
        var player = event.player();

        recordLki(card, event.from());
        state.hand(player).remove(card);
        state.graveyard(player).put(card);
    }

    private void resolveMill(MillEvent event, GameState state) {
        var player = event.player();

        for (var card : event.cards()) {
            recordLki(card, event.from());
            state.library(player).remove(card);
            state.graveyard(player).put(card);
        }
    }

    private void resolveDies(DiesEvent event, GameState state) {
        var permanent = event.permanent();

        recordLki(permanent, event.from());
        state.battlefield().remove(permanent);

        // Get the underlying card from the permanent's source
        if (permanent.source() instanceof Card card) {
            state.graveyard(permanent.owner()).put(card);
        }
    }

    private void resolveExile(ExileEvent event, GameState state) {
        var object = event.object();

        recordLki(object, event.from());
        removeFromZone(object, event.from(), state);

        // Exile only takes Cards - extract the underlying card from wrapper types
        if (object instanceof Card card) {
            state.exile().exile(card);
        } else if (object instanceof Spell spell && spell.source() instanceof Card card) {
            state.exile().exile(card);
        } else if (object instanceof Permanent permanent && permanent.source() instanceof Card card) {
            state.exile().exile(card);
        }
    }

    private void resolveEntersBattlefield(EntersBattlefieldEvent event, GameState state) {
        var permanent = event.permanent();

        // The permanent source should be removed from its current zone
        if (permanent.source() instanceof Card card) {
            recordLki(card, event.from());
            removeFromZone(card, event.from(), state);
        }

        state.battlefield().enter(permanent);
    }

    private void resolveLeavesBattlefield(LeavesBattlefieldEvent event, GameState state) {
        var permanent = event.permanent();

        recordLki(permanent, event.from());
        state.battlefield().remove(permanent);

        // The destination zone handling is done by the more specific event
        // (DiesEvent, ExileEvent, ReturnToHandEvent, etc.)
    }

    private void resolveReturnToHand(ReturnToHandEvent event, GameState state) {
        var object = event.object();
        var owner = object.owner();

        recordLki(object, event.from());
        removeFromZone(object, event.from(), state);

        if (object instanceof Card card) {
            state.hand(owner).add(card);
        } else if (object instanceof Permanent permanent && permanent.source() instanceof Card card) {
            state.hand(owner).add(card);
        }
    }

    private void resolveCast(CastEvent event, GameState state) {
        var card = event.card();

        recordLki(card, event.from());
        removeFromZone(card, event.from(), state);

        // Create a spell from the card and push to stack
        var newSpell = Spell.fromCard(card, event.caster()).build();
        state.stack().push(newSpell);
    }

    private void resolveCountered(CounterEvent event, GameState state) {
        var stackObject = event.spell();

        if (stackObject instanceof Spell spell) {
            recordLki(spell, event.from());
            state.stack().remove(spell);

            if (spell.source() instanceof Card card) {
                state.graveyard(spell.owner()).put(card);
            }
        }
        // AbilityOnStack objects just cease to exist when countered
    }

    private void resolvePutIntoGraveyard(PutIntoGraveyardEvent event, GameState state) {
        var object = event.object();

        recordLki(object, event.from());
        removeFromZone(object, event.from(), state);

        switch (object) {
            case Card card -> state.graveyard(card.owner()).put(card);
            case Spell spell when spell.source() instanceof Card card -> state.graveyard(spell.owner()).put(card);
            case Permanent permanent when permanent.source() instanceof Card card ->
                    state.graveyard(permanent.owner()).put(card);
            default -> {
            }
        }
    }

    private void resolveShuffleIntoLibrary(ShuffleIntoLibraryEvent event, GameState state) {
        var owner = event.owner();

        for (var card : event.cards()) {
            recordLki(card, event.from());
            removeFromZone(card, event.from(), state);
            state.library(owner).putOnBottom(card);
        }

        state.library(owner).shuffle();
    }

    private void recordLki(TypedObject object, ZoneType zone) {
        lki.record(object, ObjectSnapshot.of(object, zone));
    }

    private void removeFromZone(GameObject object, ZoneType zone, GameState state) {
        switch (zone) {
            case LIBRARY -> {
                if (object instanceof Card card) state.library(object.owner()).remove(card);
            }
            case HAND -> {
                if (object instanceof Card card) state.hand(object.owner()).remove(card);
            }
            case BATTLEFIELD -> {
                if (object instanceof Permanent perm) state.battlefield().remove(perm);
            }
            case GRAVEYARD -> {
                if (object instanceof Card card) state.graveyard(object.owner()).remove(card);
            }
            case STACK -> {
                if (object instanceof Spell spell) state.stack().remove(spell);
            }
            case EXILE -> {
                if (object instanceof Card card) state.exile().remove(card);
            }
            case COMMAND -> {
                if (object instanceof Card card) state.commandZone().removeCommander(card);
            }
        }
    }
}
