package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.effect.AddCountersEffect;
import be.imgn.mtg.engine.effect.CounterSpellEffect;
import be.imgn.mtg.engine.effect.CreateTokenEffect;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DiscardEffect;
import be.imgn.mtg.engine.effect.DrawEffect;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.effect.ExileEffect;
import be.imgn.mtg.engine.effect.FightEffect;
import be.imgn.mtg.engine.effect.GainAbilityEffect;
import be.imgn.mtg.engine.effect.GainControlEffect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.effect.LoseLifeEffect;
import be.imgn.mtg.engine.effect.MillEffect;
import be.imgn.mtg.engine.effect.ModifyPowerToughnessEffect;
import be.imgn.mtg.engine.effect.PutOnLibraryEffect;
import be.imgn.mtg.engine.effect.RemoveCountersEffect;
import be.imgn.mtg.engine.effect.ReturnToHandEffect;
import be.imgn.mtg.engine.effect.SacrificeEffect;
import be.imgn.mtg.engine.effect.ScryEffect;
import be.imgn.mtg.engine.effect.SearchLibraryEffect;
import be.imgn.mtg.engine.effect.TapEffect;
import be.imgn.mtg.engine.effect.UntapEffect;
import be.imgn.mtg.engine.mana.AddExactManaEffectAssert;
import be.imgn.mtg.engine.mana.AddManaCombinationEffectAssert;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.AddManaSelectionEffectAssert;
import be.imgn.mtg.engine.mana.AddVariableManaEffectAssert;

/// Assertion class for Effect.
public class EffectAssert extends AbstractAssert<EffectAssert, Effect> {

    protected EffectAssert(Effect actual) {
        super(actual, EffectAssert.class);
    }

    public static EffectAssert assertThat(Effect actual) {
        return new EffectAssert(actual);
    }

    /// Verifies that the effect is a DestroyEffect and returns a specialized assert.
    public DestroyEffectAssert isDestroyEffect() {
        isNotNull();
        if (!(actual instanceof DestroyEffect)) {
            failWithMessage(
                    "Expected effect to be a DestroyEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new DestroyEffectAssert((DestroyEffect) actual);
    }

    /// Verifies that the effect is an ExileEffect and returns a specialized assert.
    public ExileEffectAssert isExileEffect() {
        isNotNull();
        if (!(actual instanceof ExileEffect)) {
            failWithMessage(
                    "Expected effect to be an ExileEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new ExileEffectAssert((ExileEffect) actual);
    }

    /// Verifies that the effect is a SacrificeEffect and returns a specialized assert.
    public SacrificeEffectAssert isSacrificeEffect() {
        isNotNull();
        if (!(actual instanceof SacrificeEffect)) {
            failWithMessage(
                    "Expected effect to be a SacrificeEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new SacrificeEffectAssert((SacrificeEffect) actual);
    }

    /// Verifies that the effect is a ReturnToHandEffect and returns a specialized assert.
    public ReturnToHandEffectAssert isReturnToHandEffect() {
        isNotNull();
        if (!(actual instanceof ReturnToHandEffect)) {
            failWithMessage(
                    "Expected effect to be a ReturnToHandEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new ReturnToHandEffectAssert((ReturnToHandEffect) actual);
    }

    /// Verifies that the effect is a PutOnLibraryEffect and returns a specialized assert.
    public PutOnLibraryEffectAssert isPutOnLibraryEffect() {
        isNotNull();
        if (!(actual instanceof PutOnLibraryEffect)) {
            failWithMessage(
                    "Expected effect to be a PutOnLibraryEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new PutOnLibraryEffectAssert((PutOnLibraryEffect) actual);
    }

    /// Verifies that the effect is a MillEffect and returns a specialized assert.
    public MillEffectAssert isMillEffect() {
        isNotNull();
        if (!(actual instanceof MillEffect)) {
            failWithMessage(
                    "Expected effect to be a MillEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new MillEffectAssert((MillEffect) actual);
    }

    /// Verifies that the effect is a DealDamageEffect and returns a specialized assert.
    public DealDamageEffectAssert isDealDamageEffect() {
        isNotNull();
        if (!(actual instanceof DealDamageEffect)) {
            failWithMessage(
                    "Expected effect to be a DealDamageEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new DealDamageEffectAssert((DealDamageEffect) actual);
    }

    /// Verifies that the effect is a GainLifeEffect and returns a specialized assert.
    public GainLifeEffectAssert isGainLifeEffect() {
        isNotNull();
        if (!(actual instanceof GainLifeEffect)) {
            failWithMessage(
                    "Expected effect to be a GainLifeEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new GainLifeEffectAssert((GainLifeEffect) actual);
    }

    /// Verifies that the effect is a LoseLifeEffect and returns a specialized assert.
    public LoseLifeEffectAssert isLoseLifeEffect() {
        isNotNull();
        if (!(actual instanceof LoseLifeEffect)) {
            failWithMessage(
                    "Expected effect to be a LoseLifeEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new LoseLifeEffectAssert((LoseLifeEffect) actual);
    }

    /// Verifies that the effect is a DrawEffect and returns a specialized assert.
    public DrawEffectAssert isDrawEffect() {
        isNotNull();
        if (!(actual instanceof DrawEffect)) {
            failWithMessage(
                    "Expected effect to be a DrawEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new DrawEffectAssert((DrawEffect) actual);
    }

    /// Verifies that the effect is a DiscardEffect and returns a specialized assert.
    public DiscardEffectAssert isDiscardEffect() {
        isNotNull();
        if (!(actual instanceof DiscardEffect)) {
            failWithMessage(
                    "Expected effect to be a DiscardEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new DiscardEffectAssert((DiscardEffect) actual);
    }

    /// Verifies that the effect is a ScryEffect and returns a specialized assert.
    public ScryEffectAssert isScryEffect() {
        isNotNull();
        if (!(actual instanceof ScryEffect)) {
            failWithMessage(
                    "Expected effect to be a ScryEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new ScryEffectAssert((ScryEffect) actual);
    }

    /// Verifies that the effect is a SearchLibraryEffect and returns a specialized assert.
    public SearchLibraryEffectAssert isSearchLibraryEffect() {
        isNotNull();
        if (!(actual instanceof SearchLibraryEffect)) {
            failWithMessage(
                    "Expected effect to be a SearchLibraryEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new SearchLibraryEffectAssert((SearchLibraryEffect) actual);
    }

    /// Verifies that the effect is a TapEffect and returns a specialized assert.
    public TapEffectAssert isTapEffect() {
        isNotNull();
        if (!(actual instanceof TapEffect)) {
            failWithMessage(
                    "Expected effect to be a TapEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new TapEffectAssert((TapEffect) actual);
    }

    /// Verifies that the effect is an UntapEffect and returns a specialized assert.
    public UntapEffectAssert isUntapEffect() {
        isNotNull();
        if (!(actual instanceof UntapEffect)) {
            failWithMessage(
                    "Expected effect to be an UntapEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new UntapEffectAssert((UntapEffect) actual);
    }

    /// Verifies that the effect is an AddCountersEffect and returns a specialized assert.
    public AddCountersEffectAssert isAddCountersEffect() {
        isNotNull();
        if (!(actual instanceof AddCountersEffect)) {
            failWithMessage(
                    "Expected effect to be an AddCountersEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new AddCountersEffectAssert((AddCountersEffect) actual);
    }

    /// Verifies that the effect is a RemoveCountersEffect and returns a specialized assert.
    public RemoveCountersEffectAssert isRemoveCountersEffect() {
        isNotNull();
        if (!(actual instanceof RemoveCountersEffect)) {
            failWithMessage(
                    "Expected effect to be a RemoveCountersEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new RemoveCountersEffectAssert((RemoveCountersEffect) actual);
    }

    /// Verifies that the effect is a GainAbilityEffect and returns a specialized assert.
    public GainAbilityEffectAssert isGainAbilityEffect() {
        isNotNull();
        if (!(actual instanceof GainAbilityEffect)) {
            failWithMessage(
                    "Expected effect to be a GainAbilityEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new GainAbilityEffectAssert((GainAbilityEffect) actual);
    }

    /// Verifies that the effect is a ModifyPowerToughnessEffect and returns a specialized assert.
    public ModifyPowerToughnessEffectAssert isModifyPowerToughnessEffect() {
        isNotNull();
        if (!(actual instanceof ModifyPowerToughnessEffect)) {
            failWithMessage(
                    "Expected effect to be a ModifyPowerToughnessEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new ModifyPowerToughnessEffectAssert((ModifyPowerToughnessEffect) actual);
    }

    /// Verifies that the effect is a GainControlEffect and returns a specialized assert.
    public GainControlEffectAssert isGainControlEffect() {
        isNotNull();
        if (!(actual instanceof GainControlEffect)) {
            failWithMessage(
                    "Expected effect to be a GainControlEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new GainControlEffectAssert((GainControlEffect) actual);
    }

    /// Verifies that the effect is a CreateTokenEffect and returns a specialized assert.
    public CreateTokenEffectAssert isCreateTokenEffect() {
        isNotNull();
        if (!(actual instanceof CreateTokenEffect)) {
            failWithMessage(
                    "Expected effect to be a CreateTokenEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new CreateTokenEffectAssert((CreateTokenEffect) actual);
    }

    /// Verifies that the effect is a CounterSpellEffect and returns a specialized assert.
    public CounterSpellEffectAssert isCounterSpellEffect() {
        isNotNull();
        if (!(actual instanceof CounterSpellEffect)) {
            failWithMessage(
                    "Expected effect to be a CounterSpellEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new CounterSpellEffectAssert((CounterSpellEffect) actual);
    }

    /// Verifies that the effect is a FightEffect and returns a specialized assert.
    public FightEffectAssert isFightEffect() {
        isNotNull();
        if (!(actual instanceof FightEffect)) {
            failWithMessage(
                    "Expected effect to be a FightEffect but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new FightEffectAssert((FightEffect) actual);
    }

    /// Verifies that the effect is an AddManaEffect.Exact and returns a specialized assert.
    public AddExactManaEffectAssert isAddExactManaEffect() {
        isNotNull();
        if (!(actual instanceof AddManaEffect.Exact)) {
            failWithMessage(
                    "Expected effect to be an AddManaEffect.Exact but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new AddExactManaEffectAssert((AddManaEffect.Exact) actual);
    }

    /// Verifies that the effect is an AddManaEffect.Variable and returns a specialized assert.
    public AddVariableManaEffectAssert isAddVariableManaEffect() {
        isNotNull();
        if (!(actual instanceof AddManaEffect.Variable)) {
            failWithMessage(
                    "Expected effect to be an AddManaEffect.Variable but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new AddVariableManaEffectAssert((AddManaEffect.Variable) actual);
    }

    /// Verifies that the effect is an AddManaEffect.Selection and returns a specialized assert.
    public AddManaSelectionEffectAssert isAddManaFromSelectionEffect() {
        isNotNull();
        if (!(actual instanceof AddManaEffect.Selection)) {
            failWithMessage(
                    "Expected effect to be an AddManaEffect.Selection but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new AddManaSelectionEffectAssert((AddManaEffect.Selection) actual);
    }

    /// Verifies that the effect is an AddManaEffect.Combination and returns a specialized assert.
    public AddManaCombinationEffectAssert isAddManaOfAnyCombinationEffect() {
        isNotNull();
        if (!(actual instanceof AddManaEffect.Combination)) {
            failWithMessage(
                    "Expected effect to be an AddManaEffect.Combination but was <%s>",
                    actual.getClass().getSimpleName());
        }
        return new AddManaCombinationEffectAssert((AddManaEffect.Combination) actual);
    }
}
