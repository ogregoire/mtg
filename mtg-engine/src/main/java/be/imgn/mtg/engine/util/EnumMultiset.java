package be.imgn.mtg.engine.util;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.ObjIntConsumer;

import org.jspecify.annotations.Nullable;

/// A {@link Multiset} implementation optimized for enum elements.
///
/// @param <E> the enum type of elements in this multiset
@SuppressWarnings("EnumOrdinal") // Intentional for performance in enum-based collections
final class EnumMultiset<E extends Enum<E>> extends AbstractMultiset<E> {

    private transient Class<E> type;
    private transient E[] enumConstants;
    private transient int[] counts;
    private transient int distinctElements;
    private transient long size;

    /** Creates an empty {@code EnumMultiset}. */
    EnumMultiset(Class<E> type) {
        this.type = type;
        if (!type.isEnum()) {
            throw new IllegalArgumentException();
        }
        this.enumConstants = type.getEnumConstants();
        this.counts = new int[enumConstants.length];
    }

    private boolean isActuallyE(@Nullable Object o) {
        if (o instanceof Enum<?> e) {
            var index = e.ordinal();
            return index < enumConstants.length && enumConstants[index] == e;
        }
        return false;
    }

    /**
     * Returns {@code element} cast to {@code E}, if it actually is a nonnull E. Otherwise, throws either a
     * NullPointerException or a ClassCastException as appropriate.
     */
    private void checkIsE(Object element) {
        requireNonNull(element);
        if (!isActuallyE(element)) {
            throw new ClassCastException("Expected an " + type + " but got " + element);
        }
    }

    @Override
    int distinctElements() {
        return distinctElements;
    }

    @Override
    public int size() {
        return Math.clamp(size, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public int count(@Nullable Object element) {
        // isActuallyE checks for null, but we check explicitly to help nullness checkers.
        if (element == null || !isActuallyE(element)) {
            return 0;
        }
        var e = (Enum<?>) element;
        return counts[e.ordinal()];
    }

    // Modification Operations
    @Override
    public int add(E element, int occurrences) {
        checkIsE(element);
        if (occurrences < 0) {
            throw new IllegalArgumentException("Negative occurrences.");
        }
        if (occurrences == 0) {
            return count(element);
        }
        var index = element.ordinal();
        var oldCount = counts[index];
        var newCount = (long) oldCount + occurrences;
        if (newCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too many occurrences: " + newCount + ".");
        }
        counts[index] = (int) newCount;
        if (oldCount == 0) {
            distinctElements++;
        }
        size += occurrences;
        return oldCount;
    }

    // Modification Operations
    @Override
    public int remove(@Nullable Object element, int occurrences) {
        // isActuallyE checks for null, but we check explicitly to help nullness checkers.
        if (element == null || !isActuallyE(element)) {
            return 0;
        }
        var e = (Enum<?>) element;
        if (occurrences < 0) {
            throw new IllegalArgumentException("Negative occurrences.");
        }
        if (occurrences == 0) {
            return count(element);
        }
        var index = e.ordinal();
        var oldCount = counts[index];
        if (oldCount == 0) {
            return 0;
        } else if (oldCount <= occurrences) {
            counts[index] = 0;
            distinctElements--;
            size -= oldCount;
        } else {
            counts[index] = oldCount - occurrences;
            size -= occurrences;
        }
        return oldCount;
    }

    // Modification Operations
    @Override
    public int setCount(E element, int count) {
        checkIsE(element);
        if (count < 0) {
            throw new IllegalArgumentException("Negative count.");
        }
        var index = element.ordinal();
        var oldCount = counts[index];
        counts[index] = count;
        size += count - oldCount;
        if (oldCount == 0 && count > 0) {
            distinctElements++;
        } else if (oldCount > 0 && count == 0) {
            distinctElements--;
        }
        return oldCount;
    }

    @Override
    public void clear() {
        Arrays.fill(counts, 0);
        size = 0;
        distinctElements = 0;
    }

    abstract class Itr<T> implements Iterator<T> {
        int index = 0;
        int toRemove = -1;

        abstract T output(int index);

        @Override
        public boolean hasNext() {
            for (; index < enumConstants.length; index++) {
                if (counts[index] > 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = output(index);
            toRemove = index;
            index++;
            return result;
        }

        @Override
        public void remove() {
            if (toRemove < 0) {
                throw new IllegalStateException("no calls to next() since the last call to remove()");
            }
            if (counts[toRemove] > 0) {
                distinctElements--;
                size -= counts[toRemove];
                counts[toRemove] = 0;
            }
            toRemove = -1;
        }
    }

    @Override
    Iterator<E> elementIterator() {
        return new Itr<E>() {
            @Override
            E output(int index) {
                return enumConstants[index];
            }
        };
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
        return new Itr<Entry<E>>() {
            @Override
            Entry<E> output(int index) {
                return new AbstractEntry<E>() {
                    @Override
                    public E element() {
                        return enumConstants[index];
                    }

                    @Override
                    public int count() {
                        return counts[index];
                    }
                };
            }
        };
    }

    @Override
    public void forEachEntry(ObjIntConsumer<? super E> action) {
        requireNonNull(action);
        for (var i = 0; i < enumConstants.length; i++) {
            if (counts[i] > 0) {
                action.accept(enumConstants[i], counts[i]);
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new MultisetIterator();
    }

    final class MultisetIterator implements Iterator<E> {
        private final Iterator<Entry<E>> entryIterator;
        private @Nullable Entry<E> currentEntry;

        /** Count of subsequent elements equal to current element */
        private int laterCount;

        /** Count of all elements equal to current element */
        private int totalCount;

        private boolean canRemove;

        MultisetIterator() {
            this.entryIterator = entryIterator();
        }

        @Override
        public boolean hasNext() {
            return laterCount > 0 || entryIterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (laterCount == 0) {
                currentEntry = entryIterator.next();
                totalCount = laterCount = currentEntry.count();
            }
            laterCount--;
            canRemove = true;
            /*
             * requireNonNull is safe because laterCount starts at 0, forcing us to initialize
             * currentEntry above. After that, we never clear it.
             */
            return requireNonNull(currentEntry).element();
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("no calls to next() since the last call to remove()");
            }
            if (totalCount == 1) {
                entryIterator.remove();
            } else {
                /*
                 * requireNonNull is safe because canRemove is set to true only after we initialize
                 * currentEntry (which we never subsequently clear).
                 */
                EnumMultiset.this.remove(requireNonNull(currentEntry).element());
            }
            totalCount--;
            canRemove = false;
        }
    }
}
