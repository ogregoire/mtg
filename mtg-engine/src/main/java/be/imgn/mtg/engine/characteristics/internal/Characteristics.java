package be.imgn.mtg.engine.characteristics.internal;

import java.util.stream.Stream;

/// Base interface for all characteristic collections in the MTG engine.
///
/// @param <T> the type of elements in this collection
public interface Characteristics<T> extends Iterable<T> {

    /// Returns true if this collection contains no elements.
    boolean isEmpty();

    /// Returns the number of elements in this collection.
    int count();

    /// Returns a stream of elements in this collection.
    Stream<T> stream();

    /// Returns true if this collection contains the specified element.
    ///
    /// @param element the element to check for
    /// @return true if the element is present
    boolean contains(T element);

    /// Returns a builder initialized with the contents of this collection.
    Builder<T, ?, ?> toBuilder();

    /// Builder for creating characteristic collections.
    ///
    /// @param <T> the type of elements
    /// @param <C> the type of collection being built
    /// @param <B> the builder type (for fluent returns)
    interface Builder<T, C extends Characteristics<T>, B extends Builder<T, C, B>> {

        /// Adds an element to the collection.
        B add(T element);

        /// Adds multiple elements to the collection.
        @SuppressWarnings("unchecked")
        B addAll(T... elements);

        /// Adds all elements from another collection.
        B addAll(C characteristics);

        /// Sets the collection to contain only the specified element.
        B set(T element);

        /// Sets the collection to contain only the specified elements.
        @SuppressWarnings("unchecked")
        B set(T... elements);

        /// Removes all elements from the builder.
        B clear();

        /// Builds the collection.
        C build();
    }
}
