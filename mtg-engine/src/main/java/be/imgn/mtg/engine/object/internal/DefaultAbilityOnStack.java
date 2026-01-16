package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Ability;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Player;

/// Default implementation of AbilityOnStack.
public final class DefaultAbilityOnStack extends AbstractGameObject implements AbilityOnStack {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final Ability ability;
    private final GameObject source;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private DefaultAbilityOnStack(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.ability = builder.ability;
        this.source = builder.source;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        // Abilities on the stack have no mana cost, so mana value is 0 (rule 202.3a)
        this.manaValue = Value.of(0);
    }

    /// Returns a builder for AbilityOnStack with the given ability and source.
    public static AbilityOnStack.Builder from(Ability ability, GameObject source) {
        return new Builder(ability, source);
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
    public Ability ability() {
        return ability;
    }

    @Override
    public GameObject source() {
        return source;
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

    public static final class Builder extends AbstractGameObject.Builder<AbilityOnStack, Builder>
            implements AbilityOnStack.Builder {

        private final Player owner;
        private final Player controller;
        private String name = "";
        private final Ability ability;
        private final GameObject source;
        private @Nullable Value power;
        private @Nullable Value toughness;
        private @Nullable Value loyalty;

        Builder(Ability ability, GameObject source) {
            this.ability = ability;
            this.source = source;
            this.owner = source.owner();
            this.controller = source.controller();
        }

        @Override
        protected Builder self() {
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
        public AbilityOnStack build() {
            return new DefaultAbilityOnStack(this);
        }
    }
}
