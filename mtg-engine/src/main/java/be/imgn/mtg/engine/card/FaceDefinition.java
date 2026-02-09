package be.imgn.mtg.engine.card;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/// The definition of a single face of a Magic card.
///
/// Face definitions are used for double-faced cards where each face has its own
/// characteristics (name, mana cost, type line, etc.). A face can only be created
/// through the [CardDefinition.Builder] fluent API.
public final class FaceDefinition {

    private final String name;
    private final @Nullable String manaCost;
    private final @Nullable String oracleText;
    private final @Nullable String typeLine;
    private final @Nullable String power;
    private final @Nullable String toughness;
    private final @Nullable String loyalty;
    private final @Nullable String defense;
    private final Set<String> colors;
    private final Set<String> colorIndicator;
    private final int manaValue;

    private FaceDefinition(Builder builder) {
        this.name = builder.name;
        this.manaCost = builder.manaCost;
        this.oracleText = builder.oracleText;
        this.typeLine = builder.typeLine;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        this.defense = builder.defense;
        this.colors = Set.copyOf(builder.colors);
        this.colorIndicator = Set.copyOf(builder.colorIndicator);
        this.manaValue = builder.manaValue;
    }

    /// Returns the name of this face.
    ///
    /// @return the face name
    public String name() {
        return name;
    }

    /// Returns the mana cost of this face, if any.
    ///
    /// @return the mana cost
    public Optional<String> manaCost() {
        return manaCost != null ? Optional.of(manaCost) : Optional.empty();
    }

    /// Returns the oracle text of this face, if any.
    ///
    /// @return the oracle text
    public Optional<String> oracleText() {
        return oracleText != null ? Optional.of(oracleText) : Optional.empty();
    }

    /// Returns the type line of this face, if any.
    ///
    /// @return the type line
    public Optional<String> typeLine() {
        return typeLine != null ? Optional.of(typeLine) : Optional.empty();
    }

    /// Returns the power of this face, if any.
    ///
    /// @return the power
    public Optional<String> power() {
        return power != null ? Optional.of(power) : Optional.empty();
    }

    /// Returns the toughness of this face, if any.
    ///
    /// @return the toughness
    public Optional<String> toughness() {
        return toughness != null ? Optional.of(toughness) : Optional.empty();
    }

    /// Returns the loyalty of this face, if any.
    ///
    /// @return the loyalty
    public Optional<String> loyalty() {
        return loyalty != null ? Optional.of(loyalty) : Optional.empty();
    }

    /// Returns the defense of this face, if any.
    ///
    /// @return the defense
    public Optional<String> defense() {
        return defense != null ? Optional.of(defense) : Optional.empty();
    }

    /// Returns the colors of this face.
    ///
    /// @return an unmodifiable set of colors
    public Set<String> colors() {
        return colors;
    }

    /// Returns the color indicator of this face.
    ///
    /// @return an unmodifiable set of color indicator values
    public Set<String> colorIndicator() {
        return colorIndicator;
    }

    /// Returns the mana value of this face.
    ///
    /// @return the mana value
    public int manaValue() {
        return manaValue;
    }

    /// Builder for [FaceDefinition] instances.
    ///
    /// Face builders are created through [CardDefinition.Builder#primaryFace(String)]
    /// and [CardDefinition.Builder#secondaryFace(String)]. Use [#endFace()] to return
    /// to the parent card builder.
    public static final class Builder {

        private final String name;
        private final CardDefinition.Builder parentBuilder;
        private @Nullable String manaCost;
        private @Nullable String oracleText;
        private @Nullable String typeLine;
        private @Nullable String power;
        private @Nullable String toughness;
        private @Nullable String loyalty;
        private @Nullable String defense;
        private Set<String> colors = Set.of();
        private Set<String> colorIndicator = Set.of();
        private int manaValue;

        Builder(String name, CardDefinition.Builder parentBuilder) {
            this.name = name;
            this.parentBuilder = parentBuilder;
        }

        /// Sets the mana cost.
        ///
        /// @param manaCost the mana cost string
        /// @return this builder
        public Builder manaCost(String manaCost) {
            this.manaCost = manaCost;
            return this;
        }

        /// Sets the oracle text.
        ///
        /// @param oracleText the oracle text
        /// @return this builder
        public Builder oracleText(String oracleText) {
            this.oracleText = oracleText;
            return this;
        }

        /// Sets the type line.
        ///
        /// @param typeLine the type line
        /// @return this builder
        public Builder typeLine(String typeLine) {
            this.typeLine = typeLine;
            return this;
        }

        /// Sets the power.
        ///
        /// @param power the power value
        /// @return this builder
        public Builder power(String power) {
            this.power = power;
            return this;
        }

        /// Sets the toughness.
        ///
        /// @param toughness the toughness value
        /// @return this builder
        public Builder toughness(String toughness) {
            this.toughness = toughness;
            return this;
        }

        /// Sets the loyalty.
        ///
        /// @param loyalty the loyalty value
        /// @return this builder
        public Builder loyalty(String loyalty) {
            this.loyalty = loyalty;
            return this;
        }

        /// Sets the defense.
        ///
        /// @param defense the defense value
        /// @return this builder
        public Builder defense(String defense) {
            this.defense = defense;
            return this;
        }

        /// Sets the colors.
        ///
        /// @param colors the set of colors
        /// @return this builder
        public Builder colors(Set<String> colors) {
            this.colors = colors;
            return this;
        }

        /// Sets the color indicator.
        ///
        /// @param colorIndicator the set of color indicator values
        /// @return this builder
        public Builder colorIndicator(Set<String> colorIndicator) {
            this.colorIndicator = colorIndicator;
            return this;
        }

        /// Sets the mana value.
        ///
        /// @param manaValue the mana value
        /// @return this builder
        public Builder manaValue(int manaValue) {
            this.manaValue = manaValue;
            return this;
        }

        /// Finishes configuring this face and returns to the parent card builder.
        ///
        /// @return the parent [CardDefinition.Builder]
        public CardDefinition.Builder endFace() {
            return parentBuilder;
        }

        FaceDefinition build() {
            return new FaceDefinition(this);
        }
    }
}
