package be.imgn.mtg.engine.card;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/// The static, out-of-game definition of a Magic card.
///
/// A card definition represents the "template" for what a card is, as loaded from a
/// database. This is separate from the in-game card object which has owner, controller,
/// and mutable game state. Identity is based on [#oracleId()] only.
public final class CardDefinition {

    private final UUID oracleId;
    private final String name;
    private final CardLayout layout;
    private final @Nullable String manaCost;
    private final @Nullable String oracleText;
    private final @Nullable String typeLine;
    private final @Nullable String power;
    private final @Nullable String toughness;
    private final @Nullable String loyalty;
    private final @Nullable String defense;
    private final int manaValue;
    private final Set<String> colors;
    private final Set<String> colorIdentity;
    private final Set<String> colorIndicator;
    private final @Nullable FaceDefinition primaryFace;
    private final @Nullable FaceDefinition secondaryFace;

    private CardDefinition(Builder builder) {
        this.oracleId = builder.oracleId;
        this.name = builder.name;
        this.layout = builder.layout;
        this.manaCost = builder.manaCost;
        this.oracleText = builder.oracleText;
        this.typeLine = builder.typeLine;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        this.defense = builder.defense;
        this.manaValue = builder.manaValue;
        this.colors = Set.copyOf(builder.colors);
        this.colorIdentity = Set.copyOf(builder.colorIdentity);
        this.colorIndicator = Set.copyOf(builder.colorIndicator);
        this.primaryFace = builder.primaryFaceBuilder != null ? builder.primaryFaceBuilder.build() : null;
        this.secondaryFace = builder.secondaryFaceBuilder != null ? builder.secondaryFaceBuilder.build() : null;
    }

    /// Returns the Oracle ID that uniquely identifies this card definition.
    ///
    /// @return the Oracle ID
    public UUID oracleId() {
        return oracleId;
    }

    /// Returns the name of this card.
    ///
    /// @return the card name
    public String name() {
        return name;
    }

    /// Returns the layout of this card.
    ///
    /// @return the card layout
    public CardLayout layout() {
        return layout;
    }

    /// Returns the mana cost of this card, if any.
    ///
    /// @return the mana cost
    public Optional<String> manaCost() {
        return Optional.ofNullable(manaCost);
    }

    /// Returns the oracle text of this card, if any.
    ///
    /// @return the oracle text
    public Optional<String> oracleText() {
        return Optional.ofNullable(oracleText);
    }

    /// Returns the type line of this card, if any.
    ///
    /// @return the type line
    public Optional<String> typeLine() {
        return Optional.ofNullable(typeLine);
    }

    /// Returns the power of this card, if any.
    ///
    /// @return the power
    public Optional<String> power() {
        return Optional.ofNullable(power);
    }

    /// Returns the toughness of this card, if any.
    ///
    /// @return the toughness
    public Optional<String> toughness() {
        return Optional.ofNullable(toughness);
    }

    /// Returns the loyalty of this card, if any.
    ///
    /// @return the loyalty
    public Optional<String> loyalty() {
        return Optional.ofNullable(loyalty);
    }

    /// Returns the defense of this card, if any.
    ///
    /// @return the defense
    public Optional<String> defense() {
        return Optional.ofNullable(defense);
    }

    /// Returns the mana value of this card.
    ///
    /// @return the mana value
    public int manaValue() {
        return manaValue;
    }

    /// Returns the colors of this card.
    ///
    /// @return an unmodifiable set of colors
    public Set<String> colors() {
        return colors;
    }

    /// Returns the color identity of this card.
    ///
    /// @return an unmodifiable set of color identity values
    public Set<String> colorIdentity() {
        return colorIdentity;
    }

    /// Returns the color indicator of this card.
    ///
    /// @return an unmodifiable set of color indicator values
    public Set<String> colorIndicator() {
        return colorIndicator;
    }

    /// Returns the primary face of this double-faced card, if any.
    ///
    /// @return the primary face definition
    public Optional<FaceDefinition> primaryFace() {
        return Optional.ofNullable(primaryFace);
    }

    /// Returns the secondary face of this double-faced card, if any.
    ///
    /// @return the secondary face definition
    public Optional<FaceDefinition> secondaryFace() {
        return Optional.ofNullable(secondaryFace);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof CardDefinition other && oracleId.equals(other.oracleId));
    }

    @Override
    public int hashCode() {
        return oracleId.hashCode();
    }

    /// Creates a new builder for a card definition.
    ///
    /// @param oracleId the Oracle ID
    /// @param name     the card name
    /// @param layout   the card layout
    /// @return a new builder
    public static Builder builder(UUID oracleId, String name, CardLayout layout) {
        return new Builder(oracleId, name, layout);
    }

    /// Builder for [CardDefinition] instances.
    public static final class Builder {

        private final UUID oracleId;
        private final String name;
        private final CardLayout layout;
        private @Nullable String manaCost;
        private @Nullable String oracleText;
        private @Nullable String typeLine;
        private @Nullable String power;
        private @Nullable String toughness;
        private @Nullable String loyalty;
        private @Nullable String defense;
        private int manaValue;
        private Set<String> colors = Set.of();
        private Set<String> colorIdentity = Set.of();
        private Set<String> colorIndicator = Set.of();

        FaceDefinition.@Nullable Builder primaryFaceBuilder;

        FaceDefinition.@Nullable Builder secondaryFaceBuilder;

        private Builder(UUID oracleId, String name, CardLayout layout) {
            this.oracleId = oracleId;
            this.name = name;
            this.layout = layout;
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

        /// Sets the mana value.
        ///
        /// @param manaValue the mana value
        /// @return this builder
        public Builder manaValue(int manaValue) {
            this.manaValue = manaValue;
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

        /// Sets the color identity.
        ///
        /// @param colorIdentity the set of color identity values
        /// @return this builder
        public Builder colorIdentity(Set<String> colorIdentity) {
            this.colorIdentity = colorIdentity;
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

        /// Starts building the primary face of a double-faced card.
        ///
        /// @param name the face name
        /// @return a new face builder
        /// @throws IllegalStateException if the layout is not double-faced
        public FaceDefinition.Builder primaryFace(String name) {
            if (!layout.isDoubleFaced()) {
                throw new IllegalStateException("Cannot add faces to non-double-faced layout: " + layout);
            }
            this.primaryFaceBuilder = new FaceDefinition.Builder(name, this);
            return this.primaryFaceBuilder;
        }

        /// Starts building the secondary face of a double-faced card.
        ///
        /// @param name the face name
        /// @return a new face builder
        /// @throws IllegalStateException if the layout is not double-faced
        public FaceDefinition.Builder secondaryFace(String name) {
            if (!layout.isDoubleFaced()) {
                throw new IllegalStateException("Cannot add faces to non-double-faced layout: " + layout);
            }
            this.secondaryFaceBuilder = new FaceDefinition.Builder(name, this);
            return this.secondaryFaceBuilder;
        }

        /// Builds the card definition.
        ///
        /// @return the card definition
        /// @throws IllegalStateException if a DFC layout is missing one or both faces
        public CardDefinition build() {
            if (layout.isDoubleFaced() && (primaryFaceBuilder == null || secondaryFaceBuilder == null)) {
                throw new IllegalStateException(
                        "Double-faced layout " + layout + " requires both primary and secondary faces");
            }

            return new CardDefinition(this);
        }
    }
}
