package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Token;

/// Default implementation of Token.
public final class DefaultToken extends AbstractGameObject implements Token {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private DefaultToken(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        // Tokens have no mana cost, so mana value is 0 (rule 202.3a)
        this.manaValue = Value.of(0);
    }

    public static Token.Builder builder() {
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
    public static final class Builder extends AbstractGameObject.Builder<Token, Builder> implements Token.Builder {

        private Player owner;
        private Player controller;
        private String name = "";
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
        public Token build() {
            return new DefaultToken(this);
        }
    }
}
