package be.imgn.mtg.engine.util;

import java.util.stream.Gatherer;

/// Additional gatherers for stream processing.
public final class MoreGatherers {

    private MoreGatherers() {}

    /// Returns a gatherer that filters elements by type and casts them.
    ///
    /// @param type the class to filter by
    /// @param <T> the input type
    /// @param <R> the output type
    /// @return a gatherer that filters and casts elements
    public static <T, R> Gatherer<T, Void, R> instanceOf(Class<R> type) {
        return Gatherer.of((_, element, downstream) -> {
            if (type.isInstance(element)) {
                return downstream.push(type.cast(element));
            }
            return true;
        });
    }
}
