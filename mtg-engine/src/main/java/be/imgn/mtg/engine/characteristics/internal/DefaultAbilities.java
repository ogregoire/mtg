package be.imgn.mtg.engine.characteristics.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import be.imgn.mtg.engine.characteristics.Abilities;
import be.imgn.mtg.engine.characteristics.Ability;

/// Default implementation of Abilities.
public final class DefaultAbilities extends AbstractCharacteristics<Ability, Abilities> implements Abilities {

    private static final DefaultAbilities EMPTY = new DefaultAbilities(Set.of());

    private DefaultAbilities(Set<Ability> elements) {
        super(elements);
    }

    public static Abilities empty() {
        return EMPTY;
    }

    public static Abilities of(Ability... abilities) {
        if (abilities.length == 0) {
            return EMPTY;
        }
        var set = new LinkedHashSet<>(Arrays.asList(abilities));
        return new DefaultAbilities(Collections.unmodifiableSet(set));
    }

    public static Abilities.Builder builder() {
        return new Builder();
    }

    @Override
    public Abilities.Builder toBuilder() {
        return new Builder(elements);
    }

    static final class Builder extends AbstractCharacteristics.Builder<Ability, Abilities, Abilities.Builder>
            implements Abilities.Builder {

        Builder() {
            super(
                    new LinkedHashSet<>(),
                    () -> EMPTY,
                    set -> new DefaultAbilities(Collections.unmodifiableSet(new LinkedHashSet<>(set))));
        }

        Builder(Set<Ability> initial) {
            super(
                    new LinkedHashSet<>(initial),
                    () -> EMPTY,
                    set -> new DefaultAbilities(Collections.unmodifiableSet(new LinkedHashSet<>(set))));
        }
    }
}
