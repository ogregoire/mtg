package be.imgn.mtg.engine.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/// A collection that maps keys to collections of values.
///
/// <p>Unlike a regular {@link Map}, a multimap allows multiple values to be
/// associated with a single key. The values associated with a key can be
/// accessed as a collection.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
public interface Multimap<K, V> {

    /// Returns the number of key-value pairs in this multimap.
    ///
    /// @return the total number of key-value pairs
    int size();

    /// Returns {@code true} if this multimap contains no key-value pairs.
    ///
    /// @return {@code true} if this multimap is empty
    boolean isEmpty();

    /// Returns {@code true} if this multimap contains at least one key-value pair
    /// with the specified key.
    ///
    /// @param key the key to search for
    /// @return {@code true} if this multimap contains the key
    boolean containsKey(K key);

    /// Returns {@code true} if this multimap contains at least one key-value pair
    /// with the specified value.
    ///
    /// @param value the value to search for
    /// @return {@code true} if this multimap contains the value
    boolean containsValue(V value);

    /// Returns a collection view of all values associated with the specified key.
    /// If the key is not present, an empty collection is returned.
    ///
    /// @param key the key whose associated values are to be returned
    /// @return the collection of values for the key (never null)
    Collection<V> get(K key);

    /// Associates the specified value with the specified key in this multimap.
    ///
    /// @param key the key with which the value is to be associated
    /// @param value the value to be associated with the key
    /// @return {@code true} if the multimap changed as a result
    boolean put(K key, V value);

    /// Removes a single key-value pair with the specified key and value from this
    /// multimap, if such exists.
    ///
    /// @param key the key of the entry to remove
    /// @param value the value of the entry to remove
    /// @return {@code true} if the multimap changed
    boolean remove(K key, V value);

    /// Removes all values associated with the specified key.
    ///
    /// @param key the key whose mappings are to be removed
    /// @return the collection of removed values (possibly empty)
    Collection<V> removeAll(K key);

    /// Removes all key-value pairs from the multimap.
    void clear();

    /// Returns a set view of the keys contained in this multimap.
    ///
    /// @return a set view of the keys
    Set<K> keySet();

    /// Returns a collection view of all values contained in this multimap.
    ///
    /// @return a collection view of all values
    Collection<V> values();

    /// Returns a view of this multimap as a {@link Map} from each distinct key
    /// to the collection of values for that key.
    ///
    /// @return a map view of the multimap
    Map<K, ? extends Collection<V>> asMap();
}
