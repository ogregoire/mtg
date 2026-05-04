package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CounterType;

/// Selects a player by counters on them ({@mtg.rule 122}). Player-
/// side counterpart to [ObjectCounterSelector]. Player counter
/// types are typically [CounterType.Named#POISON],
/// [CounterType.Named#ENERGY], [CounterType.Named#EXPERIENCE],
/// [CounterType.Named#RAD], [CounterType.Named#TICKET] —
/// `PtCounter` is unattested on players in oracle.
///
/// The arm carries the player being filtered ([#who]) since oracle
/// phrases this as a postfix on a player ("an opponent with three
/// or more poison counters"). A future generic player-postfix
/// mechanism may extract `who` to a wrapper layer; for now it lives
/// in the arm directly.
public sealed interface PlayerCounterSelector extends PlayerSelector permits PlayerCounterSelector.HasCounters {

    /// `who` carries N counters of `type` such that `count` matches.
    record HasCounters(PlayerSelector who, CounterType type, AmountMatcher count) implements PlayerCounterSelector {
        public HasCounters {
            requireNonNull(who);
            requireNonNull(type);
            requireNonNull(count);
        }
    }
}
