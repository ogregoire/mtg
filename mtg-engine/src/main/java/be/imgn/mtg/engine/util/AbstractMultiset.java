package be.imgn.mtg.engine.util;

import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// Abstract base class for {@link Multiset} implementations.
///
/// @param <E> the type of elements in this multiset
abstract class AbstractMultiset<E extends @Nullable Object> extends AbstractCollection<E> implements Multiset<E> {

    @Override
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object element) {
        return count(element) > 0;
    }

    // Modification Operations
    @Override
    public final boolean add(E element) {
        add(element, 1);
        return true;
    }

    @Override
    public int add(E element, int occurrences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(@Nullable Object element) {
        return remove(element, 1) > 0;
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setCount(E element, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count.");
        }

        var oldCount = count(element);

        var delta = count - oldCount;
        if (delta > 0) {
            add(element, delta);
        } else if (delta < 0) {
            remove(element, -delta);
        }

        return oldCount;
    }

    @Override
    public boolean setCount(E element, int oldCount, int newCount) {
        if (oldCount < 0) {
            throw new IllegalArgumentException("Negative oldCount.");
        }
        if (newCount < 0) {
            throw new IllegalArgumentException("Negative newCount.");
        }
        if (count(element) == oldCount) {
            setCount(element, newCount);
            return true;
        } else {
            return false;
        }
    }

    // Bulk Operations

    /**
     * {@inheritDoc}
     *
     * <p>This implementation is highly efficient when {@code elementsToAdd} is itself a {@link Multiset}.
     */
    @Override
    public final boolean addAll(Collection<? extends E> elements) {
        requireNonNull(elements);
        if (elements.isEmpty()) {
            return false;
        } else if (elements instanceof Multiset<? extends E> multiset) {
            multiset.forEachEntry(this::add);
            return true;
        } else {
            for (var element : elements) {
                add(element);
            }
            return true;
        }
    }

    @Override
    public final boolean removeAll(Collection<?> elementsToRemove) {
        var collection = (elementsToRemove instanceof Multiset<?> multiset) ? multiset.elementSet() : elementsToRemove;

        return elementSet().removeAll(collection);
    }

    @Override
    public final boolean retainAll(Collection<?> elementsToRetain) {
        var collection = (elementsToRetain instanceof Multiset<?> multiset) ? multiset.elementSet() : elementsToRetain;

        return elementSet().retainAll(collection);
    }

    @Override
    public abstract void clear();

    // Views

    private transient @Nullable Set<E> elementSet;

    @Override
    public Set<E> elementSet() {
        var result = elementSet;
        if (result == null) {
            elementSet = result = createElementSet();
        }
        return result;
    }

    /** Creates a new instance of this multiset's element set, which will be returned by {@link #elementSet()}. */
    Set<E> createElementSet() {
        return new ElementSet();
    }

    final class ElementSet extends AbstractSet<E> {

        @Override
        public void clear() {
            AbstractMultiset.this.clear();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return AbstractMultiset.this.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return AbstractMultiset.this.containsAll(c);
        }

        @Override
        public boolean isEmpty() {
            return AbstractMultiset.this.isEmpty();
        }

        @Override
        public boolean remove(@Nullable Object o) {
            return AbstractMultiset.this.remove(o, Integer.MAX_VALUE) > 0;
        }

        @Override
        public int size() {
            return AbstractMultiset.this.entrySet().size();
        }

        @Override
        public Iterator<E> iterator() {
            return elementIterator();
        }
    }

    abstract Iterator<E> elementIterator();

    private transient @Nullable Set<Entry<E>> entrySet;

    @Override
    public Set<Entry<E>> entrySet() {
        var result = entrySet;
        if (result == null) {
            entrySet = result = createEntrySet();
        }
        return result;
    }

    class EntrySet extends AbstractSet<Entry<E>> {
        @Override
        public boolean contains(@Nullable Object o) {
            if (o instanceof Entry<?> entry) {
                if (entry.count() <= 0) {
                    return false;
                }
                var count = AbstractMultiset.this.count(entry.element());
                return count == entry.count();
            }
            return false;
        }

        @Override
        public boolean remove(@Nullable Object object) {
            if (object instanceof Multiset.Entry<?> entry) {
                var element = entry.element();
                var entryCount = entry.count();
                if (entryCount != 0) {
                    // Safe as long as we never add a new entry, which we won't.
                    // (Presumably it can still throw CCE/NPE but only if the underlying Multiset does.)
                    @SuppressWarnings({"unchecked", "nullness"})
                    var multiset = (Multiset<@Nullable Object>) AbstractMultiset.this;
                    return multiset.setCount(element, entryCount, 0);
                }
            }
            return false;
        }

        @Override
        public void clear() {
            AbstractMultiset.this.clear();
        }

        @Override
        public Iterator<Entry<E>> iterator() {
            return entryIterator();
        }

        @Override
        public int size() {
            return distinctElements();
        }
    }

    Set<Entry<E>> createEntrySet() {
        return new EntrySet();
    }

    abstract Iterator<Entry<E>> entryIterator();

    abstract int distinctElements();

    // Object methods

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns {@code true} if {@code object} is a multiset of the same size and if, for each
     * element, the two multisets have the same count.
     */
    @Override
    public final boolean equals(@Nullable Object object) {
        if (object instanceof Multiset<?> other) {

            if (size() != other.size() || entrySet().size() != other.entrySet().size()) {
                return false;
            }
            for (var entry : other.entrySet()) {
                if (count(entry.element()) != entry.count()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns the hash code of {@link Multiset#entrySet()}.
     */
    @Override
    public final int hashCode() {
        return entrySet().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns the result of invoking {@code toString} on {@link Multiset#entrySet()}.
     */
    @Override
    public final String toString() {
        return entrySet().toString();
    }

    protected abstract static class AbstractEntry<E extends @Nullable Object> implements Multiset.Entry<E> {
        /**
         * Indicates whether an object equals this entry, following the behavior specified in
         * {@link Multiset.Entry#equals}.
         */
        @Override
        public boolean equals(@Nullable Object object) {
            if (object instanceof Multiset.Entry<?> other) {
                return count() == other.count() && Objects.equals(element(), other.element());
            }
            return false;
        }

        /** Return this entry's hash code, following the behavior specified in {@link Multiset.Entry#hashCode}. */
        @Override
        public int hashCode() {
            var e = element();
            return ((e == null) ? 0 : e.hashCode()) ^ count();
        }

        /**
         * Returns a string representation of this multiset entry. The string representation consists of the associated
         * element if the associated count is one, and otherwise the associated element followed by the characters " x "
         * (space, x and space) followed by the count. Elements and counts are converted to strings as by
         * {@code String.valueOf}.
         */
        @Override
        public String toString() {
            var text = String.valueOf(element());
            var n = count();
            return (n == 1) ? text : (text + " x " + n);
        }
    }
}
