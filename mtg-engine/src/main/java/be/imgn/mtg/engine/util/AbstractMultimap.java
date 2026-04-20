package be.imgn.mtg.engine.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// Abstract base class for [Multimap] implementations.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
abstract class AbstractMultimap<K, V> implements Multimap<K, V> {

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(V value) {
        for (var entry : asMap().entrySet()) {
            if (entry.getValue().contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(K key) {
        return asMap().containsKey(key);
    }

    @Override
    public boolean put(K key, V value) {
        return get(key).add(value);
    }

    @Override
    public boolean remove(K key, V value) {
        var collection = asMap().get(key);
        return collection != null && collection.remove(value);
    }

    @Override
    public abstract void clear();

    @Override
    public abstract Set<K> keySet();

    @Override
    public abstract Collection<V> values();

    @Override
    public abstract Map<K, ? extends Collection<V>> asMap();

    // Object methods

    @Override
    public boolean equals(@Nullable Object object) {
        return this == object || object instanceof Multimap<?, ?> other && asMap().equals(other.asMap());
    }

    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

    @Override
    public String toString() {
        return asMap().toString();
    }
}
