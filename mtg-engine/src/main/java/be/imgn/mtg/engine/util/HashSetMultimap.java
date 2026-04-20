package be.imgn.mtg.engine.util;

import java.util.HashMap;
import java.util.HashSet;

/// A [SetMultimap] implementation backed by a [HashMap] and [HashSet].
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
final class HashSetMultimap<K, V> extends AbstractMapBasedSetMultimap<K, V> {

    HashSetMultimap() {
        super(new HashMap<>(), HashSet::new);
    }
}
