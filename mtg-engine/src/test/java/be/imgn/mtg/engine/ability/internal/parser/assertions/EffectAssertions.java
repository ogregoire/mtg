package be.imgn.mtg.engine.ability.internal.parser.assertions;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
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

/// Entry point for parser effect assertions.
public final class EffectAssertions {

    private EffectAssertions() {}

    // Base Effect assertion
    public static EffectAssert assertThat(Effect actual) {
        return new EffectAssert(actual);
    }

    // Subject assertion
    public static SubjectAssert assertThat(Subject actual) {
        return new SubjectAssert(actual);
    }

    // Removal effect assertions
    public static DestroyEffectAssert assertThat(DestroyEffect actual) {
        return new DestroyEffectAssert(actual);
    }

    public static ExileEffectAssert assertThat(ExileEffect actual) {
        return new ExileEffectAssert(actual);
    }

    public static SacrificeEffectAssert assertThat(SacrificeEffect actual) {
        return new SacrificeEffectAssert(actual);
    }

    public static ReturnToHandEffectAssert assertThat(ReturnToHandEffect actual) {
        return new ReturnToHandEffectAssert(actual);
    }

    public static PutOnLibraryEffectAssert assertThat(PutOnLibraryEffect actual) {
        return new PutOnLibraryEffectAssert(actual);
    }

    // Player effect assertions
    public static MillEffectAssert assertThat(MillEffect actual) {
        return new MillEffectAssert(actual);
    }

    public static DrawEffectAssert assertThat(DrawEffect actual) {
        return new DrawEffectAssert(actual);
    }

    public static DiscardEffectAssert assertThat(DiscardEffect actual) {
        return new DiscardEffectAssert(actual);
    }

    public static GainLifeEffectAssert assertThat(GainLifeEffect actual) {
        return new GainLifeEffectAssert(actual);
    }

    public static LoseLifeEffectAssert assertThat(LoseLifeEffect actual) {
        return new LoseLifeEffectAssert(actual);
    }

    public static ScryEffectAssert assertThat(ScryEffect actual) {
        return new ScryEffectAssert(actual);
    }

    public static SearchLibraryEffectAssert assertThat(SearchLibraryEffect actual) {
        return new SearchLibraryEffectAssert(actual);
    }

    // Damage effect assertion
    public static DealDamageEffectAssert assertThat(DealDamageEffect actual) {
        return new DealDamageEffectAssert(actual);
    }

    // Counter effect assertions
    public static AddCountersEffectAssert assertThat(AddCountersEffect actual) {
        return new AddCountersEffectAssert(actual);
    }

    public static RemoveCountersEffectAssert assertThat(RemoveCountersEffect actual) {
        return new RemoveCountersEffectAssert(actual);
    }

    // Modification effect assertions
    public static GainAbilityEffectAssert assertThat(GainAbilityEffect actual) {
        return new GainAbilityEffectAssert(actual);
    }

    public static ModifyPowerToughnessEffectAssert assertThat(ModifyPowerToughnessEffect actual) {
        return new ModifyPowerToughnessEffectAssert(actual);
    }

    public static GainControlEffectAssert assertThat(GainControlEffect actual) {
        return new GainControlEffectAssert(actual);
    }

    // Tap/Untap effect assertions
    public static TapEffectAssert assertThat(TapEffect actual) {
        return new TapEffectAssert(actual);
    }

    public static UntapEffectAssert assertThat(UntapEffect actual) {
        return new UntapEffectAssert(actual);
    }

    // Other effect assertions
    public static CounterSpellEffectAssert assertThat(CounterSpellEffect actual) {
        return new CounterSpellEffectAssert(actual);
    }

    public static FightEffectAssert assertThat(FightEffect actual) {
        return new FightEffectAssert(actual);
    }

    public static AddExactManaEffectAssert assertThat(AddManaEffect.Exact actual) {
        return new AddExactManaEffectAssert(actual);
    }

    public static AddVariableManaEffectAssert assertThat(AddManaEffect.Variable actual) {
        return new AddVariableManaEffectAssert(actual);
    }

    public static AddManaSelectionEffectAssert assertThat(AddManaEffect.Selection actual) {
        return new AddManaSelectionEffectAssert(actual);
    }

    public static AddManaCombinationEffectAssert assertThat(AddManaEffect.Combination actual) {
        return new AddManaCombinationEffectAssert(actual);
    }

    // Token effect assertions
    public static CreateTokenEffectAssert assertThat(CreateTokenEffect actual) {
        return new CreateTokenEffectAssert(actual);
    }

    public static TokenAssert assertThat(CreateTokenEffect.Token actual) {
        return new TokenAssert(actual);
    }

    public static PredefinedTokenAssert assertThat(CreateTokenEffect.Predefined actual) {
        return new PredefinedTokenAssert(actual);
    }

    public static ByCardNameTokenAssert assertThat(CreateTokenEffect.ByCardName actual) {
        return new ByCardNameTokenAssert(actual);
    }
}
