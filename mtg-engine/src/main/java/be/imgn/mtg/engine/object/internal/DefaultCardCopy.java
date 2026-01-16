package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.CardCopy;

/// Default implementation of CardCopy.
public final class DefaultCardCopy extends AbstractGameObject implements CardCopy {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final Card original;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private DefaultCardCopy(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.original = builder.original;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        // Mana value is 0 for copies without mana cost (rule 202.3a)
        // TODO: Should derive from original's mana value when available
        this.manaValue = Value.of(0);
    }

    public static CardCopy.Builder builder() {
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
    public Card original() {
        return original;
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
    public static final class Builder extends AbstractGameObject.Builder<CardCopy, Builder>
            implements CardCopy.Builder {

        private Player owner;
        private Player controller;
        private String name = "";
        private Card original;
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
        public Builder original(Card original) {
            this.original = original;
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
        public CardCopy build() {
            return new DefaultCardCopy(this);
        }
    }
}
