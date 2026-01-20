package be.imgn.mtg.engine.result;

import java.util.List;

import be.imgn.mtg.engine.game.Player;

/// Conditions under which the game ends in a draw ({@mtg.rule 104.4}).
///
/// A game can end in a draw when all remaining players lose simultaneously,
/// when an infinite loop cannot be broken, or when a card effect causes a draw.
public sealed interface DrawCondition {

    /// All remaining players lost at the same time ({@mtg.rule 104.4a}).
    ///
    /// @param players the players who lost simultaneously
    record SimultaneousLoss(List<Player> players) implements DrawCondition {}

    /// The game reached an unbreakable infinite loop ({@mtg.rule 104.4b}).
    ///
    /// If a loop is detected and no player can break it, the game ends in a draw.
    record InfiniteLoop() implements DrawCondition {}

    /// A card or effect caused the game to end in a draw ({@mtg.rule 104.4c}).
    ///
    /// @param cause a description of the effect causing the draw (e.g., "Divine Intervention")
    record EffectDraw(String cause) implements DrawCondition {}
}
