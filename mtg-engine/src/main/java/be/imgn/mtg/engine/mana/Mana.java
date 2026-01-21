package be.imgn.mtg.engine.mana;

import java.util.Locale;
import java.util.Objects;

import be.imgn.mtg.engine.object.GameObject;

/// A single unit of mana in a mana pool ({@mtg.rule 106}).
///
/// Each mana has a type (colored or colorless) and a source (the object that produced it).
/// Mana may optionally have a spending restriction (see [Restricted]).
/// Snow mana is determined by whether the source has the Snow supertype.
///
/// To check if mana has a restriction, use pattern matching:
/// ```java
/// if (mana instanceof Mana.Restricted(var type, var source, var restriction)) {
///     // use restriction
/// }
/// ```
public sealed interface Mana permits Mana.Standard, Mana.Restricted {

    /// Returns the type of this mana.
    ///
    /// @return the mana type
    ManaType type();

    /// Returns the object that produced this mana.
    ///
    /// @return the source object
    GameObject source();

    /// Returns true if this is snow mana ({@mtg.rule 106.3}).
    ///
    /// Snow mana is mana produced by a snow source.
    ///
    /// @return true if snow mana
    default boolean isSnow() {
        return source().supertypes().isSnow();
    }

    /// Creates unrestricted mana of the given type.
    ///
    /// @param type the mana type
    /// @param source the object producing this mana
    /// @return the mana
    static Mana of(ManaType type, GameObject source) {
        return new Standard(type, source);
    }

    /// Creates restricted mana of the given type.
    ///
    /// @param type the mana type
    /// @param source the object producing this mana
    /// @param restriction the spending restriction
    /// @return the restricted mana
    static Mana restricted(ManaType type, GameObject source, ManaRestriction restriction) {
        return new Restricted(type, source, restriction);
    }

    /// Unrestricted mana with a type and source.
    ///
    /// @param type the mana type
    /// @param source the object that produced this mana
    record Standard(ManaType type, GameObject source) implements Mana {
        public Standard {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(source, "source");
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            if (isSnow()) sb.append("snow ");
            sb.append(type.name().toLowerCase(Locale.ROOT));
            sb.append(" mana");
            return sb.toString();
        }
    }

    /// Restricted mana with a type, source, and spending restriction.
    ///
    /// @param type the mana type
    /// @param source the object that produced this mana
    /// @param restriction the spending restriction
    record Restricted(ManaType type, GameObject source, ManaRestriction restriction) implements Mana {
        public Restricted {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(restriction, "restriction");
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            if (isSnow()) sb.append("snow ");
            sb.append(type.name().toLowerCase(Locale.ROOT));
            sb.append(" mana (restricted)");
            return sb.toString();
        }
    }
}
