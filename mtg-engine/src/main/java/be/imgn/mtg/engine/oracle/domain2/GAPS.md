# `domain2` parity gaps vs. `domain`

Audit (2026-05-03) of what `oracle.domain2` is missing relative to the full `oracle.domain` selector model. The two have different shapes — `domain.Selector` is a flat record with parallel slots (`qualifiers`, `withClauses`, `thatClauses`, `controller`, `zone`); `domain2` enforces a strict envelope `Quantifier → Target? → Zone → ObjectType → Property` and pushes axis composition into `ObjectPropertySelector`. The list below is the conceptual diff.

For each gap, "**what it covers**" gives an example oracle phrase so you can see whether it's worth filling on demand or proactively.

---

## A. Empty marker types — no concrete shape yet

These exist in `domain2` as `non-sealed interface` placeholders with no records inside, so they cannot be constructed:

| `domain2` marker | `domain` equivalent | What's missing |
|---|---|---|
| `CardTypeSelector` | `SingleType.OfCard(CardType)` / `TypeMatcher.IsCardType` | record holding a `CardType` |
| `SubtypeSelector` | `SingleType.OfSubtype(Subtype)` / `TypeMatcher.IsSubtype` | record holding a `Subtype` |
| `SupertypeSelector` | `TypeMatcher.IsSupertype(Supertype)` | record holding a `Supertype` |
| `NameSelector` | `WithClause.Body.HasName(String)` | record holding a name |
| `ColorSelector` | `Qualifier.Colors(ColorMatcher)` | full `ColorMatcher` taxonomy (see F); base `Color` enum exists |
| `PowerSelector`, `ToughnessSelector` | `Qualifier.PtQualifier(PtValue)` / `WithClause.Body.PtComparison` | concrete shapes for fixed value + comparison |
| `ManaCostSelector` | `WithClause.Body.HasManaValue(AmountMatcher)` | record holding an `AmountMatcher` |
| `AbilitySelector` | `WithClause.Body.HasAbility(Ability)` | record holding an `Ability` |
| `StatusSelector` | `Qualifier.Status` (enum) | record holding an `ObjectStatus`; base `ObjectStatus` enum exists (see B) |
| `ObjectCounterSelector` | (was free-text in `domain`) | counter-type + amount predicate |
| `PlayerCounterSelector` | (none in `domain` yet) | symmetric to `ObjectCounter` |
| `StickerSelector` | (none in `domain` yet) | sticker kind |
| `RulesTextSelector` | `WithClause.Body.HasPredicate(String)` (free text) | structured form |
| `ColorIndicatorSelector`, `LoyaltySelector`, `DefenseSelector` | not in `domain` | concrete shape on demand |

---

## B. `Status` enum values not modeled

`domain.Selector.Qualifier.Status` lumps multiple concepts under one enum. `domain2` splits them:

- **Pure CR 110.5 status** (`TAPPED`, `UNTAPPED`, `FLIPPED`, `UNFLIPPED`, `FACE_UP`, `FACE_DOWN`, `PHASED_IN`, `PHASED_OUT`) — now covered by `ObjectStatus` enum. `StatusSelector` still needs a concrete record holding it.
- **Resolution-history tags:** `EXILED`, `MILLED`, `DRAWN`, `DISCARDED`, `REVEALED` — no `domain2` type yet. These aren't permanent status per CR 110.5 but markers placed by recent events; likely belong as their own predicate type or inside an `ObjectPropertySelector` arm distinct from `StatusSelector`.
- **Counter-derived:** `SUSPENDED` (`SuspendCounter > 0`), `TRANSFORMED` (DFC face status). Not modeled.
- **Designation overlap:** `COMMANDER`, `NONCOMMANDER`, `RING_BEARER` — already covered by `ObjectDesignation`.
- **Positional:** `LAST`, `FIRST`, `TOP` — no `domain2` type yet.

---

## C. Qualifier concepts not in `domain2`

- **`AbilitySource` (`ACTIVATED`, `TRIGGERED`)** — distinguishes activated vs triggered abilities on the stack ("counter target activated ability"). Already noted as deferred for `Stack.Ability` sub-typing.
- **`PtQualifier(PtValue)`** — fixed P/T like "1/1 creature" (Aegis of the Meek).
- **`PlayerRole(PlayerRef)`** — narrows a `PLAYER` game-object in heterogeneous target lists ("artifact, creature, planeswalker, or opponent").

---

## D. WithClause refinements not in `domain2`

- `HasManaValue(AmountMatcher)` — needs `AmountMatcher` (see F).
- `SameManaValueAs(reference)` — equality with another object's MV (Sanguine Praetor).
- `SameNameAs(reference)` — name-equality against a referent (Wake of Destruction).
- `HasManaValueOfChosenQuality()` — back-ref to a `ChooseQuality` effect (Extinction Event).
- `HasChosenName()` — back-ref to a `ChooseCardName` effect (Declaration of Naught).
- `PtComparison(Aspect, Comparator, reference)` — P/T compared to a dynamic value ("with power greater than or equal to your life total").
- `HasAnyAbility(List<Ability>)` — composable via `AnyOf` once `AbilitySelector` has concrete shape.

---

## E. ThatClause restrictions not in `domain2`

- `ReferencedType` — "of that type" back-ref to a `ChooseType` (Distant Melody).
- `FromSourceOfType(CardType)` — "from a[n] [card-type] source" on ability targets (Rust).
- `NamedAs(name)` — name match in that-clause position (Powerstone Shard).
- `HasCast(Selector)` — "who has cast X this turn" on a player subject (Ethersworn Canonist).
- `DidNotDiscardThisWay(Selector)` — back-ref to discard events (Strongarm Tactics).

---

## F. Missing supporting types

- **`ColorMatcher`** — boolean tree over colors with `Is(Color)`, `Not(Color)`, `Colorless`, `Multicolored`, `Monocolored`, `Any`, `All`. `domain2`'s `ColorSelector` is an empty marker.
- **`AmountMatcher`** — `AtLeast(n)`, `AtMost(n)`, `Exactly(n)`, `InRange(min, max)`. Distinct from `Amount` (which *states* a count); needed for "mana value 5 or greater", "with power 3 or less", condition predicates. `domain2` has `Amount` only.

---

## G. ControllerClause shapes not in `domain2`

`ControlledBySelector` and `OwnedBySelector` cover `Controls` / `Owns`. Negation is covered by `NotPropertySelector`. Not yet modeled:

- **`CastBy(PlayerRef, fromZone?)`** — "spells you cast" / "spells you cast from your graveyard".
- **`DiscardedBy(PlayerRef)`** — "cards you've discarded this turn" (Change of Fortune).
- **`Attacking(PlayerRef)`** — "for each opponent you're attacking" (Astral Confrontation).

---

## H. Top-level composition — verified non-gap

`domain.SelectorExpression` exists as an `Or` of full `Selector`s, but oracle text doesn't actually emit cross-zone or cross-object-class top-level disjunctions in the form I initially claimed ("destroy target creature or target planeswalker" doesn't appear). The real form is the type-level disjunction "target creature or planeswalker", which is single-target with a property-level `AnyOf` and is already covered by `AnyOfPropertySelector` inside `Permanent`:

```
Quantifier(Exact(1), Target(Battlefield(Permanent(
    AnyOf(CardType(CREATURE), CardType(PLANESWALKER))))))
```

No `SelectorExpression`-equivalent layer is needed in `domain2`.

---

## I. Conscious omissions — not gaps to fill

These are deliberate cleanups in the new model:

- **Free-text fallbacks** — `WithClause.Body.HasPredicate(String)`, `ThatClause.Predicate(String)`, `Qualifier.CombatStatus(String)`. `domain2` uses typed enums (`CombatStatus`) and forces structured representation.
- **Quantifier collapses** — `One` / `The` → `Amount.Exact(1)`; `All` / `Each` / `Every` → `StandardQuantifier.ALL`; `Another` / `Other` → `OtherObjectSelector` + Quantifier.
- **`SingleType` / `TypeExpression` parser-transient nodes** — replaced by the strict envelope.
- **`Historic` / `Outlaw` / `NegatedOutlaw`** — these are syntactic shorthands the parser expands into the appropriate `AnyOf` of characteristic selectors (Historic = `AnyOf(Supertype(LEGENDARY), CardType(ARTIFACT), Subtype(SAGA))`; Outlaw = `AnyOf(Subtype(ASSASSIN), Subtype(MERCENARY), Subtype(PIRATE), Subtype(ROGUE), Subtype(WARLOCK))`). No dedicated `domain2` type needed.
- **`OfEach(CoverageAxis)`** — "of each basic land type", "of each color". Handled by the parser as a coverage predicate; not a `domain2` type.
- **`SelfName` as a type-slot noun** — re-read of the example. "for each other attacking ~" doesn't put `~` in the type slot — `~` is the *target* of the attack ("attacking [~]" is part of the property), and the noun is "other [creatures]". The `SelfSelector` reference there is inside a `CombatStatus`-style "attacking X" predicate, not at the type-slot position. No new top-level shape needed.

---

## Priority hint

To make `domain2` actually constructible for the most-common oracle-text shapes, the highest-leverage additions, in order:

1. **F** — `ColorMatcher`, `AmountMatcher` (unlock `ColorSelector`, `ManaCostSelector`, future P/T comparisons).
2. **A** — concrete shape for `CardTypeSelector`, `SubtypeSelector`, `SupertypeSelector`, `NameSelector`, `AbilitySelector` (one-line records each).
3. **B** — `Status` enum.
4. **C/D/E/G/H** — fill on demand per card.
