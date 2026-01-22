package be.imgn.mtg.engine.game.internal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.ChoiceHandler;
import be.imgn.mtg.engine.game.Option;
import be.imgn.mtg.engine.game.SelectionCount;

/// Default ChoiceHandler that automatically selects the first valid option(s).
///
/// Used for testing and AI players. Always selects immediately (synchronously)
/// with the minimum valid number of options from the start of the list.
public final class FirstOptionChoiceHandler implements ChoiceHandler {

    public static final FirstOptionChoiceHandler INSTANCE = new FirstOptionChoiceHandler();

    private FirstOptionChoiceHandler() {}

    @Override
    public <T> CompletableFuture<List<Option<T>>> choose(Choice<T> choice) {
        var options = choice.options();
        var count = choice.count();

        var numToSelect = determineSelectionCount(count, options.size());
        var selected = options.subList(0, numToSelect);

        return CompletableFuture.completedFuture(List.copyOf(selected));
    }

    /// Determines how many options to select based on the SelectionCount.
    /// Returns the minimum valid count, capped by available options.
    private int determineSelectionCount(SelectionCount count, int available) {
        return switch (count) {
            case SelectionCount.Exactly(var n) -> Math.min(n, available);
            case SelectionCount.Between(var min, var max) -> Math.min(min, available);
            case SelectionCount.UpTo(var max) -> 1; // Select 1 rather than 0 for "up to"
            case SelectionCount.AtLeast(var min) -> Math.min(min, available);
        };
    }
}
