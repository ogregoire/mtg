package be.imgn.mtg.engine.util;

import java.util.HashMap;

import org.jspecify.annotations.Nullable;

/// A [Multiset] implementation backed by a [HashMap].
///
/// @param <E> the type of elements in this multiset
final class HashMultiset<E extends @Nullable Object> extends AbstractMapBasedMultiset<E> {

    HashMultiset() {
        super(new HashMap<>());
    }
}
