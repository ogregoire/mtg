package be.imgn.mtg.engine.object.internal;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Emblem;

/// Default implementation of Emblem.
public final class DefaultEmblem implements Emblem {

    private final Player owner;
    private final Player controller;
    private final Abilities abilities;

    private DefaultEmblem(Builder builder) {
        this.owner = Objects.requireNonNull(builder.owner, "owner");
        this.controller = Objects.requireNonNull(builder.controller, "controller");
        this.abilities = builder.abilitiesBuilder.build();
    }

    /// Returns a new builder for Emblem.
    public static Emblem.Builder builder() {
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
    public Abilities abilities() {
        return abilities;
    }

    public static final class Builder implements Emblem.Builder {

        private @Nullable Player owner;
        private @Nullable Player controller;
        private final Abilities.Builder abilitiesBuilder = Abilities.builder();

        Builder() {}

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
        public Builder addAbility(Ability ability) {
            this.abilitiesBuilder.add(ability);
            return this;
        }

        @Override
        public Builder abilities(Abilities abilities) {
            this.abilitiesBuilder.clear().addAll(abilities);
            return this;
        }

        @Override
        public Emblem build() {
            return new DefaultEmblem(this);
        }
    }
}
