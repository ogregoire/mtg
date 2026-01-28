package be.imgn.mtg.engine.mana.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.cost.CostContext;
import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.Option;
import be.imgn.mtg.engine.mana.Mana;
import be.imgn.mtg.engine.mana.ManaAssignment;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaPaymentOption;
import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.mana.ManaPoolPaymentResult;
import be.imgn.mtg.engine.mana.ManaSymbol;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.mana.PaymentResult;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.util.ListMultimap;

/// Default implementation of [ManaPool].
///
/// Uses a ListMultimap for efficient storage of mana by type.
public final class DefaultManaPool implements ManaPool {

    private final ListMultimap<ManaType, Mana> pool;

    public DefaultManaPool() {
        this.pool = ListMultimap.newHashListMultimap();
    }

    @Override
    public List<Mana> contents() {
        return List.copyOf(pool.values());
    }

    @Override
    public int count(ManaType type) {
        return pool.get(type).size();
    }

    @Override
    public int totalCount() {
        return pool.size();
    }

    @Override
    public boolean containsSnow() {
        return pool.values().stream().anyMatch(Mana::isSnow);
    }

    @Override
    public boolean isEmpty() {
        return pool.isEmpty();
    }

    @Override
    public void add(Mana mana) {
        pool.put(mana.type(), mana);
    }

    @Override
    public void addAll(List<Mana> manaList) {
        for (var mana : manaList) {
            add(mana);
        }
    }

    @Override
    public void remove(Mana mana) {
        if (!pool.remove(mana.type(), mana)) {
            throw new IllegalArgumentException("Mana not in pool: " + mana);
        }
    }

    @Override
    public void empty() {
        pool.clear();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "ManaPool[]";
        }
        var sb = new StringBuilder("ManaPool[");
        for (var type : ManaType.values()) {
            var count = pool.get(type).size();
            if (count > 0) {
                sb.append(type.notation()).append(" x ").append(count);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /// Creates a copy of the current pool for working calculations.
    private ListMultimap<ManaType, Mana> copyPool() {
        ListMultimap<ManaType, Mana> copy = ListMultimap.newHashListMultimap();
        for (var type : ManaType.values()) {
            for (var mana : pool.get(type)) {
                copy.put(type, mana);
            }
        }
        return copy;
    }

    @Override
    public ManaPoolPaymentResult canPay(ManaCost cost, CostContext context) {
        if (cost.isEmpty()) {
            return new ManaPoolPaymentResult.FullyPayable(List.of());
        }

        // 1. Copy the multimap to work on
        var workingPool = copyPool();
        var assignments = new ArrayList<ManaAssignment>();
        var unpayable = new ArrayList<ManaSymbol>();
        var source = context.source();

        // 2. Sort symbols: specific requirements first, generic last
        var sortedSymbols = new ArrayList<>(cost.symbols());
        sortedSymbols.sort(paymentOrderComparator());

        // 3. For each symbol, find best mana and remove from working copy
        for (var symbol : sortedSymbols) {
            // Variable (X) symbols are not paid from pool - skip them
            if (symbol instanceof ManaSymbol.Variable) {
                continue;
            }
            var mana = findManaForSymbol(symbol, workingPool, source);
            if (mana != null) {
                assignments.add(new ManaAssignment(symbol, mana));
                workingPool.remove(mana.type(), mana); // Remove from working copy
            } else {
                unpayable.add(symbol);
            }
        }

        // 4. Return result
        if (unpayable.isEmpty()) {
            return new ManaPoolPaymentResult.FullyPayable(assignments);
        } else if (assignments.isEmpty()) {
            return new ManaPoolPaymentResult.NotPayable();
        } else {
            return new ManaPoolPaymentResult.PartiallyPayable(
                    sortedSymbols.stream().filter(s -> !unpayable.contains(s)).toList(), unpayable, assignments);
        }
    }

    @Override
    public void pay(ManaCost cost, List<ManaAssignment> assignments) {
        for (var assignment : assignments) {
            if (assignment.mana() != null) {
                remove(assignment.mana());
            }
        }
    }

    /// Returns a comparator that orders symbols for optimal payment.
    /// Colored requirements first, then colorless, then generic last.
    /// Within each category, prioritize more restrictive requirements.
    private Comparator<ManaSymbol> paymentOrderComparator() {
        return (a, b) -> {
            var priorityA = getPaymentPriority(a);
            var priorityB = getPaymentPriority(b);
            return Integer.compare(priorityA, priorityB);
        };
    }

    private int getPaymentPriority(ManaSymbol symbol) {
        // Lower = pay first
        return switch (symbol) {
            case ManaSymbol.Colored _ -> 1;
            case ManaSymbol.Colorless _ -> 2;
            case ManaSymbol.Snow _ -> 3;
            case ManaSymbol.Phyrexian _ -> 4;
            case ManaSymbol.Hybrid _ -> 5;
            case ManaSymbol.MonoColorHybrid _ -> 6;
            case ManaSymbol.ColorlessHybrid _ -> 6;
            case ManaSymbol.HybridPhyrexian _ -> 7;
            case ManaSymbol.Variable _ -> 8;
            case ManaSymbol.Generic _ -> 9; // Generic last - most flexible
        };
    }

    /// Finds the best mana for a symbol.
    /// Prefers restricted mana whose restriction matches the source.
    private @Nullable Mana findManaForSymbol(
            ManaSymbol symbol, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        return switch (symbol) {
            case ManaSymbol.Colored c -> findManaOfType(c.manaType(), workingPool, source);
            case ManaSymbol.Colorless _ -> findManaOfType(ManaType.COLORLESS, workingPool, source);
            case ManaSymbol.Generic _ -> findAnyMana(workingPool, source);
            case ManaSymbol.Snow _ -> findSnowMana(workingPool, source);
            case ManaSymbol.Phyrexian p -> findManaOfType(p.manaType(), workingPool, source);
            case ManaSymbol.Hybrid h -> findHybridMana(h, workingPool, source);
            case ManaSymbol.MonoColorHybrid m -> findMonoColorHybridMana(m, workingPool, source);
            case ManaSymbol.ColorlessHybrid c -> findColorlessHybridMana(c, workingPool, source);
            case ManaSymbol.HybridPhyrexian hp -> findHybridPhyrexianMana(hp, workingPool, source);
            case ManaSymbol.Variable _ -> null; // X not paid from pool
        };
    }

    /// Finds mana of a specific type.
    /// Priority: restricted matching source > unrestricted
    private @Nullable Mana findManaOfType(ManaType type, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        var manaList = workingPool.get(type);
        if (manaList.isEmpty()) {
            return null;
        }

        // 1. Prefer restricted mana whose restriction matches source
        for (var mana : manaList) {
            if (mana instanceof Mana.Restricted(var t, var s, var restriction) && restriction.canSpendOn(source)) {
                return mana;
            }
        }

        // 2. Then unrestricted mana
        for (var mana : manaList) {
            if (!(mana instanceof Mana.Restricted)) {
                return mana;
            }
        }

        // 3. No suitable mana found (restricted mana that doesn't match is NOT usable)
        return null;
    }

    /// Finds any mana (for generic costs).
    /// Priority: restricted matching source > unrestricted
    private @Nullable Mana findAnyMana(ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        // 1. First try restricted mana that matches source
        for (var type : ManaType.values()) {
            for (var mana : workingPool.get(type)) {
                if (mana instanceof Mana.Restricted(var t, var s, var restriction) && restriction.canSpendOn(source)) {
                    return mana;
                }
            }
        }

        // 2. Then unrestricted mana
        for (var type : ManaType.values()) {
            for (var mana : workingPool.get(type)) {
                if (!(mana instanceof Mana.Restricted)) {
                    return mana;
                }
            }
        }

        return null;
    }

    /// Finds any snow mana.
    /// Priority: restricted matching source > unrestricted
    private @Nullable Mana findSnowMana(ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        // 1. First try restricted snow mana that matches source
        for (var type : ManaType.values()) {
            for (var mana : workingPool.get(type)) {
                if (mana.isSnow()
                        && mana instanceof Mana.Restricted(var t, var s, var restriction)
                        && restriction.canSpendOn(source)) {
                    return mana;
                }
            }
        }

        // 2. Then unrestricted snow mana
        for (var type : ManaType.values()) {
            for (var mana : workingPool.get(type)) {
                if (mana.isSnow() && !(mana instanceof Mana.Restricted)) {
                    return mana;
                }
            }
        }

        return null;
    }

    /// Finds mana for a two-color hybrid symbol.
    /// Tries either color option.
    private @Nullable Mana findHybridMana(
            ManaSymbol.Hybrid symbol, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        var mana1 = findManaOfType(symbol.option1(), workingPool, source);
        if (mana1 != null) {
            return mana1;
        }
        return findManaOfType(symbol.option2(), workingPool, source);
    }

    /// Finds mana for a mono-color hybrid symbol.
    /// Tries colored mana first.
    private @Nullable Mana findMonoColorHybridMana(
            ManaSymbol.MonoColorHybrid symbol, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        var colorMana = findManaOfType(symbol.colorOption(), workingPool, source);
        if (colorMana != null) {
            return colorMana;
        }
        // Could pay with 2 generic, but that requires more complex handling
        return null;
    }

    /// Finds mana for a colorless hybrid symbol.
    /// Tries colorless first, then colored.
    private @Nullable Mana findColorlessHybridMana(
            ManaSymbol.ColorlessHybrid symbol, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        var colorlessMana = findManaOfType(ManaType.COLORLESS, workingPool, source);
        if (colorlessMana != null) {
            return colorlessMana;
        }
        return findManaOfType(symbol.colorOption(), workingPool, source);
    }

    /// Finds mana for a hybrid Phyrexian symbol.
    /// Tries either color option.
    private @Nullable Mana findHybridPhyrexianMana(
            ManaSymbol.HybridPhyrexian symbol, ListMultimap<ManaType, Mana> workingPool, GameObject source) {
        // Try either color
        var mana1 = findManaOfType(symbol.option1(), workingPool, source);
        if (mana1 != null) {
            return mana1;
        }
        return findManaOfType(symbol.option2(), workingPool, source);
    }

    // ========== Full payment methods (with life payment support) ==========

    @Override
    public boolean canPayFully(ManaCost cost, CostContext context) {
        if (cost.isEmpty()) {
            return true;
        }

        var lifeTotal = context.player().lifeTotal();
        var poolResult = canPay(cost, context);

        return switch (poolResult) {
            case ManaPoolPaymentResult.FullyPayable _ -> true;
            case ManaPoolPaymentResult.NotPayable _ -> canPayAllWithLife(cost, lifeTotal);
            case ManaPoolPaymentResult.PartiallyPayable partial ->
                canPayUnpayableWithLife(partial.unpayable(), lifeTotal);
        };
    }

    private boolean canPayAllWithLife(ManaCost cost, int lifeTotal) {
        // Can only pay Phyrexian symbols with life
        var lifeRequired = 0;
        for (var symbol : cost.symbols()) {
            if (symbol instanceof ManaSymbol.Phyrexian || symbol instanceof ManaSymbol.HybridPhyrexian) {
                lifeRequired += 2;
            } else if (!(symbol instanceof ManaSymbol.Variable)) {
                // Non-Phyrexian, non-variable symbols cannot be paid with life
                return false;
            }
        }
        return lifeTotal >= lifeRequired;
    }

    private boolean canPayUnpayableWithLife(List<ManaSymbol> unpayable, int lifeTotal) {
        var lifeRequired = 0;
        for (var symbol : unpayable) {
            if (symbol instanceof ManaSymbol.Phyrexian || symbol instanceof ManaSymbol.HybridPhyrexian) {
                lifeRequired += 2;
            } else {
                // Non-Phyrexian symbols cannot be paid with life
                return false;
            }
        }
        return lifeTotal >= lifeRequired;
    }

    @Override
    public PaymentResult payFully(ManaCost cost, CostContext context) {
        if (cost.isEmpty()) {
            return new PaymentResult.Success(List.of(), 0);
        }

        var lifeTotal = context.player().lifeTotal();
        var manaSpent = new ArrayList<Mana>();
        var lifePaid = 0;
        var missingSymbols = new ArrayList<ManaSymbol>();

        for (var symbol : cost.symbols()) {
            var payResult = paySymbolFully(symbol, context, lifeTotal - lifePaid);

            switch (payResult) {
                case SymbolPayResult.PaidWithMana paid -> {
                    remove(paid.mana());
                    manaSpent.add(paid.mana());
                }
                case SymbolPayResult.PaidWithLife paid -> lifePaid += paid.amount();
                case SymbolPayResult.CannotPay _ -> missingSymbols.add(symbol);
                case SymbolPayResult.Skipped _ -> {
                    // Variable X - not paid now
                }
            }
        }

        if (!missingSymbols.isEmpty()) {
            return new PaymentResult.InsufficientMana(missingSymbols);
        }

        return new PaymentResult.Success(manaSpent, lifePaid);
    }

    private sealed interface SymbolPayResult {
        record PaidWithMana(Mana mana) implements SymbolPayResult {}

        record PaidWithLife(int amount) implements SymbolPayResult {}

        record CannotPay() implements SymbolPayResult {}

        record Skipped() implements SymbolPayResult {}
    }

    private SymbolPayResult paySymbolFully(ManaSymbol symbol, CostContext context, int availableLife) {
        var source = context.source();
        return switch (symbol) {
            case ManaSymbol.Colored c -> payColoredFully(c, source);
            case ManaSymbol.Colorless _ -> payColorlessFully(source);
            case ManaSymbol.Generic _ -> payGenericFully(source);
            case ManaSymbol.Snow _ -> paySnowFully(source);
            case ManaSymbol.Phyrexian p -> payPhyrexianFully(p, context, availableLife);
            case ManaSymbol.Hybrid h -> payHybridFully(h, context);
            case ManaSymbol.MonoColorHybrid m -> payMonoColorHybridFully(m, context);
            case ManaSymbol.ColorlessHybrid c -> payColorlessHybridFully(c, context);
            case ManaSymbol.HybridPhyrexian hp -> payHybridPhyrexianFully(hp, context, availableLife);
            case ManaSymbol.Variable _ -> new SymbolPayResult.Skipped();
        };
    }

    private SymbolPayResult payColoredFully(ManaSymbol.Colored symbol, GameObject source) {
        var mana = findManaByType(symbol.manaType(), source);
        return mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
    }

    private SymbolPayResult payColorlessFully(GameObject source) {
        var mana = findManaByType(ManaType.COLORLESS, source);
        return mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
    }

    private SymbolPayResult payGenericFully(GameObject source) {
        // Pay one mana of any type
        for (var type : ManaType.values()) {
            var mana = findManaByType(type, source);
            if (mana != null) {
                return new SymbolPayResult.PaidWithMana(mana);
            }
        }
        return new SymbolPayResult.CannotPay();
    }

    private SymbolPayResult paySnowFully(GameObject source) {
        for (var mana : contents()) {
            if (mana.isSnow() && canSpendOn(mana, source)) {
                return new SymbolPayResult.PaidWithMana(mana);
            }
        }
        return new SymbolPayResult.CannotPay();
    }

    private SymbolPayResult payPhyrexianFully(ManaSymbol.Phyrexian symbol, CostContext context, int availableLife) {
        var source = context.source();
        var hasMana = findManaByType(symbol.manaType(), source) != null;
        var canPayLife = availableLife >= 2;

        // Build options based on what's available
        var options = new ArrayList<Option<ManaPaymentOption>>();
        if (hasMana) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.manaType()),
                    "Pay " + symbol.manaType().notation()));
        }
        if (canPayLife) {
            options.add(new Option<>(ManaPaymentOption.PayLife.TWO, "Pay 2 life"));
        }

        if (options.isEmpty()) {
            return new SymbolPayResult.CannotPay();
        }

        var choice = Choice.withDescriptions(options, "Pay " + symbol);
        var selected = context.player().choose(choice).getFirst();

        return switch (selected) {
            case ManaPaymentOption.PayMana pay -> {
                var mana = findManaByType(pay.type(), source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            case ManaPaymentOption.PayLife pay -> new SymbolPayResult.PaidWithLife(pay.amount());
            case ManaPaymentOption.PayGeneric _ -> new SymbolPayResult.CannotPay(); // Not valid for Phyrexian
        };
    }

    private SymbolPayResult payHybridFully(ManaSymbol.Hybrid symbol, CostContext context) {
        var source = context.source();
        var hasMana1 = findManaByType(symbol.option1(), source) != null;
        var hasMana2 = findManaByType(symbol.option2(), source) != null;

        // Build options based on what's available
        var options = new ArrayList<Option<ManaPaymentOption>>();
        if (hasMana1) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.option1()),
                    "Pay " + symbol.option1().notation()));
        }
        if (hasMana2) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.option2()),
                    "Pay " + symbol.option2().notation()));
        }

        if (options.isEmpty()) {
            return new SymbolPayResult.CannotPay();
        }

        var choice = Choice.withDescriptions(options, "Pay " + symbol);
        var selected = context.player().choose(choice).getFirst();

        return switch (selected) {
            case ManaPaymentOption.PayMana pay -> {
                var mana = findManaByType(pay.type(), source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            default -> new SymbolPayResult.CannotPay();
        };
    }

    private SymbolPayResult payMonoColorHybridFully(ManaSymbol.MonoColorHybrid symbol, CostContext context) {
        var source = context.source();
        var hasColorMana = findManaByType(symbol.colorOption(), source) != null;
        var hasGenericMana = countAvailableGeneric(source) >= 2;

        // Build options based on what's available
        var options = new ArrayList<Option<ManaPaymentOption>>();
        if (hasColorMana) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.colorOption()),
                    "Pay " + symbol.colorOption().notation()));
        }
        if (hasGenericMana) {
            options.add(new Option<>(ManaPaymentOption.PayGeneric.TWO, "Pay {2}"));
        }

        if (options.isEmpty()) {
            return new SymbolPayResult.CannotPay();
        }

        var choice = Choice.withDescriptions(options, "Pay " + symbol);
        var selected = context.player().choose(choice).getFirst();

        return switch (selected) {
            case ManaPaymentOption.PayMana pay -> {
                var mana = findManaByType(pay.type(), source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            case ManaPaymentOption.PayGeneric _ -> {
                // Pay 2 generic - just find any mana (caller handles iterating)
                var mana = findAnyManaInPool(source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            default -> new SymbolPayResult.CannotPay();
        };
    }

    private SymbolPayResult payColorlessHybridFully(ManaSymbol.ColorlessHybrid symbol, CostContext context) {
        var source = context.source();
        var hasColorlessMana = findManaByType(ManaType.COLORLESS, source) != null;
        var hasColorMana = findManaByType(symbol.colorOption(), source) != null;

        // Build options based on what's available
        var options = new ArrayList<Option<ManaPaymentOption>>();
        if (hasColorlessMana) {
            options.add(new Option<>(new ManaPaymentOption.PayMana(ManaType.COLORLESS), "Pay {C}"));
        }
        if (hasColorMana) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.colorOption()),
                    "Pay " + symbol.colorOption().notation()));
        }

        if (options.isEmpty()) {
            return new SymbolPayResult.CannotPay();
        }

        var choice = Choice.withDescriptions(options, "Pay " + symbol);
        var selected = context.player().choose(choice).getFirst();

        return switch (selected) {
            case ManaPaymentOption.PayMana pay -> {
                var mana = findManaByType(pay.type(), source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            default -> new SymbolPayResult.CannotPay();
        };
    }

    private SymbolPayResult payHybridPhyrexianFully(
            ManaSymbol.HybridPhyrexian symbol, CostContext context, int availableLife) {
        var source = context.source();
        var hasMana1 = findManaByType(symbol.option1(), source) != null;
        var hasMana2 = findManaByType(symbol.option2(), source) != null;
        var canPayLife = availableLife >= 2;

        // Build options based on what's available
        var options = new ArrayList<Option<ManaPaymentOption>>();
        if (hasMana1) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.option1()),
                    "Pay " + symbol.option1().notation()));
        }
        if (hasMana2) {
            options.add(new Option<>(
                    new ManaPaymentOption.PayMana(symbol.option2()),
                    "Pay " + symbol.option2().notation()));
        }
        if (canPayLife) {
            options.add(new Option<>(ManaPaymentOption.PayLife.TWO, "Pay 2 life"));
        }

        if (options.isEmpty()) {
            return new SymbolPayResult.CannotPay();
        }

        var choice = Choice.withDescriptions(options, "Pay " + symbol);
        var selected = context.player().choose(choice).getFirst();

        return switch (selected) {
            case ManaPaymentOption.PayMana pay -> {
                var mana = findManaByType(pay.type(), source);
                yield mana != null ? new SymbolPayResult.PaidWithMana(mana) : new SymbolPayResult.CannotPay();
            }
            case ManaPaymentOption.PayLife pay -> new SymbolPayResult.PaidWithLife(pay.amount());
            default -> new SymbolPayResult.CannotPay();
        };
    }

    /// Counts how many mana are available to pay generic costs.
    private int countAvailableGeneric(GameObject source) {
        var count = 0;
        for (var mana : contents()) {
            if (canSpendOn(mana, source)) {
                count++;
            }
        }
        return count;
    }

    /// Finds mana of a specific type from the actual pool.
    /// Priority: restricted matching source > unrestricted
    private @Nullable Mana findManaByType(ManaType type, GameObject source) {
        var contents = contents();
        // Prefer restricted mana first (that can be spent on source)
        for (var mana : contents) {
            if (mana instanceof Mana.Restricted(var manaType, var src, var restr)
                    && manaType == type
                    && restr.canSpendOn(source)) {
                return mana;
            }
        }
        // Then unrestricted
        for (var mana : contents) {
            if (mana.type() == type && !(mana instanceof Mana.Restricted)) {
                return mana;
            }
        }
        return null;
    }

    /// Finds any mana from the actual pool.
    /// Priority: restricted matching source > unrestricted
    private @Nullable Mana findAnyManaInPool(GameObject source) {
        var allMana = contents();
        // Prefer restricted mana first (that can be spent on source)
        for (var mana : allMana) {
            if (mana instanceof Mana.Restricted(var t, var s, var restr) && restr.canSpendOn(source)) {
                return mana;
            }
        }
        // Then unrestricted
        for (var mana : allMana) {
            if (!(mana instanceof Mana.Restricted)) {
                return mana;
            }
        }
        return null;
    }

    /// Checks if the given mana can be spent on the source game object.
    /// Unrestricted mana can always be spent. Restricted mana can only be spent
    /// if the restriction allows spending on the source.
    private boolean canSpendOn(Mana mana, GameObject source) {
        if (!(mana instanceof Mana.Restricted(var type, var src, var restriction))) {
            return true; // Unrestricted mana can be spent on anything
        }
        return restriction.canSpendOn(source);
    }
}
