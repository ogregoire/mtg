# Refactor `EffectParsers.MAY`: split into `MayDo` (effect) and `MayPay` (cost), each with its own parser

## Context

`EffectParsers.MAY` is the most incomprehensible parser in the oracle
module: 147 lines, 11 distinct constructed `Effect.*` subtypes inlined
as parser arms (`EffectParsers.java:4658–4805`). It exists because oracle
text wraps optional actions in `"<player> may <verb>"`, captures the
player up front, then re-injects it into every action that has an actor
field.

The current model conflates two rules-distinct things:

1. **Optional effect** — "you may **draw a card**", "you may **destroy
   target creature**". The verb is an effect-imperative; the may-chooser
   becomes the actor of that effect.
2. **Optional cost payment** — "you may **pay {2}**", "you may
   **sacrifice a creature**". The verb is a cost-imperative
   (rule 117.6); the may-chooser is the cost-payer.

Today both produce `Effect.Optional(Effect.Pay(YOU, …))` or
`Effect.Optional(Effect.Sacrifice(YOU, …))`, which means `Effect.Pay`
and `Effect.Sacrifice` exist *only* to give MAY's body something to
return — they have no construction sites outside MAY (verified via
grep). They're pseudo-effects that wrap costs.

The proposal: model the rules distinction faithfully.

- **`Effect.MayDo`** (rename from `Effect.Optional`) wraps an `Effect`
  — the optional effect-action.
- **`Effect.MayPay`** (new) wraps a `Cost` — the optional cost
  payment.
- A **cost-side `MAY` parser** in `CostParsers` parses the
  cost-imperative form. An **effect-side `MAY` parser** in
  `EffectParsers` parses the effect-imperative form. The dispatcher
  tries cost-side first (its verbs are a more specific vocabulary).
- `Effect.Pay` and `Effect.Sacrifice` go away. Their construction
  sites collapse into `Effect.MayPay`'s `Cost` payload, which
  `CostParsers.COST_EXPRESSION` already handles natively.
- The pesky "rebind only if YOU" `withActor` mechanism is needed
  *only* for the `MayDo` path — `MayPay` doesn't need it because
  costs don't carry a player field; the chooser on the wrapper is
  authoritative.

## Final shape

### Domain (`Effect.java`)

```java
public sealed interface Effect {

    /// "<chooser> may <effect>. [If <chooser> does, <ifDone>]?" —
    /// optional effect-action. Renamed from the older `Effect.Optional`
    /// to disambiguate from `Effect.MayPay` (the optional cost form).
    record MayDo(Subject chooser, Effect action, @Nullable Effect ifDone) implements Effect {
        public MayDo(Subject chooser, Effect action) {
            this(chooser, action, null);
        }
        public MayDo withIfDone(Effect ifDone) {
            return new MayDo(chooser, action, ifDone);
        }
    }

    /// "<chooser> may <cost>. [If <chooser> does, <ifDone>]?" —
    /// optional cost payment. The cost is paid by `chooser`; oracle
    /// text never names a different payer, so the chooser is also
    /// the cost-payer at resolution time. The `Cost` itself carries
    /// no player attribution (consistent with `Cost`'s shape — costs
    /// describe *what* is paid, not *who* pays).
    record MayPay(Subject chooser, Cost cost, @Nullable Effect ifDone) implements Effect {
        public MayPay(Subject chooser, Cost cost) {
            this(chooser, cost, null);
        }
        public MayPay withIfDone(Effect ifDone) {
            return new MayPay(chooser, cost, ifDone);
        }
    }

    /// Returns this effect with its primary actor set to `actor`,
    /// **but only if the current actor is the placeholder
    /// `Subject.PlayerRef.YOU`**. Effects whose actor was bound to a
    /// non-YOU subject at parse time (e.g., a "have <target player>
    /// <verb>" causative) are returned unchanged. Effects with no actor
    /// field (Destroy, Tap, AddMana, …) inherit the default no-op.
    ///
    /// Used by [Effect.MayDo]'s parser to push the may-chooser into the
    /// inner action without clobbering an explicitly-bound inner actor.
    /// Not used by [Effect.MayPay] (Cost carries no actor).
    default Effect withActor(Subject actor) {
        return this;
    }

    record Draw(Subject player, Amount amount, @Nullable Effect xDefinition) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new Draw(actor, amount, xDefinition) : this;
        }
        // …existing constructors / withers unchanged
    }

    record Discard(Subject player, Discarded discarded) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new Discard(actor, discarded) : this;
        }
    }

    record Mill(Subject player, Amount amount) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new Mill(actor, amount) : this;
        }
    }

    record Reveal(Subject player, Subject target, boolean atRandom) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new Reveal(actor, target, atRandom) : this;
        }
    }

    record GainLife(Subject player, Amount amount) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new GainLife(actor, amount) : this;
        }
    }

    record LoseLife(Subject player, Amount amount) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new LoseLife(actor, amount) : this;
        }
    }

    record PlayAdditionalLands(Subject player, Amount amount, @Nullable Duration duration) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new PlayAdditionalLands(actor, amount, duration) : this;
        }
    }

    record CreateToken(@Nullable Subject creator, …) implements Effect {
        @Override public Effect withActor(Subject actor) {
            // CreateToken's creator defaults to null — treat null as the
            // placeholder, identical to the YOU placeholder semantics.
            return creator == null || isYou(creator) ? withCreator(actor) : this;
        }
    }

    record LookAt(Subject player, …) implements Effect {
        @Override public Effect withActor(Subject actor) {
            return isYou(player) ? new LookAt(actor, …) : this;
        }
    }

    /// Helper: is this subject the parser's YOU placeholder?
    private static boolean isYou(Subject s) {
        return s instanceof Subject.Player p
                && p.ref() == Subject.PlayerRef.YOU;
    }

    // …rest of existing variants unchanged.
}
```

### Domain deletions

- `Effect.Optional` → renamed to `Effect.MayDo` (one rename via
  `mcp__intellij-index__ide_refactor_rename` on the record class). The
  `withIfDone` accessor name stays.
- `Effect.Pay` → **deleted**. Only construction sites are MAY's two
  arms (lines 4669, 4716). Both migrate to `Effect.MayPay` whose cost
  payload comes from `CostParsers.COST_EXPRESSION`.
- `Effect.Sacrifice` (player-less / actor-less form) → **deleted as an
  Effect variant**. Construction sites: `EffectParsers.java:4647` (the
  `ALTERNATIVE_CASTING_COST`'s body — keeps using `Cost.SacrificePermanent`
  directly), `:4720` (MAY's body — migrates to `MayPay`), and
  `RemovalEffectParsers.java:126` (`SACRIFICE` parser — emits
  `Effect.Sacrifice(YOU, what)`). The last one *is* a real top-level
  effect ("Sacrifice a creature." as a sentence in a spell body), so
  it stays — `Effect.Sacrifice` does NOT go away entirely; only its
  use *as a may-target* migrates. Re-checking: see "Risks" below.

  → After re-examination: keep `Effect.Sacrifice`. It has a legitimate
  non-MAY construction site (RemovalEffectParsers.SACRIFICE for plain
  "Sacrifice X." sentences). MAY's `phrase("Sacrifice").then(SUBJECT)`
  arm migrates to MayPay using `Cost.SacrificePermanent`.

### Parser (`CostParsers.java`)

```java
/// "<chooser> may <cost>" — optional cost payment in an effect body
/// (Inheritance: "you may pay {3}. If you do, draw a card."; Browbeat:
/// "Any opponent may have ~ deal 5 damage to them. If no one does,
/// target player draws three cards."). Distinct from the cost-line
/// form (which never uses "may" — costs in cost lines are mandatory).
///
/// Reuses [#COST_EXPRESSION] so every cost-imperative the engine
/// recognises ("Pay {2}", "Pay 3 life", "Sacrifice a creature",
/// "Discard a card", "Tap a creature", …) is automatically may-able.
/// The optional `IF_DO_CONTINUATION` / `WHEN_DO_CONTINUATION`
/// follow-ups attach via withers on `Effect.MayPay`.
public static final Parser<Effect.MayPay> MAY = sequence(
        SubjectParsers.PLAYER_SUBJECTS.followedBy(word("may")),
        COST_EXPRESSION,
        Effect.MayPay::new)
    .optionallyFollowedBy(EffectParsers.IF_DO_CONTINUATION,   Effect.MayPay::withIfDone)
    .optionallyFollowedBy(EffectParsers.WHEN_DO_CONTINUATION, Effect.MayPay::withIfDone);
```

Wait — `EffectParsers.IF_DO_CONTINUATION` is private. We need to expose it
package-private (drop `private`) or move the IF/WHEN-DO continuations to
a shared location (e.g., a new `OracleContinuations` utility class) so
both `EffectParsers.MAY` and `CostParsers.MAY` can attach them.

The cleanest move: drop `private` on `IF_DO_CONTINUATION` and
`WHEN_DO_CONTINUATION` in `EffectParsers` (they become package-private,
which is fine since `CostParsers` is in the same package). One-line
visibility change.

### Parser (`EffectParsers.java`)

```java
/// "<chooser> may <effect>" — optional effect-action. The chooser is
/// captured before the verb and pushed into the inner action via
/// [Effect#withActor]. The body is the full [#BASE_EFFECT] dispatcher
/// so any verb-imperative the engine recognises is automatically
/// may-able. Causative `have <other> <verb>` effects bind their own
/// actor at parse time; `withActor` is a no-op for them.
private static final Parser<Effect.MayDo> MAY_DO = sequence(
        SubjectParsers.PLAYER_SUBJECTS.followedBy(word("may")),
        BASE_EFFECT,
        (chooser, action) -> new Effect.MayDo(chooser, action.withActor(chooser)))
    .optionallyFollowedBy(IF_DO_CONTINUATION,   Effect.MayDo::withIfDone)
    .optionallyFollowedBy(WHEN_DO_CONTINUATION, Effect.MayDo::withIfDone);

/// Top-level `<chooser> may …` dispatcher. Tries cost-imperative
/// (`MayPay`) before effect-imperative (`MayDo`) — cost verbs are a
/// more specific vocabulary, so cost-form wins for shared verbs
/// like "discard" / "exile" when followed by a cost-shape (e.g.,
/// "may discard a card or pay {2}" — Anthropede).
static final Parser<Effect> MAY = anyOf(CostParsers.MAY, MAY_DO);
```

`MAY` is registered in the existing `EFFECT` / `CLAUSE` dispatchers
exactly where the old MAY was. No callers of the field need to update
beyond accepting the wider `Parser<Effect>` (was `Parser<Effect.Optional>`
— the field type widens to the sealed parent).

### `BASE_EFFECT` additions

To absorb MAY's old "have <subject> <verb>" arms:

- **`HAVE_CAUSATIVE`** — new top-level parser combining the eight
  `Have …` arms currently in MAY (Mill, Draw, deal-damage, enter-as-copy,
  assign-damage-as-unblocked, objectVerbBodyWithDuration, fight, block).
  Each arm binds its inner actor at parse time. Registered in
  `BASE_EFFECT` near the top so it isn't shadowed.

That's the only `BASE_EFFECT` registration this refactor needs. `PAY`
and `SACRIFICE` do **not** get registered there — they're cost concepts,
not effect concepts; they live inside `Effect.MayPay`'s payload.

## Critical files

- `mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/domain/Effect.java`
  — rename `Optional` → `MayDo` (via IDE); add `MayPay` record; add
  `withActor` default + 9 overrides; add `isYou` private helper. Net
  +~60 lines.
- `mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/parser/CostParsers.java`
  — add static `MAY` parser at the bottom of the file.
- `mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/parser/EffectParsers.java`
  — three regions:
  - `MAY` collapses to ~6 lines (the dispatcher).
  - `MAY_DO` is the new "effect-imperative may" parser (~7 lines).
  - `HAVE_CAUSATIVE` extracted from old MAY's 8 `Have …` arms.
  - `IF_DO_CONTINUATION` / `WHEN_DO_CONTINUATION`: drop `private`.
- `mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/parser/RemovalEffectParsers.java`
  — verify `SACRIFICE_NO_PLAYER` is accessible (already package-private).
- No test changes expected. AST shapes change (`MayDo` instead of
  `Optional`, `MayPay` for the cost form), but the only tests that
  match on these are absent (verified — grep found zero in
  `src/test/java`).

## IntelliJ Index MCP usage

- **`mcp__intellij-index__ide_refactor_rename`** on
  `Effect.Optional` → `Effect.MayDo`. This is a real symbol rename
  with consumers in two files (Javadoc references in
  `TapEffectParsers.java`, `Condition.java`) plus the parser sites
  that use `Effect.Optional::new` / `Effect.Optional::withIfDone`.
  IDE rename catches everything; manual find-replace would miss the
  Javadoc `[Effect.Optional]` link references.
- **`mcp__intellij-index__ide_find_references`** on the new types
  (`Effect.MayDo`, `Effect.MayPay`) post-refactor to confirm the
  consumer surface is exactly the parser sites and tests.
- **`mcp__intellij-index__ide_diagnostics`** on each touched file
  after edits.

## Implementation steps

1. **Rename `Effect.Optional` → `Effect.MayDo`** via IDE refactor.
   Single rename; tests / parsers / Javadoc all updated by the IDE.
   Compile (silent success).
2. **Add `withActor` default + overrides** on `Effect`. Add
   `isYou(Subject)` helper. Add a focused unit test
   `EffectWithActorTest` with the four pinning cases (YOU rebinds,
   non-YOU doesn't, no-actor effect is a no-op, CreateToken's null
   creator rebinds). Compile + run that one test.
3. **Add `Effect.MayPay`** record with constructor variants and
   `withIfDone` wither. Compile.
4. **Drop `private`** on `IF_DO_CONTINUATION` and
   `WHEN_DO_CONTINUATION` in `EffectParsers`. Compile.
5. **Extract `HAVE_CAUSATIVE`** from MAY's body (the eight `Have …`
   arms). Declare it as a `static final Parser<Effect> HAVE_CAUSATIVE`
   near the top of the dispatchers section. Register it in
   `BASE_EFFECT` near the top (before any arm that could shadow
   `Have`). Compile + spot-check causative cards: `./mtg parse
   "Goblin Arsonist" "Aether Charge" "Mirror Image" "Quicksilver
   Gargantuan" "Deathcoil Wurm" "Undead Executioner" "Somberwald Stag"
   "Giant Ambush Beetle"`.
6. **Add `CostParsers.MAY`** parser at the bottom of `CostParsers.java`.
   Compile.
7. **Replace `EffectParsers.MAY`**:
   - Add private `MAY_DO` (the effect-imperative form).
   - Replace the existing `MAY` field with the dispatcher
     `anyOf(CostParsers.MAY, MAY_DO)`. The field type widens from
     `Parser<Effect.Optional>` to `Parser<Effect>` — verify all
     registration sites (CLAUSE, BASE_EFFECT?) accept the wider type
     via covariance; if any explicitly typed local needs updating,
     update it.
   - Delete the old MAY body (the 147-line flatMap + anyOf).
   - Delete the old `Effect.Pay` references in MAY (now subsumed by
     MayPay).
   - Compile + spot-check ~25 cards across the variant space (see
     "Verification" below).
8. **Delete `Effect.Pay`** record from `Effect.java`. Use
   `mcp__intellij-index__ide_find_references` first to confirm zero
   non-MAY uses (already verified by grep — but confirm via IDE so the
   refactor is closed). Compile.
9. **Verify**:
   - `./mvnw -pl mtg-engine test` — all 3252 tests pass.
   - Spot-check cards (see below).
   - `./mtg parse all` — no regression from 61.83%.
10. **Update `parser-index.md`**:
    - §2 Effect: `Optional` → `MayDo`; add `MayPay`; remove `Pay`.
      Note `withActor`.
    - §3 EffectParsers: rewrite `MAY` entry — short dispatcher;
      describe `MAY_DO`, `HAVE_CAUSATIVE`. Reference
      `CostParsers.MAY`.
    - §3 CostParsers: add `MAY` entry.

## Verification

- `./mvnw -q -pl mtg-engine compile -DskipTests` after each step.
- Pinning unit test for `withActor` semantics (4 cases). One test
  class, ~20 lines.
- `./mvnw -pl mtg-engine test` — 3252 green.
- Spot-check 25 cards:
  - **Pure may-effect (MayDo)**: Tolarian Kraken, Lys Alana
    Huntmaster, Chancellor of Tales, Puresight Merrow, Bloodthorn
    Flail.
  - **May-cost (MayPay)**: Inheritance ("may pay {3}"), Anthropede
    ("may discard a card or pay {2}" — Cost.AnyOf), Blood Crypt
    ("may pay 2 life"), Benthic Criminologists ("may sacrifice an
    artifact"), Browbeat ("any opponent may have ~ deal 5 damage to
    them" — wait, this is "may have" — that's MayDo with a
    have-causative inner; not MayPay).
  - **Have-causative (BASE_EFFECT.HAVE_CAUSATIVE inside MayDo)**:
    Goblin Arsonist, Aether Charge, Mirror Image, Quicksilver
    Gargantuan, Deathcoil Wurm, Undead Executioner, Somberwald
    Stag, Giant Ambush Beetle, Jace's Erasure (have-target-mill).
  - **Non-YOU may-actor**: Browbeat ("any opponent may"), some
    "target player may" card if findable.
  - For each, the AST shape changes (`MayDo[chooser=…, action=…,
    ifDone=…]` or `MayPay[chooser=…, cost=…, ifDone=…]`) but the
    *information content* is preserved. Manual eyeball of each.
- `./mtg parse all` — no regression from 61.83%.

## Risks and known sharp edges

- **`Effect.Sacrifice` survives**. The `RemovalEffectParsers.SACRIFICE`
  parser produces it for plain "Sacrifice X." sentences in spell
  bodies (not under MAY). MAY's `phrase("Sacrifice").then(SUBJECT)`
  arm goes away, but the record stays. Be careful not to delete it
  during cleanup.
- **`withActor` "rebind only if YOU" semantic** — same caveat as
  before. Mitigation: explicit doc + pinning unit test.
- **`MAY_DO` calls `withActor(chooser)` on the inner**. For most cards,
  chooser=YOU, so `withActor(YOU)` is a no-op (the placeholder *is*
  YOU). The actual behavior change happens for cards like
  "target player may draw a card" — but those are rare. Spot-check
  one if findable.
- **Anthropede edge case**. "you may discard a card or pay {2}" —
  today's MAY has a special `COST_EXPRESSION.suchThat(AnyOf)` arm.
  Under the new model: `CostParsers.MAY` handles it natively (because
  `COST_EXPRESSION` already does the "or" disjunction → `Cost.AnyOf`).
  The parse path becomes: `MayPay(chooser=YOU, cost=Cost.AnyOf([
  Cost.DiscardCard, Cost.Mana("{2}")]), ifDone=Destroy(Room))`. Verify
  this parse explicitly.
- **Dispatch order in `MAY = anyOf(CostParsers.MAY, MAY_DO)`**.
  Cost-side first because cost vocabulary is more specific (`Pay`,
  `Sacrifice`, `Tap`, etc. start cost imperatives unambiguously).
  Verify with a card whose verb is shared between the two paths
  (e.g., "Discard"). If the cost-side wins inappropriately for a
  pure-effect "may discard" card, swap order or add a guard.
- **Static-init order**. `CostParsers.MAY` references
  `EffectParsers.IF_DO_CONTINUATION` and `WHEN_DO_CONTINUATION`. We
  need to confirm CostParsers can see these — same package, fine
  visibility-wise, but the static-init order has CostParsers loaded
  *before* EffectParsers (CostParsers is in the dependency hierarchy
  upstream of EffectParsers). That means at CostParsers class init,
  the EffectParsers continuation fields might be null (uninitialized).
  → Mitigation: declare CostParsers.MAY *lazily* via `Parser.define`
  (forward reference) or move the continuations into a leaf utility
  class (`OracleContinuations`) declared above both. Cleaner fix is
  the latter: extract `IF_DO_CONTINUATION` and `WHEN_DO_CONTINUATION`
  into a new `OracleContinuations` package-private utility class with
  no upward deps, declared at the leaf level of the dependency graph.
- **Engine consumers**: `Effect.Optional` has zero structural consumers
  in the engine today (only Javadoc references). `Effect.Pay` and
  `Effect.Sacrifice` likewise have zero engine pattern-match sites.
  Tests touch none of these. No engine code needs updating.
- **`CostParsers.MAY` returns `Parser<Effect.MayPay>`**, not
  `Parser<Cost>`. That's because the result of "may pay X" is an
  Effect (it lives in an effect body), not a Cost. The cost-side MAY
  is a *parser* in CostParsers but its result is Effect.MayPay, which
  references a Cost. This mild type-mismatch is intentional — the
  alternative would be a `Cost.Optional(Cost)` wrapper that's only
  used in this one place, and then a separate Effect parser that
  unwraps it. Single Effect.MayPay is simpler.

## Out of scope

- Renaming `Effect.MayDo` further (e.g., to `Effect.OptionalEffect`).
- Promoting `Effect.MayPay` into the cost-line grammar (cost lines
  never use "may"; costs there are mandatory).
- Refactoring `CONTROLLER_CLAUSE` and `MODIFY_COST` (other long
  parsers identified earlier).
- Folding `IF_DO_CONTINUATION` / `WHEN_DO_CONTINUATION` into a single
  parser — they have distinct semantics (immediate vs delayed
  trigger).
