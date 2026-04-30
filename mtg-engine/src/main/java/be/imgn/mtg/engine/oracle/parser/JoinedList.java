package be.imgn.mtg.engine.oracle.parser;

import java.util.List;

import be.imgn.mtg.engine.oracle.domain.Mana;

/// An Oxford-comma list of `T` paired with the connective that joined
/// the elements ("and", "or", or "and/or"). Output shape for
/// [MtgParsers#joinedList]: callers that need to branch on the
/// connective use `connector()`; callers that only want the items
/// drop to `items()`.
///
/// The compact constructor defensively copies `items` so the wrapper
/// is immutable; an internally-mutable accumulator can build the list
/// during parsing and pass it in once for the copy.
record JoinedList<T>(Mana.Connector connector, List<T> items) {
    JoinedList {
        items = List.copyOf(items);
    }
}
