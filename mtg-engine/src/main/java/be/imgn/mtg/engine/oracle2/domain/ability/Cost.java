package be.imgn.mtg.engine.oracle2.domain.ability;

import static java.util.Objects.requireNonNull;

import java.util.List;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// A resource cost paid to activate an ability or cast a spell
/// ({@mtg.rule 117}, {@mtg.rule 118}, {@mtg.rule 602.1}). Today the
/// hierarchy covers four primitive costs — mana, sacrifice,
/// tap, pay-life — plus a [CompoundCost] joiner for the
/// comma-separated cost lists oracle text uses on activated
/// abilities ("`{1}{G}, Sacrifice a creature: …`"). Other shapes
/// (discard, exile-from-zone, mill, loyalty, …) land as new
/// permitted records as oracle text needs them.
public sealed interface Cost permits Cost.ManaCost, Cost.Sacrifice, Cost.Tap, Cost.PayLife, Cost.CompoundCost {

    /// "{1}{G}{W}…" — the mana payment ({@mtg.rule 107.4}). The
    /// `symbols` list preserves the order they appeared in the oracle
    /// text and is defensively copied to an unmodifiable list.
    record ManaCost(List<ManaSymbol> symbols) implements Cost {
        public ManaCost {
            requireNonNull(symbols);
            symbols = List.copyOf(symbols);
        }
    }

    /// "Sacrifice X" — the controller sacrifices permanents matching
    /// `what` ({@mtg.rule 701.16}). The slot is the broad [Selector]
    /// so that count-bearing forms ("two creatures") naturally sit
    /// here as a [be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector]
    /// — same shape [be.imgn.mtg.engine.oracle2.domain.effect.SacrificeEffect]
    /// uses on the effect axis.
    record Sacrifice(Selector what) implements Cost {
        public Sacrifice {
            requireNonNull(what);
        }
    }

    /// "Tap X" — tap permanents matching `what` ({@mtg.rule 118.12}).
    /// The `{T}` symbol parses to `Tap(SelfSelector.SELF)`; oracle
    /// phrases like "Tap an untapped creature you control" (convoke,
    /// exert) parse to `Tap(<wider selector>)`. Slot is the broad
    /// [Selector] for the same reason [Sacrifice#what] uses it —
    /// count-bearing forms ("Tap two untapped creatures…") wrap in a
    /// [be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector].
    record Tap(Selector what) implements Cost {
        public Tap {
            requireNonNull(what);
        }
    }

    /// "Pay N life" — the controller pays `amount` life
    /// ({@mtg.rule 118.8}). [Amount] handles both fixed integers and
    /// the variable `X`.
    record PayLife(Amount amount) implements Cost {
        public PayLife {
            requireNonNull(amount);
        }
    }

    /// Comma-separated cost list ("`{1}{G}, Sacrifice a creature,
    /// Pay 2 life`"). Single-cost lists are never wrapped — the bare
    /// cost is returned directly by the parser.
    record CompoundCost(List<Cost> parts) implements Cost {
        public CompoundCost {
            parts = List.copyOf(parts);
            if (parts.size() < 2) {
                throw new IllegalArgumentException("CompoundCost needs at least 2 parts, got " + parts.size());
            }
        }
    }
}
