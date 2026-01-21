package be.imgn.mtg.engine.game.internal;

import be.imgn.mtg.engine.characteristics.Cost;
import be.imgn.mtg.engine.characteristics.CostContext;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerData;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.mana.PaymentResult;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

/// Default implementation of [Player].
class PlayerImpl implements Player {

    private static final int DEFAULT_STARTING_LIFE = 20;

    private final PlayerData playerData;
    private final Library library;
    private final Hand hand;
    private final Graveyard graveyard;
    private final ManaPool manaPool;

    private int lifeTotal;

    PlayerImpl(PlayerData playerData, Library library, Hand hand, Graveyard graveyard, ManaPool manaPool) {
        this.playerData = playerData;
        this.library = library;
        this.hand = hand;
        this.graveyard = graveyard;
        this.manaPool = manaPool;
        this.lifeTotal = DEFAULT_STARTING_LIFE;
    }

    @Override
    public PlayerData data() {
        return playerData;
    }

    @Override
    public Library library() {
        return library;
    }

    @Override
    public Hand hand() {
        return hand;
    }

    @Override
    public Graveyard graveyard() {
        return graveyard;
    }

    @Override
    public ManaPool manaPool() {
        return manaPool;
    }

    @Override
    public int lifeTotal() {
        return lifeTotal;
    }

    @Override
    public void gainLife(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Life gain amount must be positive: " + amount);
        }
        lifeTotal += amount;
    }

    @Override
    public void loseLife(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Life loss amount must be positive: " + amount);
        }
        lifeTotal -= amount;
    }

    @Override
    public void setLifeTotal(int amount) {
        lifeTotal = amount;
    }

    @Override
    public boolean canPay(Cost cost) {
        // ManaCost requires CostContext; use canPay(ManaCost, CostContext) instead
        // Other cost types are not yet supported
        throw new UnsupportedOperationException(
                "Unsupported cost type: " + cost.getClass() + "; ManaCost requires CostContext");
    }

    @Override
    public void pay(Cost cost) {
        // ManaCost requires CostContext; use pay(ManaCost, CostContext) instead
        // Other cost types are not yet supported
        throw new UnsupportedOperationException(
                "Unsupported cost type: " + cost.getClass() + "; ManaCost requires CostContext");
    }

    @Override
    public boolean canPay(ManaCost cost, CostContext context) {
        if (cost.isEmpty()) {
            return true;
        }
        return manaPool.canPayFully(cost, context);
    }

    @Override
    public void pay(ManaCost cost, CostContext context) {
        if (cost.isEmpty()) {
            return;
        }
        var result = manaPool.payFully(cost, context);
        switch (result) {
            case PaymentResult.Success success -> {
                if (success.lifePaid() > 0) {
                    loseLife(success.lifePaid());
                }
            }
            case PaymentResult.InsufficientMana insufficient ->
                throw new IllegalStateException("Cannot pay mana cost: missing " + insufficient.missingSymbols());
            case PaymentResult.InsufficientLife insufficient ->
                throw new IllegalStateException("Cannot pay mana cost: not enough life (" + insufficient.lifeAvailable()
                        + " < " + insufficient.lifeRequired() + ")");
        }
    }
}
