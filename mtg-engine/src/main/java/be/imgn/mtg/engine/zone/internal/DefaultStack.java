package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.StackObject;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of [Stack].
///
/// Uses a deque for LIFO order with index 0 being the top.
public final class DefaultStack implements Stack {

    /// LIFO stack of objects (first = top).
    private final Deque<StackObject> stack = new ArrayDeque<>();

    /// Set for fast lookup.
    private final Set<StackObject> objects = new HashSet<>();

    /// Creates a new empty stack.
    public DefaultStack() {}

    @Override
    public void push(StackObject object) {
        stack.addFirst(object);
        objects.add(object);
    }

    @Override
    public Optional<StackObject> peek() {
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peekFirst());
    }

    @Override
    public Optional<StackObject> pop() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        var object = stack.removeFirst();
        objects.remove(object);
        return Optional.of(object);
    }

    @Override
    public boolean remove(StackObject object) {
        if (objects.remove(object)) {
            stack.remove(object);
            return true;
        }
        return false;
    }

    @Override
    public List<StackObject> all() {
        return List.copyOf(stack);
    }

    @Override
    public int size() {
        return stack.size();
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public boolean contains(StackObject object) {
        return objects.contains(object);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return objects.contains(object);
    }

    @Override
    public Stream<StackObject> stream() {
        return stack.stream();
    }
}
