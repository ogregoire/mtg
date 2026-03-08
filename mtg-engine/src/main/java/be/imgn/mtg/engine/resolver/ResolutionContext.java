package be.imgn.mtg.engine.resolver;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.state.GameState;

/// Context available during spell/ability resolution.
///
/// The spell remains on the stack while its effects execute ({@mtg.rule 608.2n}).
///
/// @param gameState the current game state
/// @param spell the resolving spell (source for damage, etc.)
/// @param controller the spell/ability controller
/// @param spellContext the casting-time decisions (targets, etc.)
/// @param eventProcessor the event processor for game state changes
public record ResolutionContext(
        GameState gameState,
        Spell spell,
        Player controller,
        SpellContext spellContext,
        GameEventProcessor eventProcessor) {}
