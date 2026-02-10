package be.imgn.mtg.engine.util;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

final class Count implements Serializable {

    private int value;

    Count(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }

    public void add(int delta) {
        value += delta;
    }

    public int addAndGet(int delta) {
        return value += delta;
    }

    public void set(int newValue) {
        value = newValue;
    }

    public int getAndSet(int newValue) {
        var result = value;
        value = newValue;
        return result;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || obj instanceof Count other && value == other.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
