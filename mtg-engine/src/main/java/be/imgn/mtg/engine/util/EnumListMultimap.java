package be.imgn.mtg.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// A {@link ListMultimap} implementation optimized for enum keys.
///
/// @param <K> the enum type of keys maintained by this multimap
/// @param <V> the type of mapped values
@SuppressWarnings("EnumOrdinal") // Intentional for performance in enum-based collections
final class EnumListMultimap<K extends Enum<K>, V> extends AbstractListMultimap<K, V> {

    private final Class<K> keyType;
    private final K[] enumConstants;

    @SuppressWarnings("unchecked")
    private final List<V>[] lists;

    private long size;

    @SuppressWarnings("unchecked")
    EnumListMultimap(Class<K> keyType) {
        this.keyType = keyType;
        this.enumConstants = keyType.getEnumConstants();
        this.lists = (List<V>[]) new List<?>[enumConstants.length];
    }

    @Override
    public int size() {
        return (int) Math.min(size, Integer.MAX_VALUE);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(K key) {
        return lists[key.ordinal()] != null;
    }

    @Override
    public List<V> get(K key) {
        var list = lists[key.ordinal()];
        return list != null ? Collections.unmodifiableList(list) : List.of();
    }

    @Override
    public @Nullable V getFirst(K key) {
        var list = lists[key.ordinal()];
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public @Nullable V getLast(K key) {
        var list = lists[key.ordinal()];
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getLast();
    }

    @Override
    public boolean put(K key, V value) {
        var index = key.ordinal();
        var list = lists[index];
        if (list == null) {
            lists[index] = list = new ArrayList<>();
        }
        list.add(value);
        size++;
        return true;
    }

    @Override
    public boolean remove(K key, V value) {
        var index = key.ordinal();
        var list = lists[index];
        if (list != null && list.remove(value)) {
            size--;
            if (list.isEmpty()) {
                lists[index] = null;
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable V removeFirst(K key) {
        var index = key.ordinal();
        var list = lists[index];
        if (list == null || list.isEmpty()) {
            return null;
        }
        var value = list.removeFirst();
        size--;
        if (list.isEmpty()) {
            lists[index] = null;
        }
        return value;
    }

    @Override
    public @Nullable V removeLast(K key) {
        var index = key.ordinal();
        var list = lists[index];
        if (list == null || list.isEmpty()) {
            return null;
        }
        var value = list.removeLast();
        size--;
        if (list.isEmpty()) {
            lists[index] = null;
        }
        return value;
    }

    @Override
    public List<V> removeAll(K key) {
        var index = key.ordinal();
        var list = lists[index];
        if (list != null) {
            lists[index] = null;
            size -= list.size();
            return list;
        }
        return List.of();
    }

    @Override
    public void clear() {
        for (var i = 0; i < lists.length; i++) {
            lists[i] = null;
        }
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        var keys = EnumSet.noneOf(keyType);
        for (var i = 0; i < lists.length; i++) {
            if (lists[i] != null) {
                keys.add(enumConstants[i]);
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Collection<V> values() {
        var result = new ArrayList<V>((int) Math.min(size, Integer.MAX_VALUE));
        for (var list : lists) {
            if (list != null) {
                result.addAll(list);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Map<K, List<V>> asMap() {
        var map = new EnumMap<K, List<V>>(keyType);
        for (var i = 0; i < lists.length; i++) {
            if (lists[i] != null) {
                map.put(enumConstants[i], Collections.unmodifiableList(lists[i]));
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
