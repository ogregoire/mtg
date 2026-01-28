package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.object.Card;

/// Default implementation of Card.
public final class DefaultCard extends AbstractGameObject implements Card {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final @Nullable ManaCost manaCost;
    private final Colors colorIndicator;
    private final String rulesText;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private DefaultCard(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.manaCost = builder.manaCost;
        this.colorIndicator = builder.colorIndicatorBuilder.build();
        this.rulesText = builder.rulesText;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;

        // Mana value is 0 when there's no mana cost (rule 202.3a)
        this.manaValue = manaCost != null ? Value.of(manaCost.manaValue()) : Value.of(0);
    }

    public static Card.Builder builder() {
        return new Builder();
    }

    @Override
    public Player owner() {
        return owner;
    }

    @Override
    public Player controller() {
        return controller;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public @Nullable ManaCost manaCost() {
        return manaCost;
    }

    @Override
    public Colors colorIndicator() {
        return colorIndicator;
    }

    @Override
    public String rulesText() {
        return rulesText;
    }

    @Override
    public Value manaValue() {
        return manaValue;
    }

    @Override
    public @Nullable Value power() {
        return power;
    }

    @Override
    public @Nullable Value toughness() {
        return toughness;
    }

    @Override
    public @Nullable Value loyalty() {
        return loyalty;
    }

    @SuppressWarnings("NullAway.Init")
    public static final class Builder extends AbstractGameObject.Builder<Card, Builder> implements Card.Builder {

        private Player owner;
        private Player controller;
        private String name = "";
        private @Nullable ManaCost manaCost;
        private final Colors.Builder colorIndicatorBuilder = Colors.builder();
        private String rulesText = "";
        private @Nullable Value power;
        private @Nullable Value toughness;
        private @Nullable Value loyalty;

        Builder() {}

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public Builder owner(Player owner) {
            this.owner = owner;
            return this;
        }

        @Override
        public Builder controller(Player controller) {
            this.controller = controller;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder manaCost(ManaCost manaCost) {
            this.manaCost = manaCost;
            return this;
        }

        @Override
        public Builder colorIndicator(Colors colorIndicator) {
            this.colorIndicatorBuilder.clear().addAll(colorIndicator);
            return this;
        }

        @Override
        public Builder addColorIndicator(Color color) {
            this.colorIndicatorBuilder.add(color);
            return this;
        }

        @Override
        public Builder rulesText(String rulesText) {
            this.rulesText = rulesText;
            return this;
        }

        @Override
        public Builder power(@Nullable Value power) {
            this.power = power;
            return this;
        }

        @Override
        public Builder toughness(@Nullable Value toughness) {
            this.toughness = toughness;
            return this;
        }

        @Override
        public Builder loyalty(@Nullable Value loyalty) {
            this.loyalty = loyalty;
            return this;
        }

        @Override
        public Card build() {
            return new DefaultCard(this);
        }
    }
}
