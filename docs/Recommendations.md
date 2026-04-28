# Recommendations

Concrete one-time follow-ups that would systematically address the patterns documented in [Frictions.md](Frictions.md). Each is independent of any single card and pays back across many future batches.

These are not in scope for any individual batch — they're cleanup work that should happen between batches, ideally tracked as standalone tasks.

---

## 1. Replace `String` controller / scope fields with typed enums

**Target:** `Zone.OntoBattlefield.controller` and `Effect.UntapLimit.scope`.

**Why:** Both fields hold closed-set English variants masquerading as free text. Each new card adds another value — recently `"their owners'"` (Planar Birth). The structure is already an enum; the typing just hasn't been formalized.

**Approach for `Zone.OntoBattlefield.controller`:**

1. Introduce `Zone.OntoBattlefield.Controller` enum with values `OWNER`, `OWNERS` (singular vs plural is a card-text gloss; engine treats them identically — drop the distinction or keep as a separate boolean), `YOU`, `THEY`, `IT`. Or, more aggressively, reuse `Subject.PlayerRef` directly with an `IS_OWNER` extension.
2. Update the parser arms in `ZoneParsers.UNDER_CONTROL` to map each phrase to the typed value rather than passing through the raw String.
3. Migrate engine consumers (control assignment when a permanent enters) to switch on the enum.

**Approach for `Effect.UntapLimit.scope`:**

1. The semantics are "during [owner-ref]'s [step] step". This is structurally a `Duration.DuringStep(StepName step, OwnerRef owner)` variant.
2. `Duration` already exists with `Fixed` (THIS_TURN, etc.). Add a `DuringStep` variant.
3. Update `UNTAP_LIMIT`'s scope parser to produce the typed value; remove the String field.

**Effort:** ~2 hours each. Mostly mechanical — search for all `OntoBattlefield(` and `UntapLimit(` constructor sites, switch them to the new typed values.

**Friction addressed:** [Frictions §3](Frictions.md#3-string-fields-where-enums-would-do--pre-existing-debt-that-compounds).

---

## 2. Carve `Selector.ThatClause.OfChoice(PlayerRef)` out of the free-text "of X's choice" predicate

**Target:** Replace `Predicate("of <possessive> choice")` with a typed `OfChoice` variant.

**Why:** "Of an opponent's choice" / "of your choice" / "of defending player's choice" are not free text — they specify *who chooses* the target. This is engine-relevant: the engine needs to query the right player at resolution time. Today the engine has to string-match the predicate, which is brittle.

**Approach:**

1. Add `Selector.ThatClause.OfChoice(@Nullable Subject.PlayerRef chooser)` to the sealed `ThatClause` hierarchy. `null` represents "your choice" (default actor).
2. In `SelectorParsers`, replace the "of [poss] choice" arm in `OF_CHOICE_CATEGORY`:
   - `"of your choice"` → `OfChoice(YOU)` (or `null`)
   - `"of their choice"` → `OfChoice(THEY)`
   - `"of an opponent's choice"` → `OfChoice(AN_OPPONENT)`
   - `"of defending player's choice"` → `OfChoice(DEFENDING_PLAYER)`
3. Engine consumers that previously string-matched the predicate switch to a sealed-type case match.

**Effort:** ~1-2 hours. Limited blast radius because few engine consumers actually act on this predicate today.

**Friction addressed:** [Frictions §2](Frictions.md#2-free-text-predicate--description-fields-as-a-parser-landing-pad).

---

## 3. Unify `WHERE_X_IS` parsers across `CountOfParsers` and `DamageEffectParsers`

**Target:** Eliminate the duplicate `WHERE_X_IS` (shared) and `WHERE_X_IS_WITH_DAMAGE` (local fork in DamageEffectParsers).

**Why:** Ivory Tower required parallel updates to both. Future cards will keep encountering the same drift risk. The local fork exists because `DamageEffectParsers` needs `DAMAGE_DEALT_THIS_TURN` integration, which `CountOfParsers` doesn't have access to.

**Approach:**

1. Lift `DAMAGE_DEALT_THIS_TURN` to a lower level (or a shared utility class) so `CountOfParsers.WHERE_X_IS` can include it as an arm.
2. Remove `WHERE_X_IS_WITH_DAMAGE`; replace its callers with `CountOfParsers.WHERE_X_IS`.
3. Verify that the order of arms preserves the existing dispatch (e.g., damage-dealt-this-turn must precede property-of-amount in the unified parser).

**Alternative:** Promote `WHERE_X_IS_WITH_DAMAGE` to be the canonical one and have `CountOfParsers.WHERE_X_IS` delegate to it. Either direction works.

**Effort:** ~1 hour. The two parsers are nearly identical structures.

**Friction addressed:** [Frictions §4](Frictions.md#4-cross-file-dependency-hierarchy-forces-inlining-and-duplication).

---

## 4. Lift `ZONE_NAME` / `ZONE` so `SelectorParsers` can reuse it

**Target:** Allow `SelectorParsers.CONTROLLER_CLAUSE` (specifically the new "you cast from [zone]" arm in Patrician Geist) to reference `ZoneParsers.ZONE` instead of inlining four hand-rolled possessive arms.

**Why:** The Patrician Geist fix duplicated zone-name parsing inline because the dependency hierarchy forbids `SelectorParsers` from importing `ZoneParsers`. The duplicate handles only "your", "their", "a/an", and bare zone names — narrower than `ZoneParsers.ZONE` and missing alternations like "the [zone]" that other zone parsers handle.

**Approach:**

1. Move the canonical `ZONE_NAME` / `ZONE` parsers to a lower-level module (`SelectorParsers` itself, or a new `ZoneAtomParsers` that sits below SelectorParsers).
2. Have `ZoneParsers` build its higher-level destinations on top of the lower-level atom.
3. Update Patrician Geist's "from [zone]" arm to use the canonical parser.

**Effort:** ~2 hours. The bigger lift is verifying that no circular import emerges; `ZoneParsers.ZONE` references `SelectorParsers.ZONE_NAME` already (one direction), so the other direction needs care.

**Friction addressed:** [Frictions §4](Frictions.md#4-cross-file-dependency-hierarchy-forces-inlining-and-duplication).

---

## 5. Audit and fill dispatcher coverage gaps

**Target:** Every effect type that's a valid arm of `BASE_EFFECT` should be reachable from `CLAUSE`, `MAY` (post-"may"), `objectVerbBody` (chain body), and `playerVerbBody` (player-actor chain) — wherever it grammatically applies.

**Why:** Several recent batches shipped with parsers that existed but weren't wired into a particular dispatcher. Each one is one card away from being discovered the hard way. The pattern is repeatable — there are likely more gaps not yet hit.

**Approach:**

1. Enumerate all `Effect.*` variants and the dispatcher(s) that should reach each one. (Some effects only make sense at top-level; a chain body wouldn't accept "Counter target spell" as a continuation, for example.)
2. Build a coverage matrix of `(Effect variant, dispatcher)` and mark each cell as "reachable" or "intentionally not reachable".
3. Walk the matrix; for each missing-but-should-be-reachable cell, add the registration.
4. Add an integration test (or grep-based lint) that fails when a new variant lands without dispatcher coverage.

**Effort:** ~1 day for the audit + per-gap fix time. The coverage matrix itself becomes a useful long-lived artifact.

**Friction addressed:** [Frictions §6](Frictions.md#6-dispatcher-coverage-gaps--existing-parsers-not-wired-into-all-entry-points).

---

## 6. Extend `AMOUNT_MATCHER` to accept the indefinite article

**Target:** `AmountMatcher` should accept "a/an X" as `AtLeast(exact(1))` directly, rather than requiring per-condition special-case arms.

**Why:** Imaginary Pet's "if you have a card in hand" required a one-off arm in `CARDS_IN_HAND_CONDITION`. Other AMOUNT_MATCHER consumers (`CARDS_IN_LIBRARY_CONDITION`, `ANY_ZONE_HAS_CARDS_CONDITION`, with-clause counters, etc.) almost certainly have the same gap. Each will need its own special-case until the matcher itself is fixed.

**Approach:**

1. Add an arm at the top of `AMOUNT_MATCHER`: `phrase("[a|an]").thenReturn(new AmountMatcher.AtLeast(Amount.exact(1)))`.
2. Verify ordering — "a"/"an" must precede arms that might consume "a" as part of a longer pattern (e.g., "another", "any").
3. Remove the per-condition workaround in `CARDS_IN_HAND_CONDITION`.
4. Add tests for representative consumers ("if you have a card in your library", "with a counter on it", etc.).

**Effort:** ~1-2 hours including dispatch-order verification.

**Friction addressed:** [Frictions §8](Frictions.md#8-amountmatcher-gaps--indefinite-article-and-other-shorthand-forms).

---

## 7. Curate `WITH_STOP_WORDS` against the verb dictionary

**Target:** Make `WITH_STOP_WORDS` derived from (or at least cross-checked against) the set of outer-effect verbs that can follow a selector with a with-clause.

**Why:** Today `WITH_STOP_WORDS` is hand-curated. Backslide added "face" because "with a morph ability face down" needed a boundary. Future verbs ("up", "to", "until", "while") could each require similar one-off additions, with the cost of debugging each greedy-match failure in turn.

**Approach:**

1. Generate a list of outer-effect-clause-starting words from the existing parser arms (`SET_TYPE`, `TURN_FACE_*`, `BECOME`, etc.).
2. Cross-reference against `WITH_STOP_WORDS`. Add any missing entries proactively.
3. Document the convention so future parser additions know to extend the stop-word list when introducing a new verb that follows a selector.

**Effort:** ~2 hours. Half exploration, half mechanical.

**Friction addressed:** [Frictions §7](Frictions.md#7-greedy-stop-word-lists-are-hand-curated).

---

## 8. Restructure `EffectParsers.java` to remove static-init order dependency

**Target:** Eliminate the silent NPE class of bugs where field A forward-references field B in the same file.

**Why:** `EffectParsers.java` is now >4500 lines. Source-position dependency between hundreds of `static final` fields is fragile and not enforced by the compiler. Channel had to be carefully placed after MAY; future agents will hit this again.

**Approach (in increasing order of effort):**

1. **Mitigation only:** Add a Javadoc block at the top of `EffectParsers.java` describing the static-init order constraint and listing the most-likely reverse references (e.g., "MAY must precede MAY_PAY_ANY_TIME_FOR_MANA").

2. **Mechanical fix:** Use `Parser.Rule<T>` (lazy reference) for fields that have forward dependencies. dot-parse already supports this for recursive grammars; the same mechanism would let any field reference any other regardless of source order. Cost: noisier code at the call site.

3. **Structural fix:** Split `EffectParsers.java` into multiple smaller files organized by effect category (combat effects, card-manipulation effects, counter effects, ...). Each file's static-init order is contained. The top-level dispatchers move to a thin file that imports the leaves. Cost: significant refactor, but pays back in maintainability beyond just the static-init problem.

**Effort:** Option 1 is ~30 min, Option 2 is ~half a day, Option 3 is ~1 week.

**Friction addressed:** [Frictions §5](Frictions.md#5-static-init-forward-references-silently-npe).

---

## 9. Address dot-parse no-backtracking systematically

**Target:** The class of bugs where an `anyOf` arm partially commits via a `sequence(...)` and prevents fallback.

**Why:** Three cards in the last batch (Cogwork Spy, Cylian Sunsinger, Firespitter Whelp) hit variations of this. It's the single largest source of "this card looks simple but the fix took an hour" cases.

**Approach (no silver bullet, but layered defenses):**

1. **Convention:** When an `anyOf` arm starts with a permissive parser (`QUALIFIER_LIST`, `SUBJECT`, `SELECTOR`, `PLAYER_LIKE_SUBJECT`, etc.), the arm should *check* with `notFollowedBy` or rearrange to use `optionallyFollowedBy` so the permissive prefix only commits when the suffix can in fact match. Document this in CLAUDE.md.

2. **Restructure dispatchers:** For high-frequency offenders like `OR_ALTERNATIVE`, split the `anyOf` so that the most-likely-to-fail-late arms are tried *last*, and the leading prefix is the most distinguishing token.

3. **Acceptance grammar:** When designing a new dispatcher, write a small set of representative oracle texts that distinguish each arm. If two arms can both match the same prefix-up-to-N tokens, that's a sign of structural conflict — flag it for restructuring before ship.

4. **Diagnostic tooling:** Improve dot-parse error messages to point at the "deepest committed position" along with the alternatives that *would* have matched if backtracking were available. This is a dot-parse-internal change but would dramatically cut diagnosis time.

**Effort:** Items 1-3 are ongoing convention work (no single ticket). Item 4 is upstream library work.

**Friction addressed:** [Frictions §1](Frictions.md#1-dot-parse-has-no-backtracking--partial-commits-become-dead-ends).
