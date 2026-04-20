package be.imgn.mtg.engine.util;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import org.jspecify.annotations.Nullable;

/// A collection that supports order-independent equality, like [Set],
/// but may have duplicate elements.
///
/// Elements of a multiset that are equal to one another are referred to as
/// *occurrences* of the same single element. The total number of
/// occurrences of an element in a multiset is called the *count* of that
/// element.
///
/// @param <E> the type of elements in this multiset
public interface Multiset<E extends @Nullable Object> extends Collection<E> {

    /// Returns the total number of all occurrences of all elements in this multiset.
    /// **Note: this method does not return the number of <i>distinct elements</i> in the
    /// multiset, which is given by `entrySet().size()`.
    ///
    /// @return the total number of occurrences
    @Override
    int size();

    /// Returns the number of occurrences of an element in this multiset.
    ///
    /// @param element the element to count occurrences of
    /// @return the number of occurrences of the element
    int count(@Nullable Object element);

    /// Adds a number of occurrences of an element to this multiset.
    ///
    /// @param element     the element to add occurrences of
    /// @param occurrences the number of occurrences to add (must be non-negative)
    /// @return the count of the element before the operation
    /// @throws IllegalArgumentException if occurrences is negative
    int add(E element, int occurrences);

    /// Removes a number of occurrences of an element from this multiset.
    ///
    /// @param element     the element to remove occurrences of
    /// @param occurrences the number of occurrences to remove (must be non-negative)
    /// @return the count of the element before the operation
    /// @throws IllegalArgumentException if occurrences is negative
    int remove(Object element, int occurrences);

    /// Sets the count of an element to a specific value.
    ///
    /// @param element the element to set the count for
    /// @param count   the desired count (must be non-negative)
    /// @return the count of the element before the operation
    /// @throws IllegalArgumentException if count is negative
    int setCount(E element, int count);

    /// Conditionally sets the count of an element to a new value, if the current
    /// count matches the expected old count.
    ///
    /// @param element  the element to set the count for
    /// @param oldCount the expected current count
    /// @param newCount the desired new count (must be non-negative)
    /// @return `true` if the count was successfully changed
    /// @throws IllegalArgumentException if oldCount or newCount is negative
    boolean setCount(E element, int oldCount, int newCount);

    /// Returns the set of distinct elements contained in this multiset.
    ///
    /// @return an unmodifiable view of the set of distinct elements
    Set<E> elementSet();

    /// Returns a view of the entries in this multiset.
    ///
    /// @return an unmodifiable view of the set of entries
    Set<Entry<E>> entrySet();

    /// An entry in a multiset, representing an element and its count.
    ///
    /// @param <E> the type of the element
    interface Entry<E extends @Nullable Object> {

        /// Returns the element corresponding to this entry.
        ///
        /// @return the element
        E element();

        /// Returns the count of the element in the multiset.
        ///
        /// @return the count (always positive)
        int count();

        @Override
        boolean equals(@Nullable Object o);

        @Override
        int hashCode();

        /// Returns the canonical string representation of this entry.
        /// If the count is one, returns the string representation of the element.
        /// Otherwise, returns the element followed by `" x "` and the count.
        @Override
        String toString();
    }

    /// Performs the given action for each distinct element and its count.
    ///
    /// @param action the action to perform for each element-count pair
    /// @throws NullPointerException if the action is null
    default void forEachEntry(ObjIntConsumer<? super E> action) {
        requireNonNull(action);
        entrySet().forEach(entry -> action.accept(entry.element(), entry.count()));
    }

    /// Performs the given action for each occurrence of each element.
    /// If an element has count N, the action is invoked N times for that element.
    ///
    /// @param action the action to perform for each occurrence
    /// @throws NullPointerException if the action is null
    @Override
    default void forEach(Consumer<? super E> action) {
        requireNonNull(action);
        entrySet().forEach(entry -> {
            var elem = entry.element();
            var count = entry.count();
            for (var i = 0; i < count; i++) {
                action.accept(elem);
            }
        });
    }

    /// Creates a new empty EnumMultiset for the specified enum type.
    ///
    /// @param type the enum class
    /// @param <E>  the enum type
    /// @return a new empty EnumMultiset
    static <E extends Enum<E>> Multiset<E> newEnumMultiset(Class<E> type) {
        return new EnumMultiset<>(type);
    }

    /// Creates a new empty HashMultiset.
    ///
    /// @param <E> the element type
    /// @return a new empty HashMultiset
    static <E> Multiset<E> newHashMultiset() {
        return new HashMultiset<>();
    }

    /// Returns an immutable copy of the given collection as a multiset.
    ///
    /// If the source is a multiset, element counts are preserved.
    /// Otherwise, each element in the collection contributes one occurrence.
    ///
    /// The returned multiset is a snapshot; subsequent changes to the source
    /// collection will not be reflected in the returned multiset.
    ///
    /// @param <E>    the element type
    /// @param source the collection to copy
    /// @return an immutable multiset copy
    @SuppressWarnings("unchecked")
    static <E> Multiset<E> copyOf(Collection<? extends E> source) {
        if (source instanceof ImmutableMultiset<? extends E> result) {
            return (Multiset<E>) result;
        }
        return new ImmutableMultiset<>(source);
    }
}
