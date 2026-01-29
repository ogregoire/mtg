package be.imgn.mtg.engine.object.internal;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.TypedObject;

/// Default implementation of AbilityOnStack.
public final class DefaultAbilityOnStack implements AbilityOnStack {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final Ability ability;
    private final TypedObject source;

    private DefaultAbilityOnStack(Builder builder) {
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.ability = builder.ability;
        this.source = builder.source;
    }

    /// Returns a builder for AbilityOnStack with the given ability and source.
    public static AbilityOnStack.Builder from(Ability ability, TypedObject source) {
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
    public TypedObject source() {
        return source;
    }

    public static final class Builder implements AbilityOnStack.Builder {

        private final Player owner;
        private final Player controller;
        private String name = "";
        private final Ability ability;
        private final TypedObject source;

        Builder(Ability ability, TypedObject source) {
            this.ability = ability;
            this.source = source;
            this.owner = source.owner();
            this.controller = source.controller();
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public AbilityOnStack build() {
            return new DefaultAbilityOnStack(this);
        }
    }
}
