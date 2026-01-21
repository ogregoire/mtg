package be.imgn.mtg.engine.mana.internal;

import be.imgn.mtg.engine.mana.ColorlessHybridChoice;
import be.imgn.mtg.engine.mana.HybridChoice;
import be.imgn.mtg.engine.mana.HybridPhyrexianChoice;
import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.mana.ManaSymbol;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.mana.MonoColorHybridChoice;
import be.imgn.mtg.engine.mana.PaymentChoiceProvider;
import be.imgn.mtg.engine.mana.PhyrexianChoice;

/// Default automatic payment choice provider.
///
/// Makes optimal choices automatically:
/// - For hybrid: prefers the color with more mana available
/// - For Phyrexian: prefers mana if available, life if not
/// - For hybrid Phyrexian: prefers available mana, then life
public final class AutoPayChoiceProvider implements PaymentChoiceProvider {

    public static final AutoPayChoiceProvider INSTANCE = new AutoPayChoiceProvider();

    private AutoPayChoiceProvider() {}

    @Override
    public HybridChoice chooseHybrid(ManaSymbol.Hybrid symbol, ManaPool pool) {
        // Prefer the color with more mana available
        int count1 = pool.count(symbol.option1());
        int count2 = pool.count(symbol.option2());
        return count1 >= count2 ? new HybridChoice.Option1() : new HybridChoice.Option2();
    }

    @Override
    public MonoColorHybridChoice chooseMonoColorHybrid(ManaSymbol.MonoColorHybrid symbol, ManaPool pool) {
        // Prefer colored mana if available, otherwise 2 generic
        if (pool.count(symbol.colorOption()) > 0) {
            return new MonoColorHybridChoice.PayColor();
        }
        return pool.totalCount() >= 2 ? new MonoColorHybridChoice.PayGeneric() : new MonoColorHybridChoice.PayColor();
    }

    @Override
    public ColorlessHybridChoice chooseColorlessHybrid(ManaSymbol.ColorlessHybrid symbol, ManaPool pool) {
        // Prefer colorless if available, otherwise colored
        if (pool.count(ManaType.COLORLESS) > 0) {
            return new ColorlessHybridChoice.PayColorless();
        }
        return new ColorlessHybridChoice.PayColor();
    }

    @Override
    public PhyrexianChoice choosePhyrexian(ManaSymbol.Phyrexian symbol, ManaPool pool, int currentLife) {
        // Prefer mana if available, life otherwise
        if (pool.count(symbol.manaType()) > 0) {
            return new PhyrexianChoice.PayMana();
        }
        return new PhyrexianChoice.PayLife();
    }

    @Override
    public HybridPhyrexianChoice chooseHybridPhyrexian(
            ManaSymbol.HybridPhyrexian symbol, ManaPool pool, int currentLife) {
        // Prefer mana (either color) if available, life otherwise
        if (pool.count(symbol.option1()) > 0) {
            return new HybridPhyrexianChoice.PayColor1();
        }
        if (pool.count(symbol.option2()) > 0) {
            return new HybridPhyrexianChoice.PayColor2();
        }
        return new HybridPhyrexianChoice.PayLife();
    }
}
