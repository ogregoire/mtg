package be.imgn.mtg.engine.util;

import java.util.Map;
import java.util.Set;

/// A {@link Multimap} that maps each key to a {@link Set} of values.
///
/// Unlike {@link ListMultimap}, duplicate key-value pairs are not allowed.
/// Adding a value that already exists for a key has no effect.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
public interface SetMultimap<K, V> extends Multimap<K, V> {

    /// Returns a set view of all values associated with the specified key.
    /// If the key is not present, an empty set is returned.
    ///
    /// @param key the key whose associated values are to be returned
    /// @return the set of values for the key (never null)
    @Override
    Set<V> get(K key);

    /// Removes all values associated with the specified key.
    ///
    /// @param key the key whose mappings are to be removed
    /// @return the set of removed values (possibly empty)
    @Override
    Set<V> removeAll(K key);

    /// Returns a view of this multimap as a {@link Map} from each distinct key
    /// to the set of values for that key.
    ///
    /// @return a map view of the multimap
    @Override
    Map<K, Set<V>> asMap();

    /// Creates a new empty SetMultimap backed by a HashMap for keys and HashSet for values.
    ///
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return a new empty SetMultimap
    static <K, V> SetMultimap<K, V> newHashSetMultimap() {
        return new HashSetMultimap<>();
    }

    /// Creates a new empty SetMultimap backed by a HashMap for keys and EnumSet for values.
    ///
    /// @param valueType the enum class for values
    /// @param <K> the key type
    /// @param <V> the enum value type
    /// @return a new empty SetMultimap
    static <K, V extends Enum<V>> SetMultimap<K, V> newHashEnumSetMultimap(Class<V> valueType) {
        return new HashEnumSetMultimap<>(valueType);
    }
}
