package be.imgn.mtg.engine.util;

import java.util.HashMap;

/// A {@link ListMultimap} implementation backed by a {@link HashMap}.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
final class HashListMultimap<K, V> extends AbstractMapBasedListMultimap<K, V> {

    HashListMultimap() {
        super(new HashMap<>());
    }
}
