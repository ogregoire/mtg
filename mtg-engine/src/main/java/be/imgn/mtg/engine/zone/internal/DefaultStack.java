package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.StackObject;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of [Stack].
///
/// Backed by the central [ObjectStore]. Maintains a secondary deque for LIFO ordering.
public final class DefaultStack implements Stack {

    private final ObjectStore store;

    /// LIFO ordering (first = top).
    private final Deque<StackObject> deque = new ArrayDeque<>();

    /// Creates a new empty stack backed by the given store.
    ///
    /// @param store the central object store
    public DefaultStack(ObjectStore store) {
        this.store = store;
    }

    @Override
    public void push(StackObject object) {
        deque.addFirst(object);
        store.add((GameObject) object, this);
    }

    @Override
    public Optional<StackObject> peek() {
        return deque.isEmpty() ? Optional.empty() : Optional.of(deque.peekFirst());
    }

    @Override
    public Optional<StackObject> pop() {
        if (deque.isEmpty()) {
            return Optional.empty();
        }
        var object = deque.removeFirst();
        store.remove((GameObject) object);
        return Optional.of(object);
    }

    @Override
    public boolean remove(StackObject object) {
        if (deque.remove(object)) {
            store.remove((GameObject) object);
            return true;
        }
        return false;
    }

    @Override
    public List<StackObject> all() {
        return List.copyOf(deque);
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    @Override
    public boolean contains(StackObject object) {
        return deque.contains(object);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return deque.contains(object);
    }

    @Override
    public Stream<StackObject> stream() {
        return deque.stream();
    }
}
