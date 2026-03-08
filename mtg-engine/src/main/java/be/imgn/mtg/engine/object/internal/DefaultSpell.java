package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.CardCopy;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.SpellSource;
import be.imgn.mtg.engine.spell.SpellContext;

/// Default implementation of Spell.
public final class DefaultSpell extends AbstractGameObject implements Spell {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final SpellSource source;
    private final SpellContext context;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private DefaultSpell(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.source = builder.source;
        this.context = builder.context;
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.loyalty = builder.loyalty;
        // Mana value is 0 for spells without mana cost (rule 202.3a)
        // TODO: Should derive from source's mana value when available
        this.manaValue = Value.of(0);
    }

    /// Creates a spell builder from a card being cast.
    public static Spell.Builder fromCard(Card card, Player controller) {
        return new Builder(card, card.owner(), controller)
                .name(card.name())
                .colors(card.colors())
                .types(card.types())
                .supertypes(card.supertypes())
                .subtypes(card.subtypes())
                .abilities(card.abilities())
                .power(card.power())
                .toughness(card.toughness())
                .loyalty(card.loyalty())
                .costs(card.costs());
    }

    /// Creates a spell builder from a card copy being cast.
    public static Spell.Builder fromCopy(CardCopy copy, Player controller) {
        return new Builder(copy, copy.owner(), controller)
                .name(copy.name())
                .colors(copy.colors())
                .types(copy.types())
                .supertypes(copy.supertypes())
                .subtypes(copy.subtypes())
                .abilities(copy.abilities())
                .power(copy.power())
                .toughness(copy.toughness())
                .loyalty(copy.loyalty())
                .costs(copy.costs());
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
    public SpellSource source() {
        return source;
    }

    @Override
    public SpellContext context() {
        return context;
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

    public static final class Builder extends AbstractGameObject.Builder<Spell, Builder> implements Spell.Builder {

        private final SpellSource source;
        private final Player owner;
        private final Player controller;
        private String name = "";
        private SpellContext context = SpellContext.empty();
        private @Nullable Value power;
        private @Nullable Value toughness;
        private @Nullable Value loyalty;

        Builder(SpellSource source, Player owner, Player controller) {
            this.source = source;
            this.owner = owner;
            this.controller = controller;
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
        public Builder context(SpellContext context) {
            this.context = context;
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
        public Spell build() {
            return new DefaultSpell(this);
        }
    }
}
