package be.imgn.mtg.engine.characteristics.internal;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/// Abstract base class for characteristic collections.
///
/// @param <T> the type of elements in this collection
/// @param <C> the concrete characteristics type
public abstract class AbstractCharacteristics<T, C extends Characteristics<T>> implements Characteristics<T> {

    protected final Set<T> elements;

    protected AbstractCharacteristics(Set<T> elements) {
        this.elements = elements;
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public int count() {
        return elements.size();
    }

    @Override
    public Stream<T> stream() {
        return elements.stream();
    }

    @Override
    public boolean contains(T element) {
        return elements.contains(element);
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof AbstractCharacteristics<?, ?> other && elements.equals(other.elements));
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    /// Abstract base class for characteristic builders.
    ///
    /// @param <T> the type of elements
    /// @param <C> the concrete characteristics type
    /// @param <B> the builder type (for fluent returns)
    public abstract static class Builder<T, C extends Characteristics<T>, B extends Characteristics.Builder<T, C, B>>
            implements Characteristics.Builder<T, C, B> {

        protected final Set<T> elements;
        private final Supplier<C> emptySupplier;
        private final Function<Set<T>, C> factory;

        protected Builder(Set<T> elements, Supplier<C> emptySupplier, Function<Set<T>, C> factory) {
            this.elements = elements;
            this.emptySupplier = emptySupplier;
            this.factory = factory;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        @Override
        public B add(T element) {
            elements.add(element);
            return self();
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addAll(T... elementsToAdd) {
            for (var element : elementsToAdd) {
                elements.add(element);
            }
            return self();
        }

        @Override
        public B addAll(C characteristics) {
            characteristics.forEach(elements::add);
            return self();
        }

        @Override
        public B set(T element) {
            elements.clear();
            elements.add(element);
            return self();
        }

        @Override
        @SuppressWarnings("unchecked")
        public B set(T... elementsToSet) {
            elements.clear();
            for (var element : elementsToSet) {
                elements.add(element);
            }
            return self();
        }

        @Override
        public B clear() {
            elements.clear();
            return self();
        }

        @Override
        public C build() {
            if (elements.isEmpty()) {
                return emptySupplier.get();
            }
            return factory.apply(elements);
        }
    }
}
