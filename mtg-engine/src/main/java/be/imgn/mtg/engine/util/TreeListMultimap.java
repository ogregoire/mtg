package be.imgn.mtg.engine.util;

import java.util.Comparator;
import java.util.TreeMap;

/// A [ListMultimap] implementation backed by a [TreeMap].
///
/// Keys are maintained in sorted order according to their natural ordering
/// or by a [Comparator] provided at creation time.
///
/// @param <K> the type of keys maintained by this multimap
/// @param <V> the type of mapped values
final class TreeListMultimap<K, V> extends AbstractMapBasedListMultimap<K, V> {

    TreeListMultimap() {
        super(new TreeMap<>());
    }

    TreeListMultimap(Comparator<? super K> comparator) {
        super(new TreeMap<>(comparator));
    }
}
