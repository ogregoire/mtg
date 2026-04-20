package be.imgn.mtg.engine.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/// A [Multimap] that maps each key to a [List] of values.
///
/// <p>The lists returned by [#get] maintain insertion order.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
public interface ListMultimap<K, V> extends Multimap<K, V> {

    /// Returns a list view of all values associated with the specified key.
    /// If the key is not present, an empty list is returned.
    ///
    /// @param key the key whose associated values are to be returned
    /// @return the list of values for the key (never null)
    @Override
    List<V> get(K key);

    /// Removes and returns the first value associated with the specified key.
    ///
    /// @param key the key whose first value is to be removed
    /// @return the removed value, or null if no values exist for the key
    @Nullable
    V removeFirst(K key);

    /// Removes and returns the last value associated with the specified key.
    ///
    /// @param key the key whose last value is to be removed
    /// @return the removed value, or null if no values exist for the key
    @Nullable
    V removeLast(K key);

    /// Returns the first value associated with the specified key without removing it.
    ///
    /// @param key the key whose first value is to be returned
    /// @return the first value, or null if no values exist for the key
    @Nullable
    V getFirst(K key);

    /// Returns the last value associated with the specified key without removing it.
    ///
    /// @param key the key whose last value is to be returned
    /// @return the last value, or null if no values exist for the key
    @Nullable
    V getLast(K key);

    /// Removes all values associated with the specified key.
    ///
    /// @param key the key whose mappings are to be removed
    /// @return the list of removed values (possibly empty)
    @Override
    List<V> removeAll(K key);

    /// Returns a view of this multimap as a [Map] from each distinct key
    /// to the list of values for that key.
    ///
    /// @return a map view of the multimap
    @Override
    Map<K, List<V>> asMap();

    /// Creates a new empty ListMultimap backed by an EnumMap for keys.
    ///
    /// @param keyType the enum class for keys
    /// @param <K> the enum key type
    /// @param <V> the value type
    /// @return a new empty ListMultimap
    static <K extends Enum<K>, V> ListMultimap<K, V> newEnumListMultimap(Class<K> keyType) {
        return new EnumListMultimap<>(keyType);
    }

    /// Creates a new empty ListMultimap backed by a HashMap for keys.
    ///
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return a new empty ListMultimap
    static <K, V> ListMultimap<K, V> newHashListMultimap() {
        return new HashListMultimap<>();
    }

    /// Creates a new empty ListMultimap backed by a TreeMap for keys.
    /// Keys are maintained in their natural ordering.
    ///
    /// @param <K> the key type (must be Comparable)
    /// @param <V> the value type
    /// @return a new empty ListMultimap with sorted keys
    static <K extends Comparable<? super K>, V> ListMultimap<K, V> newTreeListMultimap() {
        return new TreeListMultimap<>();
    }

    /// Creates a new empty ListMultimap backed by a TreeMap for keys.
    /// Keys are maintained in order according to the specified comparator.
    ///
    /// @param comparator the comparator to use for ordering keys
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return a new empty ListMultimap with sorted keys
    static <K, V> ListMultimap<K, V> newTreeListMultimap(Comparator<? super K> comparator) {
        return new TreeListMultimap<>(comparator);
    }
}
