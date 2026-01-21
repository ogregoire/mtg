package be.imgn.mtg.engine.object.internal;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.Costs;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.ObjectId;

/// Abstract base class for all game objects in Magic.
/// Provides common storage and getters for Characteristics (Colors, Types, etc.).
abstract class AbstractGameObject {

    protected final ObjectId id;
    protected final Colors colors;
    protected final Types types;
    protected final Supertypes supertypes;
    protected final Subtypes subtypes;
    protected final Abilities abilities;
    protected final Costs costs;

    protected AbstractGameObject(Builder<?, ?> builder) {
        this.id = new ObjectId();
        this.colors = builder.colorsBuilder.build();
        this.types = builder.typesBuilder.build();
        this.supertypes = builder.supertypesBuilder.build();
        this.subtypes = builder.subtypesBuilder.build();
        this.abilities = builder.abilitiesBuilder.build();
        this.costs = builder.costsBuilder.build();
    }

    public ObjectId id() {
        return id;
    }

    public Colors colors() {
        return colors;
    }

    public Types types() {
        return types;
    }

    public Supertypes supertypes() {
        return supertypes;
    }

    public Subtypes subtypes() {
        return subtypes;
    }

    public Abilities abilities() {
        return abilities;
    }

    public Costs costs() {
        return costs;
    }

    /// Abstract builder for all game objects.
    /// Handles only Characteristics (Colors, Types, Supertypes, Subtypes, Abilities, Costs).
    abstract static class Builder<T extends GameObject, B extends Builder<T, B>> {

        protected final Colors.Builder colorsBuilder = Colors.builder();
        protected final Types.Builder typesBuilder = Types.builder();
        protected final Supertypes.Builder supertypesBuilder = Supertypes.builder();
        protected final Subtypes.Builder subtypesBuilder = Subtypes.builder();
        protected final Abilities.Builder abilitiesBuilder = Abilities.builder();
        protected final Costs.Builder costsBuilder = Costs.builder();

        protected Builder() {}

        protected abstract B self();

        public B colors(Colors colors) {
            this.colorsBuilder.clear().addAll(colors);
            return self();
        }

        public B addColor(Color color) {
            this.colorsBuilder.add(color);
            return self();
        }

        public B color(Color color) {
            this.colorsBuilder.set(color);
            return self();
        }

        public B colors(Color... colors) {
            this.colorsBuilder.set(colors);
            return self();
        }

        public B types(Types types) {
            this.typesBuilder.clear().addAll(types);
            return self();
        }

        public B addType(Type type) {
            this.typesBuilder.add(type);
            return self();
        }

        public B type(Type type) {
            this.typesBuilder.set(type);
            return self();
        }

        public B types(Type... types) {
            this.typesBuilder.set(types);
            return self();
        }

        public B supertypes(Supertypes supertypes) {
            this.supertypesBuilder.clear().addAll(supertypes);
            return self();
        }

        public B addSupertype(Supertype supertype) {
            this.supertypesBuilder.add(supertype);
            return self();
        }

        public B supertype(Supertype supertype) {
            this.supertypesBuilder.set(supertype);
            return self();
        }

        public B supertypes(Supertype... supertypes) {
            this.supertypesBuilder.set(supertypes);
            return self();
        }

        public B subtypes(Subtypes subtypes) {
            this.subtypesBuilder.clear().addAll(subtypes);
            return self();
        }

        public B addSubtype(Subtype subtype) {
            this.subtypesBuilder.add(subtype);
            return self();
        }

        public B subtype(Subtype subtype) {
            this.subtypesBuilder.set(subtype);
            return self();
        }

        public B subtypes(Subtype... subtypes) {
            this.subtypesBuilder.set(subtypes);
            return self();
        }

        public B abilities(Abilities abilities) {
            this.abilitiesBuilder.clear().addAll(abilities);
            return self();
        }

        public B addAbility(Ability ability) {
            this.abilitiesBuilder.add(ability);
            return self();
        }

        public B ability(Ability ability) {
            this.abilitiesBuilder.set(ability);
            return self();
        }

        public B abilities(Ability... abilities) {
            this.abilitiesBuilder.set(abilities);
            return self();
        }

        public B costs(Costs costs) {
            this.costsBuilder.clear().addAll(costs);
            return self();
        }

        public B addCost(Cost cost) {
            this.costsBuilder.add(cost);
            return self();
        }

        public B cost(Cost cost) {
            this.costsBuilder.set(cost);
            return self();
        }

        public B costs(Cost... costs) {
            this.costsBuilder.set(costs);
            return self();
        }
    }
}
