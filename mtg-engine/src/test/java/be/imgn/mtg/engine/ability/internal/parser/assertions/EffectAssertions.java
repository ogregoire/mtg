package be.imgn.mtg.engine.ability.internal.parser.assertions;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.AddManaEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.CounterSpellEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.CreateTokenEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DealDamageEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DestroyEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DiscardEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DrawEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ExileEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.FightEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.GainAbilityEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.GainControlEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.GainLifeEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.LoseLifeEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.MillEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ModifyPowerToughnessEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.PutOnLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.RemoveCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ReturnToHandEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.SacrificeEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ScryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.SearchLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.TapEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.UntapEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

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

    public static AddManaEffectAssert assertThat(AddManaEffect actual) {
        return new AddManaEffectAssert(actual);
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
