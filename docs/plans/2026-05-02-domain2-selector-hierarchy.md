# Selector hierarchy + AllOf/AnyOf for `oracle.domain2`

## Context

The Quantifier system is in. Two gaps remain in `oracle.domain2.selector`:

1. **Hierarchy under the markers is unstructured.** `ZoneSelector` and `ObjectTypeSelector` are empty `non-sealed` markers — the engine has no fixed set of zones / object classes to dispatch on, and there's nothing stopping `Permanent(Battlefield(...))` or `Permanent(Permanent(...))` from compiling.
2. **No boolean composition over the property axes.** Oracle text routinely combines: "creature or planeswalker", "non-creature non-land permanent", "creature you control with flying".

Goal: a strict, type-enforced envelope — each layer can hold only the appropriate next-layer type — plus boolean composition at the property level. **Nest new types inside their containing sealed interface** (so they shed the redundant `Selector` suffix per Java convention).

## Hierarchy (strict envelope)

```
Selector (sealed)
├── ObjectSelector (sealed)              ← top-level object marker, permits exactly two arms
│   ├── ZoneSelector (sealed)
│   │   ├── ZoneSelector.Battlefield(Battlefield.Contents of)
│   │   │   └── Battlefield.Contents (sealed) → permits ObjectTypeSelector.Permanent | .Token
│   │   ├── ZoneSelector.Stack(Stack.Contents of)
│   │   │   └── Stack.Contents (sealed) → permits ObjectTypeSelector.Spell | .Ability | .Copy
│   │   ├── ZoneSelector.Hand(PlayerSelector owner, ObjectTypeSelector.Card of)
│   │   ├── ZoneSelector.Library(PlayerSelector owner, ObjectTypeSelector.Card of)
│   │   ├── ZoneSelector.Graveyard(PlayerSelector owner, ObjectTypeSelector.Card of)
│   │   ├── ZoneSelector.Exile(ObjectTypeSelector.Card of)                ← shared zone, no owner
│   │   └── ZoneSelector.CommandZone(CommandZone.Contents of)             ← shared zone, no owner
│   │       └── CommandZone.Contents (sealed) → permits ObjectTypeSelector.Card | .Emblem
│   └── SelfSelector                     ← single specific object known to resolver

ObjectTypeSelector (sealed, NOT extends ObjectSelector)  ← engine dispatch layer for object class
├── ObjectTypeSelector.Permanent(ObjectPropertySelector where)   implements Battlefield.Contents
├── ObjectTypeSelector.Token(ObjectPropertySelector where)       implements Battlefield.Contents
├── ObjectTypeSelector.Spell(ObjectPropertySelector where)       implements Stack.Contents
├── ObjectTypeSelector.Ability(ObjectPropertySelector where)     implements Stack.Contents
├── ObjectTypeSelector.Copy(ObjectPropertySelector where)        implements Stack.Contents
├── ObjectTypeSelector.Card(ObjectPropertySelector where)        implements CommandZone.Contents
└── ObjectTypeSelector.Emblem(ObjectPropertySelector where)      implements CommandZone.Contents
│
├── PlayerSelector
│   ├── PlayerSelector.Anyone               ← NEW (enum, ANYONE — always-true)
│   ├── PlayerSelector.Enchanted(ObjectSelector by) ← NEW (replaces AttachedPlayerSelector)
│   └── (existing player arms — OtherPlayerSelector, PlayerRelationSelector, …)
│
├── Target
└── QuantifierSelector

ObjectPropertySelector (sealed, NOT extends ObjectSelector)
├── (existing standalone axis files — keep names, retype implements)
│   ├── CharacteristicSelector (sealed) → 13 characteristic leaves
│   ├── ControlledBySelector(PlayerSelector by)
│   ├── OwnedBySelector(PlayerSelector by)
│   ├── StatusSelector / ObjectDesignationSelector
│   ├── ObjectCounterSelector / StickerSelector
│   ├── CombatStatusSelector / CombatRoleSelector
│   ├── AttachesToSelector(Selector to)
│   └── OtherObjectSelector(ObjectSelector than)
└── (NEW nested arms)
    ├── ObjectPropertySelector.Anything                              ← enum, ANYTHING
    ├── ObjectPropertySelector.AllOf(List<ObjectPropertySelector>)
    ├── ObjectPropertySelector.AnyOf(List<ObjectPropertySelector>)
    ├── ObjectPropertySelector.Not(ObjectPropertySelector)
    ├── ObjectPropertySelector.Enchanted(ObjectSelector by)     ← Aura host ("enchanted creature")
    ├── ObjectPropertySelector.Equipped(ObjectSelector by)      ← Equipment host ("equipped creature")
    └── ObjectPropertySelector.Fortified(ObjectSelector by)     ← Fortification host ("fortified land")
```

**Why `ObjectPropertySelector` does NOT extend `ObjectSelector`:** if it did, `QuantifierSelector(Quantifier, Selector)` would accept a property at the top, breaking the envelope. As a parallel sealed type, `ObjectPropertySelector` exists only as the content of an object-class record (Permanent, Token, …). That's what enforces the order.

**`ObjectTypeSelector` is kept as a parallel sealed type at the package level** so the engine has a single dispatch layer covering all object classes (Permanent | Token | Spell | Ability | Copy | Card | Emblem) regardless of zone. **The leaf records nest inside `ObjectTypeSelector`** as their primary home; each leaf also implements the appropriate per-zone nested `Contents` sealed interface (which lives inside the zone record). Same reason `ZoneSelector` stays as its own layer rather than being collapsed into `ObjectSelector`.

## ZoneSelector (one file, nested zones + per-zone Contents)

```java
package be.imgn.mtg.engine.oracle.domain2.selector;

public sealed interface ZoneSelector extends ObjectSelector
        permits ZoneSelector.Battlefield, ZoneSelector.Stack, ZoneSelector.Hand,
                ZoneSelector.Library, ZoneSelector.Graveyard, ZoneSelector.Exile,
                ZoneSelector.CommandZone {

    record Battlefield(Contents of) implements ZoneSelector {
        public Battlefield { Objects.requireNonNull(of); }
        public sealed interface Contents permits ObjectTypeSelector.Permanent, ObjectTypeSelector.Token {}
    }

    record Stack(Contents of) implements ZoneSelector {
        public Stack { Objects.requireNonNull(of); }
        public sealed interface Contents permits
                ObjectTypeSelector.Spell, ObjectTypeSelector.Ability, ObjectTypeSelector.Copy {}
    }

    record Hand(PlayerSelector owner, ObjectTypeSelector.Card of)      implements ZoneSelector { /* null-checks */ }
    record Library(PlayerSelector owner, ObjectTypeSelector.Card of)   implements ZoneSelector { /* null-checks */ }
    record Graveyard(PlayerSelector owner, ObjectTypeSelector.Card of) implements ZoneSelector { /* null-checks */ }
    record Exile(ObjectTypeSelector.Card of)                            implements ZoneSelector { /* null-check */  }

    record CommandZone(Contents of) implements ZoneSelector {
        public CommandZone { Objects.requireNonNull(of); }
        public sealed interface Contents permits ObjectTypeSelector.Card, ObjectTypeSelector.Emblem {}
    }
}
```

## ObjectTypeSelector (one file, nests the leaf records)

```java
package be.imgn.mtg.engine.oracle.domain2.selector;

public sealed interface ObjectTypeSelector
        permits ObjectTypeSelector.Permanent, ObjectTypeSelector.Token,
                ObjectTypeSelector.Spell, ObjectTypeSelector.Ability, ObjectTypeSelector.Copy,
                ObjectTypeSelector.Card, ObjectTypeSelector.Emblem {

    record Permanent(ObjectPropertySelector where) implements ObjectTypeSelector, ZoneSelector.Battlefield.Contents {
        public Permanent { Objects.requireNonNull(where); }
    }
    record Token(ObjectPropertySelector where)     implements ObjectTypeSelector, ZoneSelector.Battlefield.Contents {
        public Token { Objects.requireNonNull(where); }
    }
    record Spell(ObjectPropertySelector where)     implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Spell { Objects.requireNonNull(where); }
    }
    record Ability(ObjectPropertySelector where)   implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Ability { Objects.requireNonNull(where); }
    }
    record Copy(ObjectPropertySelector where)      implements ObjectTypeSelector, ZoneSelector.Stack.Contents {
        public Copy { Objects.requireNonNull(where); }
    }
    record Card(ObjectPropertySelector where)      implements ObjectTypeSelector, ZoneSelector.CommandZone.Contents {
        public Card { Objects.requireNonNull(where); }
    }
    record Emblem(ObjectPropertySelector where)    implements ObjectTypeSelector, ZoneSelector.CommandZone.Contents {
        public Emblem { Objects.requireNonNull(where); }
    }
}
```

Notes on the layout:
- The leaf records' **primary home** is `ObjectTypeSelector` — that's the dispatch layer the engine uses to ask "what kind of object". Each leaf additionally implements the appropriate per-zone `Contents` so the type system enforces that `Battlefield.of` can only hold `Permanent` or `Token`, etc.
- The per-zone `Contents` interfaces (nested in their respective zone records) only enumerate which `ObjectTypeSelector` arms belong to that zone.
- `Card` lives in `ObjectTypeSelector` and implements `CommandZone.Contents`. Hand / Library / Graveyard / Exile take `ObjectTypeSelector.Card` directly (typed by the leaf record, not by Contents).
- `Copy` lives only in `Stack.Contents` (CR 707.10). A copy of a permanent spell becomes a `Token` as it resolves (CR 111.13, 608.3f, 707.10f).
- `Emblem` lives only in `CommandZone.Contents` (CR 114, 408.2c).
- Java sealed permits can cross between sibling top-level files — `Battlefield.Contents permits ObjectTypeSelector.Permanent, ObjectTypeSelector.Token` works because the leaf records list `Battlefield.Contents` in their `implements`. Cross-file sealed wiring is supported as long as both files are in the same module/compilation unit.

## ObjectPropertySelector (one file, nested boolean comp + filler + attachment hosts)

```java
package be.imgn.mtg.engine.oracle.domain2.selector;

public sealed interface ObjectPropertySelector permits
        // standalone axes (existing files, retyped)
        CharacteristicSelector,
        ControlledBySelector, OwnedBySelector,
        StatusSelector, ObjectDesignationSelector,
        ObjectCounterSelector, StickerSelector,
        CombatStatusSelector, CombatRoleSelector,
        AttachesToSelector, OtherObjectSelector,
        // nested
        ObjectPropertySelector.Anything,
        ObjectPropertySelector.AllOf,
        ObjectPropertySelector.AnyOf,
        ObjectPropertySelector.Not,
        ObjectPropertySelector.Enchanted,
        ObjectPropertySelector.Equipped,
        ObjectPropertySelector.Fortified {

    enum Anything implements ObjectPropertySelector { ANYTHING }   // always-true predicate

    record AllOf(List<ObjectPropertySelector> selectors) implements ObjectPropertySelector {
        public AllOf { selectors = List.copyOf(selectors); }
    }
    record AnyOf(List<ObjectPropertySelector> selectors) implements ObjectPropertySelector {
        public AnyOf { selectors = List.copyOf(selectors); }
    }
    record Not(ObjectPropertySelector selector) implements ObjectPropertySelector {
        public Not { Objects.requireNonNull(selector); }
    }

    /// "enchanted creature/permanent/land" — Aura host (CR 702.5).
    record Enchanted(ObjectSelector by) implements ObjectPropertySelector {
        public Enchanted { Objects.requireNonNull(by); }
    }
    /// "equipped creature" — Equipment host (CR 702.6).
    record Equipped(ObjectSelector by) implements ObjectPropertySelector {
        public Equipped { Objects.requireNonNull(by); }
    }
    /// "fortified land" — Fortification host (CR 702.67).
    record Fortified(ObjectSelector by) implements ObjectPropertySelector {
        public Fortified { Objects.requireNonNull(by); }
    }
}
```

The 14 existing axis files keep their names and live as standalone files (not nested) because they pre-date this restructure and follow the `*Selector`-suffix convention; only their `implements` line changes.

`Selector`-suffixed standalone vs. nested-without-suffix is the explicit intent: things the user wrote as separate files keep their original style; types we're introducing as part of the nested grouping shed the suffix.

## PlayerSelector — nested Anyone + Enchanted

```java
public non-sealed interface PlayerSelector extends Selector {
    enum Anyone implements PlayerSelector { ANYONE }
    /// "enchanted player" — Aura host on a player (Curses, CR 303.4).
    record Enchanted(ObjectSelector by) implements PlayerSelector {
        public Enchanted { Objects.requireNonNull(by); }
    }
}
```

If we choose to seal `PlayerSelector`, list every existing arm (`OtherPlayerSelector`, `PlayerRelationSelector`, `PlayerCounterSelector`, `PlayerDesignationSelector`, `PlayerTurnRoleSelector`, `ControllerSelector`, `OwnerSelector`) plus the two nested ones in permits. If sealing creates downstream friction, leave it `non-sealed`.

## Worked examples

Notation in this table uses simple names assuming the right static imports (e.g. `import static …ZoneSelector.*;` and `import static …ObjectPropertySelector.*;`). Real code reads `Battlefield`, `Permanent`, `AllOf`, `Anything.ANYTHING`.

| Oracle text | AST |
|---|---|
| "target creature" | `Quantifier(Exact(1), Target(Battlefield(Permanent(CardType(CREATURE)))))` |
| "target two permanents" | `Quantifier(Exact(2), Target(Battlefield(Permanent(Anything.ANYTHING))))` |
| "destroy target token" | `Quantifier(Exact(1), Target(Battlefield(Token(Anything.ANYTHING))))` |
| "destroy target creature or planeswalker" | `Quantifier(Exact(1), Target(Battlefield(Permanent(AnyOf(List.of(CardType(CREATURE), CardType(PLANESWALKER)))))))` |
| "non-creature non-land permanent" | `Quantifier(Exact(1), Battlefield(Permanent(AllOf(List.of(Not(CardType(CREATURE)), Not(CardType(LAND)))))))` |
| "creatures you control with flying" | `Quantifier(ALL, Battlefield(Permanent(AllOf(List.of(CardType(CREATURE), ControlledBy(YOU), Ability(FLYING))))))` |
| "a red card from your graveyard" | `Quantifier(Exact(1), Graveyard(YOU, Card(Color(RED))))` |
| "counter target spell" | `Quantifier(Exact(1), Target(Stack(Spell(Anything.ANYTHING))))` |
| "copy target spell" (back-ref to created copy) | `Quantifier(Exact(1), Target(Stack(Copy(Anything.ANYTHING))))` |
| "an emblem you control" | `Quantifier(Exact(1), CommandZone(Emblem(ControlledBy(YOU))))` |
| "all Auras attached to it" | `Quantifier(ALL, Battlefield(Permanent(AllOf(List.of(Subtype(AURA), AttachesTo(<it>))))))` |
| "enchanted creature" (subject) | `Battlefield(Permanent(AllOf(List.of(CardType(CREATURE), Enchanted(SELF)))))` |
| "equipped creature" (subject) | `Battlefield(Permanent(AllOf(List.of(CardType(CREATURE), Equipped(SELF)))))` |
| "fortified land" (subject) | `Battlefield(Permanent(AllOf(List.of(CardType(LAND), Fortified(SELF)))))` |
| "another creature" | `Quantifier(Exact(1), Battlefield(Permanent(AllOf(List.of(CardType(CREATURE), OtherObjectSelector(SELF))))))` |
| "other creatures" | `Quantifier(ALL, Battlefield(Permanent(AllOf(List.of(CardType(CREATURE), OtherObjectSelector(SELF))))))` |
| "another player" | `Quantifier(Exact(1), OtherPlayerSelector(<reference, e.g. YOU>))` |
| "any graveyard" (owner slot) | `Graveyard(PlayerSelector.Anyone.ANYONE, Card(Anything.ANYTHING))` |
| "~" | `SelfSelector.SELF` |

## Files

### Create (new files) / Restructure
- `selector/ObjectPropertySelector.java` — **NEW**. Sealed interface with nested `Anything`, `AllOf`, `AnyOf`, `Not`, `Enchanted`, `Equipped`, `Fortified`. Permits the existing standalone axes (listed above) plus the nested arms.
- `selector/ZoneSelector.java` — **rewrite** of the existing empty marker into the sealed interface above with all 7 zone records nested (each carries its `Contents` interface where applicable).
- `selector/ObjectTypeSelector.java` — **rewrite** of the existing empty marker into the sealed interface above with the 7 leaf records nested (`Permanent`, `Token`, `Spell`, `Ability`, `Copy`, `Card`, `Emblem`), each implementing both `ObjectTypeSelector` and the appropriate per-zone `Contents`.

Everything else is either an in-place edit to an existing file or a deletion.

### Update — sealing + reparenting
- `Selector.java` — no permits change.
- `ObjectSelector.java` — permits exactly `ZoneSelector, SelfSelector` (replace the 14-arm list).
- `PlayerSelector.java` — add nested `Anyone` enum and nested `Enchanted` record. Sealing decision: see PlayerSelector section.
- `CharacteristicSelector.java` — change `extends ObjectSelector` → `extends ObjectPropertySelector`. Permits unchanged.

### Update — implements only (one-line change per file)
The standalone axis files keep names and structure; their `implements ObjectSelector` becomes `implements ObjectPropertySelector`:
- `ControlledBySelector`, `OwnedBySelector`
- `StatusSelector`, `ObjectDesignationSelector`
- `ObjectCounterSelector`, `StickerSelector`
- `CombatStatusSelector`, `CombatRoleSelector`
- `AttachesToSelector`
- `OtherObjectSelector` (its `than` slot stays typed as `ObjectSelector`)

The 13 characteristic leaves (`CardTypeSelector`, `ColorSelector`, …) need NO change — they implement `CharacteristicSelector` and inherit its parent switch.

### Delete
- `AttachedObjectSelector.java` — replaced by nested `ObjectPropertySelector.Enchanted`/`Equipped`/`Fortified`.
- `AttachedPlayerSelector.java` — replaced by nested `PlayerSelector.Enchanted`.

### Player-side untouched (this pass)
- `OtherPlayerSelector`, `PlayerCounterSelector`, `PlayerDesignationSelector`, `PlayerRelationSelector`, `PlayerTurnRoleSelector`, `ControllerSelector`, `OwnerSelector` — unchanged. Player-side property umbrella deferred until oracle text demands the composition.

## Record conventions

Every new record (nested or standalone) must include a compact constructor that:
1. **Null-checks** every non-`@Nullable` component with `Objects.requireNonNull`.
2. **Defensively copies** every collection component with `List.copyOf` / `Set.copyOf` / `Map.copyOf` (these also null-check and produce immutable views).

Patterns shown inline in the `ZoneSelector` and `ObjectPropertySelector` skeletons above. Verify the existing `QuantifierSelector`, `OtherObjectSelector`, and `OtherPlayerSelector` follow the rule and patch them if they don't.

## Verification

1. `./mvnw -q -pl mtg-engine -am compile` — clean.
2. `mcp__intellij-index__ide_diagnostics` on each modified file.
3. **Strictness spot checks** — these must NOT compile:
   - `Battlefield(Spell(Anything.ANYTHING))` — `Spell` doesn't implement `Battlefield.Contents`.
   - `Hand(YOU, Permanent(Anything.ANYTHING))` — `Hand.of` is typed `ObjectTypeSelector.Card`; `Permanent` isn't a `Card`.
   - `Permanent(Permanent(Anything.ANYTHING))` — inner slot is `ObjectPropertySelector`; `Permanent` is an `ObjectTypeSelector`, not a property.
   - `Permanent(Battlefield(...))` — same reason.
   - `Quantifier(Exact(1), CardType(CREATURE))` — `CardTypeSelector` is `ObjectPropertySelector`, not `Selector`; can't sit at top.
4. Exhaustive `switch` over `ZoneSelector` lists all 7 nested zone records. Exhaustive switch over `ObjectTypeSelector` lists all 7 leaf records (Permanent, Token, Spell, Ability, Copy, Card, Emblem) — proves the engine's object-class dispatch layer is reachable. Exhaustive switch over `ObjectPropertySelector` lists every permit (standalone + nested).

## Open considerations

- **`Ability` sub-typing** — "counter target activated ability" needs `Stack.Ability` to split. Defer.
- **`PlayerSelector` sealing** — recommended now that two new nested arms are joining; trivial to revert if downstream breaks.
- **Player-side property umbrella** — symmetric `PlayerPropertySelector` would mirror `ObjectPropertySelector`. Defer until a card needs the composition.
- **Naming inside `ObjectPropertySelector`** — `Enchanted` / `Equipped` / `Fortified` are nested records under `ObjectPropertySelector`. Standalone axis files retain their `*Selector` suffix because that's their existing convention. Mixed naming is the explicit intent: nested types follow the host-type's namespace, standalone types follow file-name convention.
