package be.imgn.mtg.engine.oracle.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.Mana;

/// An Oxford-comma list of `T` paired with the connective that joined
/// the elements ("and", "or", or "and/or"). Output shape for
/// [MtgParsers#joinedList].
///
/// Designed as a mutable accumulator: parser arms call [#add] for each
/// element they parse and [#connector] once they see the connective
/// word. The retrievers [#items] and [#connector()] expose the result.
/// `items()` returns a `List.copyOf` — internal mutation cannot leak
/// after the wrapper has been observed.
///
/// Implements `Iterable<T>` so a `JoinedList<T>` can be passed directly
/// to [#addAll] (e.g. the [MtgParsers#joinedList] tail-merge combiner).
///
/// `connector` is `null` for single-element lists where no connective
/// appears (the single arm of [MtgParsers#joinedList]).
final class JoinedList<T> implements Iterable<T> {
    private Mana.@Nullable Connector connector;
    private final List<T> items = new ArrayList<>();

    /// Append an element to the internal mutable list.
    JoinedList<T> add(T item) {
        items.add(item);
        return this;
    }

    /// Append every element of `more` to the internal mutable list.
    JoinedList<T> addAll(Iterable<? extends T> more) {
        more.forEach(items::add);
        return this;
    }

    /// Append every element of `other` and copy `other`'s connector
    /// when set. The chainable return is `this` for fluent merges.
    JoinedList<T> merge(JoinedList<? extends T> other) {
        addAll(other);
        if (other.connector != null) connector = other.connector;
        return this;
    }

    /// Set the connective that joins the elements.
    JoinedList<T> connector(Mana.Connector connector) {
        this.connector = connector;
        return this;
    }

    /// The connective that joined the elements, or `null` if a single
    /// element was parsed with no connective.
    Mana.@Nullable Connector connector() {
        return connector;
    }

    /// Defensive immutable view of the parsed elements.
    List<T> items() {
        return List.copyOf(items);
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
