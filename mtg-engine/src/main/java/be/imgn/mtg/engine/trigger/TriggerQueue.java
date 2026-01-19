package be.imgn.mtg.engine.trigger;

import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.Stack;

/// Queue of triggered abilities waiting to be put onto the stack ({@mtg.rule 603.3}).
///
/// When multiple triggered abilities trigger simultaneously, they are added to
/// this queue and then placed onto the stack in APNAP order (Active Player,
/// Non-Active Player) when a player would receive priority.
///
/// Each player chooses the order of their own triggers.
public interface TriggerQueue {

    /// Adds a triggered ability instance to the queue.
    ///
    /// @param instance the triggered ability instance
    void add(TriggeredAbilityInstance instance);

    /// Adds multiple triggered ability instances to the queue.
    ///
    /// @param instances the triggered ability instances
    void addAll(List<TriggeredAbilityInstance> instances);

    /// Returns true if there are pending triggers in the queue.
    ///
    /// @return true if triggers are waiting
    boolean hasPending();

    /// Flushes all pending triggers onto the stack in APNAP order.
    ///
    /// The active player puts their triggers on the stack first (in their chosen
    /// order), then each other player in turn order does the same.
    ///
    /// @param stack the stack to put abilities on
    /// @param state the current game state
    /// @param activePlayer the active player for APNAP ordering
    void flushToStack(Stack stack, GameState state, Player activePlayer);

    /// Clears all pending triggers from the queue.
    ///
    /// This might be called when the game ends or resets.
    void clear();
}
