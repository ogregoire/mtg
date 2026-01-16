package be.imgn.mtg.engine.util;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/// Abstract base class for {@link ListMultimap} implementations.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
abstract class AbstractListMultimap<K, V> extends AbstractMultimap<K, V> implements ListMultimap<K, V> {

    @Override
    public abstract List<V> get(K key);

    @Override
    public abstract @Nullable V removeFirst(K key);

    @Override
    public abstract @Nullable V removeLast(K key);

    @Override
    public abstract @Nullable V getFirst(K key);

    @Override
    public abstract @Nullable V getLast(K key);

    @Override
    public abstract List<V> removeAll(K key);

    @Override
    public abstract Map<K, List<V>> asMap();
}
