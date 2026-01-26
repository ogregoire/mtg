package be.imgn.mtg.engine.util;

import java.util.Map;
import java.util.Set;

/// Abstract base class for {@link SetMultimap} implementations.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
abstract class AbstractSetMultimap<K, V> extends AbstractMultimap<K, V> implements SetMultimap<K, V> {

    @Override
    public abstract Set<V> get(K key);

    @Override
    public abstract Set<V> removeAll(K key);

    @Override
    public abstract Map<K, Set<V>> asMap();
}
