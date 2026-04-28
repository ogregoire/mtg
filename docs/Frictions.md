# Frictions

A running log of recurring frictions hit when extending the oracle-text parser. Each entry describes a structural or design pain point that surfaces repeatedly across cards and batches — not bugs, but design choices that cost time on every new card touching the affected area.

These are observations from agent-driven parser work. They are *symptoms* of underlying design debt; the corresponding fixes live in [Recommendations.md](Recommendations.md).

---

## 1. dot-parse has no backtracking — partial commits become dead ends

**Where it bites:** Any `anyOf` arm that consumes input via a leading `sequence(...)` or `phrase(...)` and then fails on a later sub-parser. The `anyOf` does *not* fall through to the next arm; the partial input is gone, and the outer parser is left expecting whatever the failed inner sub-parser wanted.

**Recent examples:**

- **Cogwork Spy** ("Reveal this card as you draft it. You may look at the next card drafted from this booster pack."): The first sentence's `REVEAL` parser stopped at the SUBJECT boundary, leaving "as you draft it" stranded. The TRIGGERED parser then matched lowercase "as" as its trigger word and tried to interpret "you draft it" as a die effect. Fix required absorbing "as you draft it" as a tail on REVEAL itself.

- **Cylian Sunsinger** ("This creature and each other creature with the same name as it get +3/+3"): `OR_ALTERNATIVE`'s first arm `sequence(QUALIFIER_LIST, TYPE_GROUP, ...)` committed to "noncreature" via QUALIFIER_LIST, then failed when TYPE_GROUP saw the "or" delimiter. `anyOf` never tried the fallback. Fix required a dedicated `NEGATED_QUALIFIER_OR_WITH_OBJECT` parser at a *higher* level that bypasses `OR_ALTERNATIVE` entirely.

- **Firespitter Whelp** ("noncreature or Dragon spell"): Same root cause as Cylian Sunsinger — `QUALIFIER_LIST` consumed "noncreature" and TYPE_GROUP failed on "or".

**Why it matters:** Every card that triggers this bug requires a non-trivial structural fix. The agent must:

1. Diagnose where the partial commit happened (the error message points at the *symptom*, not the cause).
2. Walk up the dispatcher chain to find a level high enough to bypass the committing arm.
3. Sometimes split a guard or restructure a multi-arm dispatcher.

Each instance costs significant agent time and adds a new branch that future contributors must understand.

**Pattern:** any `sequence(p1, p2, ...)` arm in an `anyOf` is a potential trap when `p1` is permissive. Particularly dangerous: `QUALIFIER_LIST`, `SUBJECT`, `SELECTOR`, anything starting with a possessive or pronoun.

---

## 2. Free-text `Predicate` / `description` fields as a parser landing pad

**Where it bites:** Several domain types accept a fallback `String` field that captures "whatever the structural arms didn't match." Examples: `Selector.ThatClause.Predicate(String)`, `Selector.WithClause.HasPredicate(String)`, `Effect.SetCharacteristic.description(String)`, `Effect.Prevent.description(String)`.

When agents encounter a phrase that doesn't fit any existing typed variant, the path of least resistance is to add another `phrase("...").map(Predicate::new)` arm — silently extending free-text capture rather than introducing a typed variant.

**Recent examples:**

- **Erithizon** ("a creature of defending player's choice"): The agent extended `OF_CHOICE_CATEGORY`'s free-text predicate to include "defending player's" as a possessive variant. Result: `Predicate("of defending player's choice")` — a string. The structurally-correct shape would be `OfChoice(PlayerRef.DEFENDING_PLAYER)`. The agent acknowledged this debt but followed existing convention.

- **Grudge Keeper** ("each opponent who voted for a choice you didn't vote for"): The whole vote-divergence relative clause is captured as `Predicate("who voted for a choice you didn't vote for")`. No engine consumer can act on this — the predicate string is opaque. A typed `WhoVotedDifferentlyFrom(Subject)` would be needed.

- **Backslide** ("with a morph ability"): The morph ability isn't in `Ability.StaticKeyword`, so the with-clause fell back to `HasPredicate("a morph ability")`. Engine can't query for "creatures with morph" structurally.

- **Acolyte of Bahamut** ("each turn"): Absorbed as flavor on the Casts controller clause, indistinguishable from "this turn" in the AST. Engine can't tell a once-ever-first from a per-turn-first.

- **Harmony of Nature** ("tapped this way"): New PARTICIPIAL_CLAUSE arm produces `Predicate("tapped this way")`. Same shape as "sacrificed this way", "destroyed this way", etc. — all free text.

**Why it matters:**

1. **Engine consumers can't case-match** on the AST in a way that exercises the structural distinction the oracle text actually carries.
2. **Tests can't verify** the parser captured the right meaning — they can only verify the right *string* came through.
3. **Each new card adds another string variant**, growing the free-text universe without bound. Refactoring the field to a typed enum becomes harder with every addition.
4. **Debuggers see opaque text** when inspecting a Card's AST, instead of named variants.

The CLAUDE.md rule says "free text is a fallback, not a default" and "carve out a typed variant and narrow the free-text fallback over time." In practice, agents almost always extend the fallback because that's the smallest local change.

---

## 3. String fields where enums would do — pre-existing debt that compounds

**Where it bites:** Several record fields are typed as `@Nullable String` because they were introduced before a typed enum existed. Each new card that touches the field reads the existing convention as license to add another string value.

**Recent examples:**

- `Zone.OntoBattlefield.controller` is a String. Possible values currently in the codebase: `"its owner's"`, `"their owner's"`, `"their owners'"` (added by Planar Birth this batch), `"your"`, `"their"`. Should be a typed enum (`PlayerRef`-like) — owner/owners distinction is plurality, which the engine doesn't need; controller distinction (your/their) maps directly to PlayerRef.

- `Effect.UntapLimit.scope` is `@Nullable String` holding "during their untap steps" / "during your untap step". Should be `@Nullable Duration` with a `Duration.DuringStep(StepName, OwnerRef)` variant. The Winter Orb fix added a properly-typed `condition` field but left `scope` as String because retrofitting was out of scope.

- `Subject.WithClause.SameNameAs.reference` is a `String` holding either `"it"`, `"that creature"`, `"that land"`, etc. Should be either a `Subject` (for the demonstrative cases) or a small enum `SelfRefKind { IT, THAT_CREATURE, ... }`.

**Why it matters:** Each touch adds another string value to the de-facto enum. Refactoring becomes a bigger lift each batch. The Plural Possessive case (Planar Birth) is a tell — when pluralizing requires a one-line literal addition, the field is encoding closed-set English variants, which is the exact case the parser-review checklist flags.

---

## 4. Cross-file dependency hierarchy forces inlining and duplication

**Where it bites:** The parser package has a strict layering: `SubjectParsers` < `SelectorParsers` < `ZoneParsers` < `EffectParsers` (rough order). Lower-level parsers can't reference higher-level ones. When a lower-level parser needs functionality that already exists in a higher-level one, the workaround is either to inline a near-duplicate or to lift the shared code to an even-lower-level utility.

**Recent examples:**

- **Patrician Geist** (Casts.fromZone): The "you cast" arm in `CONTROLLER_CLAUSE` (in `SelectorParsers`) needs to parse "from your graveyard". `ZoneParsers.ZONE` is the natural reuse, but `ZoneParsers` sits above `SelectorParsers` in the dependency graph. The agent inlined `phrase("your").then(ZONE_NAME).<Zone>map(z -> new Zone.Named("your", z))` plus three sibling arms — duplicating logic that `ZoneParsers.ZONE` already implements.

- **Ivory Tower** (`WHERE_X_IS`): The "where X is" amount clause exists in *two* places — `CountOfParsers.WHERE_X_IS` (shared) and `DamageEffectParsers.WHERE_X_IS_WITH_DAMAGE` (local fork). When Ivory Tower needed "where X is the number of cards in your hand minus 4," the agent had to update *both* parsers in parallel to keep them consistent. Otherwise the fix would be visible only to gain-life and not to other "where X is" consumers.

- **Cost vs Damage `HALF_LIFE`** (Murderous Betrayal, batch 9): Same shape — fixed last batch by lifting HALF_LIFE to package-private and sharing it. That worked because both files are in the same package and the dependency was naturally one-way. ZoneParsers/SelectorParsers don't have that escape hatch.

**Why it matters:** Duplication hides bugs (one side gets a fix, the other rots). Inlining narrow zone parsers loses the alternation/inflection logic the canonical ZoneParsers handles. Each new card that hits the duplication risks divergence.

---

## 5. Static-init forward references silently NPE

**Where it bites:** `EffectParsers.java` is one giant file with hundreds of `static final Parser<...>` fields, ordered by source-line position. If field A references field B and A is declared first, A sees `null` at class-load time and the eventual parse blows up with an NPE that doesn't point at the cycle.

**Recent example:**

- **Channel** (MAY_PAY_ANY_TIME_FOR_MANA): The new parser wraps the existing `MAY` parser. The agent had to declare `MAY_PAY_ANY_TIME_FOR_MANA` *after* `MAY` in source order to avoid the NPE. There's no compile-time check; the only signal is that tests fail with a NullPointerException during static init.

**Why it matters:** As `EffectParsers.java` grows (currently >4500 lines), the order-dependency graph becomes harder to navigate. Agents adding a new parser frequently land it near related parsers without realizing they've introduced a forward reference, then have to debug an opaque NPE during test runs.

---

## 6. Dispatcher coverage gaps — existing parsers not wired into all entry points

**Where it bites:** Effect parsers have multiple top-level dispatchers: `BASE_EFFECT` (most common), `CLAUSE` (with subject sharing), `MAY`'s flatMap inner list (post-"may"), `objectVerbBody` (chain-body for "X gets +1/+1 and Y"), `playerVerbBody` (for player-actor chains). When a new effect is added, all of these need to be checked. Several batches have shipped with a parser that exists but isn't reachable from a particular context.

**Recent examples:**

- **Kor Outfitter** ("you may attach…"): `Effect.Attach` and the `ATTACH` parser already existed; only missing from `MAY`'s inner dispatcher. One-line registration.

- **On Serra's Wings** ("Enchanted creature is legendary, gets +1/+1, …"): `Effect.SetSupertype` and standalone `SET_SUPERTYPE` parser existed; missing from `objectVerbBody` (the chain-body method). The bug was reachable specifically from "X is legendary, gets …, and has …" multi-effect chains.

- **Jugan, the Rising Star** (last batch): `Effect.DistributeCounters` existed in `BASE_EFFECT` and `CLAUSE`, but was missing from `MAY`. "you may distribute" failed.

**Why it matters:** Without an automated check, every new effect requires manual audit of all dispatchers. The pattern is easy to miss because each dispatcher is in a different region of the file. New cards keep finding the gaps one at a time.

---

## 7. Greedy stop-word lists are hand-curated

**Where it bites:** `WITH_STOP_WORDS` (and similar "stop here" lists) bound free-text predicates by listing words that should never appear *inside* a with-clause. Each new outer-effect verb needs to be added when a card combines that verb with a with-clause-bearing selector.

**Recent example:**

- **Backslide** ("Turn target creature with a morph ability face down."): The free-text with-predicate greedily consumed "a morph ability face down", swallowing the outer `TURN_FACE_DOWN` verb. Fix: add `"face"` to `WITH_STOP_WORDS`.

**Why it matters:** Future verbs that share words ("turn face up", "tap to cast", "pay to activate") may all hit the same class of issue, requiring more stop-word entries. Each addition risks over-stopping (rejecting a legitimate predicate that contains the word). The list grows monotonically; it has no natural bound.

---

## 8. AmountMatcher gaps — indefinite article and other shorthand forms

**Where it bites:** `AMOUNT_MATCHER` parses numeric amounts plus comparators ("3 or more cards", "fewer than 5 lands"). It doesn't accept the indefinite article "a"/"an" as a synonym for "at least one".

**Recent example:**

- **Imaginary Pet** ("if you have a card in hand"): `CARDS_IN_HAND_CONDITION` consumed "if you have" then asked AMOUNT_MATCHER for a number. AMOUNT_MATCHER doesn't know "a"; the parse failed. Fix added an arm: `phrase("[a|an]").followedBy(phrase("card in hand")).thenReturn(AtLeast(exact(1)))` — but that's a special-case workaround for one specific condition, not a general AMOUNT_MATCHER extension.

**Why it matters:** Other AMOUNT_MATCHER consumers (`CARDS_IN_LIBRARY_CONDITION`, `ANY_ZONE_HAS_CARDS_CONDITION`, with-clause comparators, etc.) likely have the same gap. Each one will need its own special-case arm until AMOUNT_MATCHER itself is extended.
