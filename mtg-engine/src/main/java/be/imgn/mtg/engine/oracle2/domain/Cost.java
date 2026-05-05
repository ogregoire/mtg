package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// A resource cost paid to activate an ability or cast a spell
/// ({@mtg.rule 117}, {@mtg.rule 118}, {@mtg.rule 602.1}). Today the
/// hierarchy covers four primitive costs — mana, sacrifice,
/// tap-self, pay-life — plus a [CompoundCost] joiner for the
/// comma-separated cost lists oracle text uses on activated
/// abilities ("`{1}{G}, Sacrifice a creature: …`"). Other shapes
/// (discard, exile-from-zone, mill, loyalty, …) land as new
/// permitted records as oracle text needs them.
public sealed interface Cost permits Cost.ManaCost, Cost.Sacrifice, Cost.TapSelf, Cost.PayLife, Cost.CompoundCost {

    /// "{1}{G}{W}…" — the mana payment. The raw oracle symbol string
    /// is kept for now; full mana-symbol modelling lands when the
    /// engine needs it.
    record ManaCost(String symbols) implements Cost {
        public ManaCost {
            requireNonNull(symbols);
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

    /// "{T}" — tap this permanent ({@mtg.rule 118.12}). Singleton
    /// because tap-self carries no payload.
    enum TapSelf implements Cost {
        TAP_SELF
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
            requireNonNull(parts);
            parts = List.copyOf(parts);
            if (parts.size() < 2) {
                throw new IllegalArgumentException("CompoundCost needs at least 2 parts, got " + parts.size());
            }
        }
    }
}
