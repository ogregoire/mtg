package be.imgn.mtg.engine.oracle.domain2.selector;

import java.util.Objects;

/// Game-object class (CR 109.1) — the engine's dispatch layer for
/// "what kind of object" the selection is about: a `Permanent` /
/// `Token` (battlefield), a `Spell` / `Ability` / `Copy` (stack), or
/// a `Card` / `Emblem` (everywhere else / command zone). Distinct
/// from card-type characteristics (creature, artifact, …) which live
/// as [CharacteristicSelector] arms inside the `where` slot below.
///
/// Each leaf record carries an [ObjectPropertySelector] `where` slot
/// — the predicate constraining which instance of that object class
/// is selected. Each leaf also implements the appropriate per-zone
/// `Contents` interface ([ZoneSelector.Battlefield.Contents],
/// [ZoneSelector.Stack.Contents], [ZoneSelector.CommandZone.Contents])
/// so the type system enforces the zone↔class coupling.
///
/// `Copy` lives only on the Stack (CR 707.10): a copy of a permanent
/// spell becomes a `Token` as it resolves (CR 111.13, 608.3f, 707.10f).
/// `Emblem` lives only in the Command zone (CR 114, 408.2c).
public sealed interface ObjectTypeSelector
        permits ObjectTypeSelector.Permanent,
                ObjectTypeSelector.Token,
                ObjectTypeSelector.Spell,
                ObjectTypeSelector.Ability,
                ObjectTypeSelector.Copy,
                ObjectTypeSelector.Card,
                ObjectTypeSelector.Emblem {

    record Permanent(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Battlefield.Contents {
        public Permanent {
            Objects.requireNonNull(where);
        }
    }

    record Token(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Battlefield.Contents {
        public Token {
            Objects.requireNonNull(where);
        }
    }

    record Spell(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Spell {
            Objects.requireNonNull(where);
        }
    }

    record Ability(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Ability {
            Objects.requireNonNull(where);
        }
    }

    /// Copy of a spell or ability on the Stack ({@mtg.rule 707.10}).
    /// Copies of permanent spells become Tokens as they resolve
    /// ({@mtg.rule 111.13}, {@mtg.rule 608.3f}, {@mtg.rule 707.10f}) —
    /// they never reach the Battlefield as a Copy.
    record Copy(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Copy {
            Objects.requireNonNull(where);
        }
    }

    record Card(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.CommandZone.Contents {
        public Card {
            Objects.requireNonNull(where);
        }
    }

    /// Emblem ({@mtg.rule 114}). Lives exclusively in the Command zone
    /// ({@mtg.rule 408.2c}).
    record Emblem(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.CommandZone.Contents {
        public Emblem {
            Objects.requireNonNull(where);
        }
    }
}
