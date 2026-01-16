package be.imgn.mtg.engine.util;

import static java.util.Objects.requireNonNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import org.jspecify.annotations.Nullable;

abstract class AbstractMapBasedMultiset<E extends @Nullable Object> extends AbstractMultiset<E> {
    private transient Map<E, Count> backingMap;

    /*
     * Cache the size for efficiency. Using a long lets us avoid the need for
     * overflow checking and ensures that size() will function correctly even if
     * the multiset had once been larger than Integer.MAX_VALUE.
     */
    private transient long size;

    /** Standard constructor. */
    protected AbstractMapBasedMultiset(Map<E, Count> backingMap) {
        if (!backingMap.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.backingMap = backingMap;
    }

    /** Used during deserialization only. The backing map must be empty. */
    void setBackingMap(Map<E, Count> backingMap) {
        this.backingMap = backingMap;
    }

    // Required Implementations

    /**
     * {@inheritDoc}
     *
     * <p>Invoking {@link Multiset.Entry#count} on an entry in the returned set always returns the current count of that
     * element in the multiset, as opposed to the count at the time the entry was retrieved.
     */
    @Override
    public Set<Entry<E>> entrySet() {
        return super.entrySet();
    }

    @Override
    Iterator<E> elementIterator() {
        var backingEntries = backingMap.entrySet().iterator();
        return new Iterator<>() {

            Map.@Nullable Entry<E, Count> toRemove;

            @Override
            public boolean hasNext() {
                return backingEntries.hasNext();
            }

            @Override
            public E next() {
                var mapEntry = backingEntries.next();
                toRemove = mapEntry;
                return mapEntry.getKey();
            }

            @Override
            public void remove() {
                if (toRemove == null) {
                    throw new IllegalStateException("no calls to next() since the last call to remove()");
                }
                size -= toRemove.getValue().getAndSet(0);
                backingEntries.remove();
                toRemove = null;
            }
        };
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
        var backingEntries = backingMap.entrySet().iterator();
        return new Iterator<>() {
            Map.@Nullable Entry<E, Count> toRemove;

            @Override
            public boolean hasNext() {
                return backingEntries.hasNext();
            }

            @Override
            public Multiset.Entry<E> next() {
                var mapEntry = backingEntries.next();
                toRemove = mapEntry;
                return new AbstractEntry<>() {
                    @Override
                    public E element() {
                        return mapEntry.getKey();
                    }

                    @Override
                    public int count() {
                        var count = mapEntry.getValue();
                        if (count == null || count.get() == 0) {
                            var frequency = backingMap.get(element());
                            if (frequency != null) {
                                return frequency.get();
                            }
                        }
                        return (count == null) ? 0 : count.get();
                    }
                };
            }

            @Override
            public void remove() {
                if (toRemove == null) {
                    throw new IllegalStateException("no calls to next() since the last call to remove()");
                }
                size -= toRemove.getValue().getAndSet(0);
                backingEntries.remove();
                toRemove = null;
            }
        };
    }

    @Override
    public void forEachEntry(ObjIntConsumer<? super E> action) {
        requireNonNull(action);
        backingMap.forEach((element, count) -> action.accept(element, count.get()));
    }

    @Override
    public void clear() {
        for (var frequency : backingMap.values()) {
            frequency.set(0);
        }
        backingMap.clear();
        size = 0L;
    }

    @Override
    int distinctElements() {
        return backingMap.size();
    }

    // Optimizations - Query Operations

    @Override
    public int size() {
        return Math.clamp(size, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public Iterator<E> iterator() {
        return new MapBasedMultisetIterator();
    }

    /*
     * Not subclassing AbstractMultiset$MultisetIterator because next() needs to
     * retrieve the Map.Entry<E, Count> entry, which can then be used for
     * a more efficient remove() call.
     */
    private final class MapBasedMultisetIterator implements Iterator<E> {
        final Iterator<Map.Entry<E, Count>> entryIterator;
        Map.@Nullable Entry<E, Count> currentEntry;
        int occurrencesLeft;
        boolean canRemove;

        MapBasedMultisetIterator() {
            this.entryIterator = backingMap.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return occurrencesLeft > 0 || entryIterator.hasNext();
        }

        @Override
        public E next() {
            if (occurrencesLeft == 0) {
                currentEntry = entryIterator.next();
                occurrencesLeft = currentEntry.getValue().get();
            }
            occurrencesLeft--;
            canRemove = true;
            /*
             * requireNonNull is safe because occurrencesLeft starts at 0, forcing us to initialize
             * currentEntry above. After that, we never clear it.
             */
            return requireNonNull(currentEntry).getKey();
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("no calls to next() since the last call to remove()");
            }
            /*
             * requireNonNull is safe because canRemove is set to true only after we initialize
             * currentEntry (which we never subsequently clear).
             */
            var frequency = requireNonNull(currentEntry).getValue().get();
            if (frequency <= 0) {
                throw new ConcurrentModificationException();
            }
            if (currentEntry.getValue().addAndGet(-1) == 0) {
                entryIterator.remove();
            }
            size--;
            canRemove = false;
        }
    }

    @Override
    public int count(@Nullable Object element) {
        try {
            var frequency = backingMap.get(element);
            return frequency != null ? frequency.get() : 0;
        } catch (ClassCastException | NullPointerException e) {
            return 0;
        }
    }

    // Optional Operations - Modification Operations

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the call would result in more than {@link Integer#MAX_VALUE} occurrences of
     *     {@code element} in this multiset.
     */
    @Override
    public int add(E element, int occurrences) {
        if (occurrences == 0) {
            return count(element);
        }
        if (occurrences < 0) {
            throw new IllegalArgumentException("occurrences cannot be negative: " + occurrences + ".");
        }
        var frequency = backingMap.get(element);
        int oldCount;
        if (frequency == null) {
            oldCount = 0;
            backingMap.put(element, new Count(occurrences));
        } else {
            oldCount = frequency.get();
            var newCount = (long) oldCount + (long) occurrences;
            if (newCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("too many occurrences: " + newCount + ".");
            }
            frequency.add(occurrences);
        }
        size += occurrences;
        return oldCount;
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        if (occurrences == 0) {
            return count(element);
        }
        if (occurrences < 0) {
            throw new IllegalArgumentException("occurrences cannot be negative: " + occurrences + ".");
        }
        var frequency = backingMap.get(element);
        if (frequency == null) {
            return 0;
        }

        var oldCount = frequency.get();

        int numberRemoved;
        if (oldCount > occurrences) {
            numberRemoved = occurrences;
        } else {
            numberRemoved = oldCount;
            backingMap.remove(element);
        }

        frequency.add(-numberRemoved);
        size -= numberRemoved;
        return oldCount;
    }

    // Roughly a 33% performance improvement over AbstractMultiset.setCount().
    @Override
    public int setCount(E element, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count.");
        }

        Count existingCounter;
        int oldCount;
        if (count == 0) {
            existingCounter = backingMap.remove(element);
            oldCount = getAndSet(existingCounter, count);
        } else {
            existingCounter = backingMap.get(element);
            oldCount = getAndSet(existingCounter, count);

            if (existingCounter == null) {
                backingMap.put(element, new Count(count));
            }
        }

        size += count - oldCount;
        return oldCount;
    }

    private static int getAndSet(@Nullable Count i, int count) {
        if (i == null) {
            return 0;
        }

        return i.getAndSet(count);
    }
}
