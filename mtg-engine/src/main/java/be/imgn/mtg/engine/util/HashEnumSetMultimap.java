package be.imgn.mtg.engine.util;

import java.util.EnumSet;
import java.util.HashMap;

/// A [SetMultimap] implementation backed by a [HashMap] and [EnumSet].
///
/// This is optimized for enum values, using the efficient bit-vector implementation
/// of EnumSet for storing values.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the enum type of mapped values
final class HashEnumSetMultimap<K, V extends Enum<V>> extends AbstractMapBasedSetMultimap<K, V> {

    HashEnumSetMultimap(Class<V> valueType) {
        super(new HashMap<>(), () -> EnumSet.noneOf(valueType));
    }
}
