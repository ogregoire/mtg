package be.imgn.mtg.engine.oracle.parser;

import java.util.ArrayList;
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
/// `connector` is `null` for single-element lists where no connective
/// appears (the single arm of [MtgParsers#joinedList]).
final class JoinedList<T> {
    private Mana.@Nullable Connector connector;
    private final List<T> items = new ArrayList<>();

    /// Append an element to the internal mutable list.
    JoinedList<T> add(T item) {
        items.add(item);
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
}
