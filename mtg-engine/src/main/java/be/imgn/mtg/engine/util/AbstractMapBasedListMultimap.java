package be.imgn.mtg.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// Abstract base class for map-backed [ListMultimap] implementations.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
abstract class AbstractMapBasedListMultimap<K, V> extends AbstractListMultimap<K, V> {

    private transient Map<K, List<V>> backingMap;
    private transient long size;

    /// Creates a new multimap backed by the specified map.
    ///
    /// @param backingMap the backing map (must be empty)
    /// @throws IllegalArgumentException if the backing map is not empty
    protected AbstractMapBasedListMultimap(Map<K, List<V>> backingMap) {
        if (!backingMap.isEmpty()) {
            throw new IllegalArgumentException("Backing map must be empty");
        }
        this.backingMap = backingMap;
    }

    @Override
    public int size() {
        return (int) Math.min(size, Integer.MAX_VALUE);
    }

    @Override
    public List<V> get(K key) {
        var list = backingMap.get(key);
        return list != null ? Collections.unmodifiableList(list) : List.of();
    }

    @Override
    public @Nullable V removeFirst(K key) {
        var list = backingMap.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        var value = list.removeFirst();
        size--;
        if (list.isEmpty()) {
            backingMap.remove(key);
        }
        return value;
    }

    @Override
    public @Nullable V removeLast(K key) {
        var list = backingMap.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        var value = list.removeLast();
        size--;
        if (list.isEmpty()) {
            backingMap.remove(key);
        }
        return value;
    }

    @Override
    public @Nullable V getFirst(K key) {
        var list = backingMap.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public @Nullable V getLast(K key) {
        var list = backingMap.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getLast();
    }

    @Override
    public boolean put(K key, V value) {
        backingMap.computeIfAbsent(key, _ -> new ArrayList<>()).add(value);
        size++;
        return true;
    }

    @Override
    public boolean remove(K key, V value) {
        var list = backingMap.get(key);
        if (list != null && list.remove(value)) {
            size--;
            if (list.isEmpty()) {
                backingMap.remove(key);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<V> removeAll(K key) {
        var list = backingMap.remove(key);
        if (list != null) {
            size -= list.size();
            return list;
        }
        return List.of();
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
        for (var list : backingMap.values()) {
            result.addAll(list);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Map<K, List<V>> asMap() {
        return Collections.unmodifiableMap(backingMap);
    }
}
