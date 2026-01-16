package be.imgn.mtg.engine.object.internal;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.Counters;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.characteristics.internal.CounterValue;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.PermanentSource;
import be.imgn.mtg.engine.object.Player;
import be.imgn.mtg.engine.object.Token;

/// Default implementation of Permanent.
public final class DefaultPermanent extends AbstractGameObject implements Permanent {

    private final Player owner;
    private final Player controller;
    private final String name;
    private final PermanentSource source;
    private final Counters counters;
    private final @Nullable Value power;
    private final @Nullable Value toughness;
    private final @Nullable Value loyalty;
    private final Value manaValue;

    private boolean tapped;
    private boolean flipped;
    private boolean faceDown;
    private boolean phasedOut;

    private DefaultPermanent(Builder builder) {
        super(builder);
        this.owner = builder.owner;
        this.controller = builder.controller;
        this.name = builder.name;
        this.source = builder.source;
        this.counters = Counters.create();
        this.power = builder.power;
        this.toughness = builder.toughness;
        this.tapped = false;
        this.flipped = false;
        this.faceDown = false;
        this.phasedOut = false;
        // Mana value is 0 for permanents without mana cost (rule 202.3a)
        // TODO: Should derive from source's mana value when available
        this.manaValue = Value.of(0);

        // Initialize loyalty counters if loyalty is set (rule 306.5b)
        if (builder.loyalty != null) {
            this.counters.add(CounterType.LOYALTY, builder.loyalty.value());
            this.loyalty = new CounterValue(this.counters, CounterType.LOYALTY);
        } else {
            this.loyalty = null;
        }
    }

    /// Creates a permanent builder from a card entering the battlefield.
    public static Permanent.Builder fromCard(Card card, Player controller) {
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

    /// Creates a permanent builder from a token entering the battlefield.
    public static Permanent.Builder fromToken(Token token, Player controller) {
        return new Builder(token, token.owner(), controller)
                .name(token.name())
                .colors(token.colors())
                .types(token.types())
                .supertypes(token.supertypes())
                .subtypes(token.subtypes())
                .abilities(token.abilities())
                .power(token.power())
                .toughness(token.toughness())
                .loyalty(token.loyalty())
                .costs(token.costs());
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
    public PermanentSource source() {
        return source;
    }

    @Override
    public Counters counters() {
        return counters;
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

    @Override
    public boolean isTapped() {
        return tapped;
    }

    @Override
    public boolean isUntapped() {
        return !tapped;
    }

    @Override
    public boolean canTap() {
        return !tapped;
    }

    @Override
    public boolean canUntap() {
        return tapped;
    }

    @Override
    public void tap() {
        tapped = true;
    }

    @Override
    public void untap() {
        tapped = false;
    }

    @Override
    public boolean isFlipped() {
        return flipped;
    }

    @Override
    public boolean isUnflipped() {
        return !flipped;
    }

    @Override
    public boolean canFlip() {
        return !flipped;
    }

    @Override
    public boolean canUnflip() {
        return flipped;
    }

    @Override
    public void flip() {
        flipped = true;
    }

    @Override
    public void unflip() {
        flipped = false;
    }

    @Override
    public boolean isFaceUp() {
        return !faceDown;
    }

    @Override
    public boolean isFaceDown() {
        return faceDown;
    }

    @Override
    public boolean canTurnFaceUp() {
        return faceDown;
    }

    @Override
    public boolean canTurnFaceDown() {
        return !faceDown;
    }

    @Override
    public void turnFaceUp() {
        faceDown = false;
    }

    @Override
    public void turnFaceDown() {
        faceDown = true;
    }

    @Override
    public boolean isPhasedIn() {
        return !phasedOut;
    }

    @Override
    public boolean isPhasedOut() {
        return phasedOut;
    }

    @Override
    public boolean canPhaseIn() {
        return phasedOut;
    }

    @Override
    public boolean canPhaseOut() {
        return !phasedOut;
    }

    @Override
    public void phaseIn() {
        phasedOut = false;
    }

    @Override
    public void phaseOut() {
        phasedOut = true;
    }

    public static final class Builder extends AbstractGameObject.Builder<Permanent, Builder>
            implements Permanent.Builder {

        private final PermanentSource source;
        private final Player owner;
        private final Player controller;
        private String name = "";
        private @Nullable Value power;
        private @Nullable Value toughness;
        private @Nullable Value loyalty;

        Builder(PermanentSource source, Player owner, Player controller) {
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
        public Permanent build() {
            return new DefaultPermanent(this);
        }
    }
}
