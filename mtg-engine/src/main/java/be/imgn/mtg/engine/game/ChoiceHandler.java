package be.imgn.mtg.engine.game;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/// Handles player choices - can be UI, AI, network, or test stub.
///
/// This is the async interface for presenting choices to players. Implementations
/// return a CompletableFuture that completes when the player makes their selection.
///
/// Common implementations:
/// - FirstOptionChoiceHandler: Selects first option(s) automatically (for testing/AI)
/// - UI handlers: Display choices to user and complete when user clicks
/// - Network handlers: Send choice to remote client and complete when response arrives
///
/// @see Player#choose(Choice) for the synchronous API that wraps this
public interface ChoiceHandler {

    /// Presents a choice to the player and returns a future that completes with their selection.
    ///
    /// The returned future completes with the selected Options from the Choice.
    /// The number of selections must satisfy the Choice's SelectionCount.
    ///
    /// @param choice the choice to present
    /// @param <T> the type of the underlying values
    /// @return a future that completes with the selected Options
    <T> CompletableFuture<List<Option<T>>> choose(Choice<T> choice);
}
