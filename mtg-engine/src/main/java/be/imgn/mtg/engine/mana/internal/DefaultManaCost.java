package be.imgn.mtg.engine.mana.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaSymbol;

/// Default implementation of [ManaCost].
public final class DefaultManaCost implements ManaCost {

    private final List<ManaSymbol> symbols;
    private final int manaValue;
    private final Colors colors;
    private final int genericComponent;
    private final int variableCount;

    DefaultManaCost(List<ManaSymbol> symbols) {
        this.symbols = List.copyOf(symbols);
        this.manaValue = symbols.stream().mapToInt(ManaSymbol::manaValue).sum();
        this.colors = computeColors(symbols);
        this.genericComponent = computeGenericComponent(symbols);
        this.variableCount = (int)
                symbols.stream().filter(s -> s instanceof ManaSymbol.Variable).count();
    }

    private static Colors computeColors(List<ManaSymbol> symbols) {
        var builder = Colors.builder();
        for (var symbol : symbols) {
            for (var color : symbol.colors().stream().toList()) {
                builder.add(color);
            }
        }
        return builder.build();
    }

    private static int computeGenericComponent(List<ManaSymbol> symbols) {
        return symbols.stream()
                .filter(s -> s instanceof ManaSymbol.Generic)
                .mapToInt(s -> ((ManaSymbol.Generic) s).amount())
                .sum();
    }

    @Override
    public List<ManaSymbol> symbols() {
        return symbols;
    }

    @Override
    public int manaValue() {
        return manaValue;
    }

    @Override
    public Colors colors() {
        return colors;
    }

    @Override
    public int genericComponent() {
        return genericComponent;
    }

    @Override
    public boolean hasVariable() {
        return variableCount > 0;
    }

    @Override
    public int variableCount() {
        return variableCount;
    }

    @Override
    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    @Override
    public ManaCost plus(ManaCost other) {
        var combined = new ArrayList<>(symbols);
        combined.addAll(other.symbols());
        return new DefaultManaCost(combined);
    }

    @Override
    public ManaCost minusGeneric(int amount) {
        if (amount <= 0) {
            return this;
        }
        var newSymbols = new ArrayList<ManaSymbol>();
        int remaining = amount;
        for (var symbol : symbols) {
            if (symbol instanceof ManaSymbol.Generic g && remaining > 0) {
                int newAmount = g.amount() - remaining;
                remaining = Math.max(0, remaining - g.amount());
                if (newAmount > 0) {
                    newSymbols.add(new ManaSymbol.Generic(newAmount));
                }
            } else {
                newSymbols.add(symbol);
            }
        }
        return new DefaultManaCost(newSymbols);
    }

    @Override
    public String description() {
        if (symbols.isEmpty()) {
            return "{0}";
        }
        return symbols.stream().map(ManaSymbol::notation).collect(Collectors.joining());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DefaultManaCost other)) return false;
        return symbols.equals(other.symbols);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbols);
    }

    @Override
    public String toString() {
        return description();
    }
}
