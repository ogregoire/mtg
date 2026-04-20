package be.imgn.mtg.engine.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// An immutable [Multiset] implementation backed by arrays.
///
/// Uses a hash table array for O(1) lookups and an entry array for
/// cache-friendly iteration. All mutation operations throw
/// [UnsupportedOperationException].
///
/// @param <E> the type of elements in this multiset
final class ImmutableMultiset<E extends @Nullable Object> extends AbstractMultiset<E> {

    private static final float LOAD_FACTOR = 0.75f;

    private final ImmutableEntry<E>[] entries;
    private final ImmutableEntry<E>[] hashTable;
    private final int size;
    private final int mask;

    @SuppressWarnings("unchecked")
    ImmutableMultiset(Collection<? extends E> source) {
        // First pass: collect entries
        ImmutableEntry<E>[] tempEntries;
        int entryCount;
        long totalSize = 0;

        if (source instanceof Multiset<? extends E> multiset) {
            var sourceEntries = multiset.entrySet();
            entryCount = sourceEntries.size();
            tempEntries = new ImmutableEntry[entryCount];
            var i = 0;
            for (var entry : sourceEntries) {
                tempEntries[i++] = new ImmutableEntry<>((E) entry.element(), entry.count());
                totalSize += entry.count();
            }
        } else {
            // Build from regular collection - use temporary mutable multiset
            var temp = Multiset.<E>newHashMultiset();
            for (var element : source) {
                temp.add(element);
                totalSize++;
            }
            var sourceEntries = temp.entrySet();
            entryCount = sourceEntries.size();
            tempEntries = new ImmutableEntry[entryCount];
            var i = 0;
            for (var entry : sourceEntries) {
                tempEntries[i++] = new ImmutableEntry<>(entry.element(), entry.count());
            }
        }

        this.entries = tempEntries;
        this.size = (int) Math.min(totalSize, Integer.MAX_VALUE);

        // Build hash table with power-of-two size for fast modulo
        var tableSize = tableSizeFor(entryCount);
        this.hashTable = new ImmutableEntry[tableSize];
        this.mask = tableSize - 1;

        // Insert entries into hash table using linear probing
        for (var entry : entries) {
            var hash = hash(entry.element);
            var slot = hash & mask;
            while (hashTable[slot] != null) {
                slot = (slot + 1) & mask;
            }
            hashTable[slot] = entry;
        }
    }

    private static int tableSizeFor(int entryCount) {
        var minSize = (int) Math.ceil(entryCount / LOAD_FACTOR);
        // Round up to power of two
        var size = 1;
        while (size < minSize) {
            size <<= 1;
        }
        return Math.max(size, 4); // Minimum size of 4
    }

    private static int hash(@Nullable Object element) {
        if (element == null) {
            return 0;
        }
        // Spread bits to reduce clustering
        var h = element.hashCode();
        return h ^ (h >>> 16);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int count(@Nullable Object element) {
        var entry = findEntry(element);
        return entry != null ? entry.count : 0;
    }

    private @Nullable ImmutableEntry<E> findEntry(@Nullable Object element) {
        if (entries.length == 0) {
            return null;
        }
        var hash = hash(element);
        var slot = hash & mask;
        while (true) {
            var entry = hashTable[slot];
            if (entry == null) {
                return null;
            }
            if (Objects.equals(entry.element, element)) {
                return entry;
            }
            slot = (slot + 1) & mask;
        }
    }

    @Override
    public int add(E element, int occurrences) {
        throw new UnsupportedOperationException("Immutable multiset");
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        throw new UnsupportedOperationException("Immutable multiset");
    }

    @Override
    public int setCount(E element, int count) {
        throw new UnsupportedOperationException("Immutable multiset");
    }

    @Override
    public boolean setCount(E element, int oldCount, int newCount) {
        throw new UnsupportedOperationException("Immutable multiset");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable multiset");
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int entryIndex;
            private int remaining;

            @Override
            public boolean hasNext() {
                return remaining > 0 || entryIndex < entries.length;
            }

            @Override
            public E next() {
                if (remaining <= 0) {
                    if (entryIndex >= entries.length) {
                        throw new NoSuchElementException();
                    }
                    var entry = entries[entryIndex++];
                    remaining = entry.count;
                }
                remaining--;
                return entries[entryIndex - 1].element;
            }
        };
    }

    @Override
    Iterator<E> elementIterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < entries.length;
            }

            @Override
            public E next() {
                if (index >= entries.length) {
                    throw new NoSuchElementException();
                }
                return entries[index++].element;
            }
        };
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < entries.length;
            }

            @Override
            public Entry<E> next() {
                if (index >= entries.length) {
                    throw new NoSuchElementException();
                }
                return entries[index++];
            }
        };
    }

    @Override
    int distinctElements() {
        return entries.length;
    }

    @Override
    Set<E> createElementSet() {
        return new ElementSet();
    }

    @Override
    Set<Entry<E>> createEntrySet() {
        return new ImmutableEntrySet();
    }

    private final class ElementSet extends AbstractSet<E> {
        @Override
        public int size() {
            return entries.length;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return findEntry(o) != null;
        }

        @Override
        public Iterator<E> iterator() {
            return elementIterator();
        }

        @Override
        public boolean remove(@Nullable Object o) {
            throw new UnsupportedOperationException("Immutable multiset");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Immutable multiset");
        }
    }

    private final class ImmutableEntrySet extends EntrySet {
        @Override
        public boolean remove(@Nullable Object object) {
            throw new UnsupportedOperationException("Immutable multiset");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Immutable multiset");
        }
    }

    private static final class ImmutableEntry<E extends @Nullable Object> extends AbstractEntry<E> {
        private final E element;
        private final int count;

        ImmutableEntry(E element, int count) {
            this.element = element;
            this.count = count;
        }

        @Override
        public E element() {
            return element;
        }

        @Override
        public int count() {
            return count;
        }
    }
}
