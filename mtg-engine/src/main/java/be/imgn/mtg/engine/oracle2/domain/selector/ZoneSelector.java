package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Selects an object by the zone it's in ({@mtg.rule 400}). The
/// engine's zone-routing layer: each arm is a per-zone record that
/// carries the appropriate [ObjectTypeSelector] in its `of` slot, and
/// — for owned zones — the player whose zone we're looking in.
///
/// Owned zones (Hand / Library / Graveyard) require a [PlayerSelector]
/// `owner`. Shared zones (Battlefield / Stack / Exile / CommandZone)
/// have no owner field; cards in Exile / CommandZone still have owners
/// per CR 108.3 but oracle text typically picks them by content rather
/// than by owner.
///
/// Each multi-content zone (Battlefield, Stack, CommandZone) nests its
/// own sealed `Contents` interface that enumerates which
/// [ObjectTypeSelector] arms are valid in that zone. Single-content
/// zones (Hand / Library / Graveyard / Exile) take
/// [ObjectTypeSelector.Card] directly.
public sealed interface ZoneSelector extends ObjectSelector
        permits ZoneSelector.Battlefield,
                ZoneSelector.Stack,
                ZoneSelector.Hand,
                ZoneSelector.Library,
                ZoneSelector.Graveyard,
                ZoneSelector.Exile,
                ZoneSelector.CommandZone {

    record Battlefield(Contents of) implements ZoneSelector {
        public Battlefield {
            requireNonNull(of);
        }
        /// Object classes that can live on the Battlefield.
        public sealed interface Contents permits ObjectTypeSelector.Permanent, ObjectTypeSelector.Token {}
    }

    record Stack(Contents of) implements ZoneSelector {
        public Stack {
            requireNonNull(of);
        }
        /// Object classes that can live on the Stack.
        public sealed interface Contents
                permits ObjectTypeSelector.Spell, ObjectTypeSelector.Ability, ObjectTypeSelector.Copy {}
    }

    record Hand(PlayerSelector owner, ObjectTypeSelector.Card of) implements ZoneSelector {
        public Hand {
            requireNonNull(owner);
            requireNonNull(of);
        }
    }

    record Library(PlayerSelector owner, ObjectTypeSelector.Card of) implements ZoneSelector {
        public Library {
            requireNonNull(owner);
            requireNonNull(of);
        }
    }

    record Graveyard(PlayerSelector owner, ObjectTypeSelector.Card of) implements ZoneSelector {
        public Graveyard {
            requireNonNull(owner);
            requireNonNull(of);
        }
    }

    /// Exile is a shared zone — no owner field on the selector itself.
    record Exile(ObjectTypeSelector.Card of) implements ZoneSelector {
        public Exile {
            requireNonNull(of);
        }
    }

    /// Command zone is shared — no owner field on the selector itself.
    record CommandZone(Contents of) implements ZoneSelector {
        public CommandZone {
            requireNonNull(of);
        }
        /// Object classes that can live in the Command zone.
        public sealed interface Contents permits ObjectTypeSelector.Card, ObjectTypeSelector.Emblem {}
    }
}
