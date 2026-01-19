package be.imgn.mtg.engine.replacement;

import java.util.List;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.ObjectId;

/// Registry for active replacement effects in the game.
///
/// Replacement effects are registered when their sources enter the battlefield
/// or become active, and unregistered when their sources leave.
///
/// The registry finds all applicable effects for an event and provides them
/// in an order that respects Rule 616 ordering.
public interface ReplacementEffectRegistry {

    /// Registers a replacement effect from a source.
    ///
    /// @param effect the replacement effect
    /// @param source the ID of the object that creates this effect
    /// @param controller the player who controls the source
    void register(ReplacementEffect effect, ObjectId source, Player controller);

    /// Unregisters all replacement effects from a source.
    ///
    /// This is called when a permanent leaves the battlefield or an effect ends.
    ///
    /// @param source the ID of the source to unregister
    void unregister(ObjectId source);

    /// Finds all replacement effects that apply to an event.
    ///
    /// The returned list is ordered according to Rule 616:
    /// 1. Self-replacement effects
    /// 2. Control-changing effects (for ETB)
    /// 3. Copy effects (for ETB)
    /// 4. Remaining effects (affected player must choose order)
    ///
    /// @param event the event to check
    /// @return the list of applicable replacements
    List<ApplicableReplacement> findApplicable(GameEvent event);

    /// A replacement effect that is applicable to an event, along with its source info.
    ///
    /// @param effect the replacement effect
    /// @param source the ID of the source permanent
    /// @param controller the player who controls the source
    record ApplicableReplacement(ReplacementEffect effect, ObjectId source, Player controller) {}
}
