package be.imgn.mtg.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/// Abstract base class for map-backed {@link SetMultimap} implementations.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
abstract class AbstractMapBasedSetMultimap<K, V> extends AbstractSetMultimap<K, V> {

    private final Map<K, Set<V>> backingMap;
    private final Supplier<Set<V>> setSupplier;
    private long size;

    /// Creates a new multimap backed by the specified map.
    ///
    /// @param backingMap the backing map (must be empty)
    /// @param setSupplier supplier for creating new value sets
    /// @throws IllegalArgumentException if the backing map is not empty
    protected AbstractMapBasedSetMultimap(Map<K, Set<V>> backingMap, Supplier<Set<V>> setSupplier) {
        if (!backingMap.isEmpty()) {
            throw new IllegalArgumentException("Backing map must be empty");
        }
        this.backingMap = backingMap;
        this.setSupplier = setSupplier;
    }

    @Override
    public int size() {
        return (int) Math.min(size, Integer.MAX_VALUE);
    }

    @Override
    public Set<V> get(K key) {
        var set = backingMap.get(key);
        return set != null ? Collections.unmodifiableSet(set) : Set.of();
    }

    @Override
    public boolean put(K key, V value) {
        var set = backingMap.computeIfAbsent(key, k -> setSupplier.get());
        if (set.add(value)) {
            size++;
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(K key, V value) {
        var set = backingMap.get(key);
        if (set != null && set.remove(value)) {
            size--;
            if (set.isEmpty()) {
                backingMap.remove(key);
            }
            return true;
        }
        return false;
    }

    @Override
    public Set<V> removeAll(K key) {
        var set = backingMap.remove(key);
        if (set != null) {
            size -= set.size();
            return set;
        }
        return Set.of();
    }

    @Override
    public void clear() {
        backingMap.clear();
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        return Set.copyOf(backingMap.keySet());
    }

    @Override
    public Collection<V> values() {
        var result = new ArrayList<V>((int) Math.min(size, Integer.MAX_VALUE));
        for (var set : backingMap.values()) {
            result.addAll(set);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Map<K, Set<V>> asMap() {
        return Collections.unmodifiableMap(backingMap);
    }
}
