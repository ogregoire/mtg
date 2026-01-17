package be.imgn.mtg.engine.characteristics.internal;

import java.util.stream.Stream;

/// Base interface for all characteristic collections in the MTG engine.
///
/// @param <T> the type of elements in this collection
public interface Characteristics<T> extends Iterable<T> {

    /// Returns true if this collection contains no elements.
    ///
    /// @return true if empty, false otherwise
    boolean isEmpty();

    /// Returns the number of elements in this collection.
    ///
    /// @return the element count
    int count();

    /// Returns a stream of elements in this collection.
    ///
    /// @return a stream of elements
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
        ///
        /// @param element the element to add
        /// @return this builder
        B add(T element);

        /// Adds multiple elements to the collection.
        ///
        /// @param elements the elements to add
        /// @return this builder
        @SuppressWarnings("unchecked")
        B addAll(T... elements);

        /// Adds all elements from another collection.
        ///
        /// @param characteristics the collection to add from
        /// @return this builder
        B addAll(C characteristics);

        /// Sets the collection to contain only the specified element.
        ///
        /// @param element the element to set
        /// @return this builder
        B set(T element);

        /// Sets the collection to contain only the specified elements.
        ///
        /// @param elements the elements to set
        /// @return this builder
        @SuppressWarnings("unchecked")
        B set(T... elements);

        /// Removes all elements from the builder.
        ///
        /// @return this builder
        B clear();

        /// Builds the collection.
        ///
        /// @return the built collection
        C build();
    }
}
