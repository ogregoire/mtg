package be.imgn.mtg.parse;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/// A collector that collects pairs of elements. Similar to {@link Collector} but operates on pairs.
///
/// @param <A> the type of the first element
/// @param <B> the type of the second element
/// @param <R> the type of the result
public interface BiCollector<A, B, R> {

    /// Functional interface for accumulating pairs.
    ///
    /// @param <E> the accumulator type
    /// @param <A> the first element type
    /// @param <B> the second element type
    @FunctionalInterface
    interface BiAccumulator<E, A, B> {
        void accept(E accumulator, A first, B second);
    }

    /// Converts this BiCollector to a regular Collector that operates on Both instances.
    ///
    /// @param biCollector the bi-collector
    /// @param <A> the first element type
    /// @param <B> the second element type
    /// @param <R> the result type
    /// @return a regular collector that collects Both instances
    static <A, B, R> Collector<Both<A, B>, ?, R> toBothCollector(BiCollector<? super A, ? super B, R> biCollector) {
        // Create a simple adapter that converts BiCollector to Collector
        Supplier<Object[]> boxedSupplier =
                () -> new Object[] {biCollector.supplier().get()};
        BiConsumer<Object[], Both<A, B>> boxedAccumulator = (arr, both) -> {
            @SuppressWarnings("unchecked")
            var acc = arr[0];
            biCollector.accumulator().accept(acc, both.first(), both.second());
        };
        BinaryOperator<Object[]> boxedCombiner = (a, b) -> {
            @SuppressWarnings("unchecked")
            var combined = biCollector.combiner().apply(a[0], b[0]);
            a[0] = combined;
            return a;
        };
        Function<Object[], R> boxedFinisher = arr -> {
            @SuppressWarnings("unchecked")
            var result = biCollector.finisher().apply(arr[0]);
            return result;
        };

        return Collector.of(boxedSupplier, boxedAccumulator, boxedCombiner, boxedFinisher);
    }

    /// Returns a supplier of the accumulator.
    ///
    /// @param <E> the type of the accumulator
    /// @return the supplier
    <E> Supplier<E> supplier();

    /// Returns the accumulator function.
    ///
    /// @param <E> the type of the accumulator
    /// @return the accumulator
    <E> BiAccumulator<E, A, B> accumulator();

    /// Returns the combiner function.
    ///
    /// @param <E> the type of the accumulator
    /// @return the combiner
    <E> BinaryOperator<E> combiner();

    /// Returns the finisher function.
    ///
    /// @param <E> the type of the accumulator
    /// @return the finisher
    <E> Function<E, R> finisher();
}
