package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;

/// Default implementation of Colors.
public final class DefaultColors extends AbstractCharacteristics<Color, Colors> implements Colors {

    private static final DefaultColors EMPTY = new DefaultColors(EnumSet.noneOf(Color.class));

    private DefaultColors(Set<Color> elements) {
        super(elements);
    }

    public static Colors empty() {
        return EMPTY;
    }

    public static Colors of(Color... colors) {
        if (colors.length == 0) {
            return EMPTY;
        }
        var set = EnumSet.noneOf(Color.class);
        set.addAll(Arrays.asList(colors));
        return new DefaultColors(set);
    }

    public static Colors.Builder builder() {
        return new Builder();
    }

    /// Returns an optimized Collector for Colors using EnumSet.
    public static Collector<Color, ?, Colors> collector() {
        return Collector.of(
                () -> EnumSet.noneOf(Color.class),
                Set::add,
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                },
                set -> set.isEmpty() ? EMPTY : new DefaultColors(EnumSet.copyOf(set)),
                Collector.Characteristics.UNORDERED);
    }

    @Override
    public Colors.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Color, Colors, Colors.Builder>
            implements Colors.Builder {

        Builder() {
            super(EnumSet.noneOf(Color.class), () -> EMPTY, set -> new DefaultColors(EnumSet.copyOf(set)));
        }

        Builder(Set<Color> initial) {
            super(EnumSet.copyOf(initial), () -> EMPTY, set -> new DefaultColors(EnumSet.copyOf(set)));
        }
    }
}
