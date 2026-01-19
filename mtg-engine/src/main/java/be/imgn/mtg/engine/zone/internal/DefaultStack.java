package be.imgn.mtg.engine.zone.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.StackObject;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of [Stack].
///
/// Uses a deque for LIFO order with index 0 being the top.
public final class DefaultStack implements Stack {

    /// LIFO stack of objects (first = top).
    private final Deque<StackObject> stack = new ArrayDeque<>();

    /// Index for fast lookup by ID.
    private final Map<ObjectId, StackObject> objectsById = new HashMap<>();

    /// Creates a new empty stack.
    public DefaultStack() {}

    @Override
    public void push(StackObject object) {
        stack.addFirst(object);
        objectsById.put(idOf(object), object);
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
        objectsById.remove(idOf(object));
        return Optional.of(object);
    }

    private static ObjectId idOf(StackObject object) {
        return switch (object) {
            case Spell spell -> spell.id();
            case AbilityOnStack ability -> ability.id();
        };
    }

    @Override
    public Optional<StackObject> remove(ObjectId id) {
        var object = objectsById.remove(id);
        if (object != null) {
            stack.remove(object);
        }
        return Optional.ofNullable(object);
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
    public boolean contains(ObjectId id) {
        return objectsById.containsKey(id);
    }

    @Override
    public Optional<StackObject> findById(ObjectId id) {
        return Optional.ofNullable(objectsById.get(id));
    }

    @Override
    public Stream<StackObject> stream() {
        return stack.stream();
    }
}
