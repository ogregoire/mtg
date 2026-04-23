---
name: parser-review
description: Review an oracle-text parser just written for semantic correctness, structure-over-strings, reuse, dispatch order, dot-parse idioms, and type-narrowness. Invoke after writing any non-trivial parser in mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/parser/ before considering it done.
user_invocable: true
---

# parser-review

Walk the checklist below against the parser you just wrote. Each item is a concrete check derived from real mistakes in this codebase — don't skim.

The project's dot-parse and oracle-text conventions live in `mtg-engine/src/main/java/be/imgn/mtg/engine/oracle/CLAUDE.md`. Re-read the sections relevant to what you just wrote; the checklist below picks up where that leaves off.

## Checklist

Work through these in order. If any fail, fix before moving on.

### 1. Semantic correctness

Does the output structure actually *mean* what the oracle text says?

- "mana of any color the land could produce" is **not** `anyOneColor` (WUBRG). It's bounded by the specific source — use a source-aware `ManaOption.ProducedBy` / equivalent variant.
- "Choose one" vs "you may" vs "if … instead" — each has distinct rules semantics. Don't collapse them into a common record just because the parse succeeds.
- If you're emitting a `SetCharacteristic` / `Prevent` with a free-text description, ask: could this be a typed record variant? Free text is a fallback, not a default.

### 2. Structure over strings

The *only* correct use of `String` in a parser output is to name another **card** — e.g., a meld card referencing its partner by literal name, or "a card named X" in a count-of. Every other use is a bug.

Scan every parameter of every new record and replace:

- `String` that names a game object → `Subject`.
- `String` that names a zone → `Zone` / `Zone.Named` / `Zone.Source`.
- `String` that names a keyword ability → an `Ability` constant (`Ability.StaticKeyword.*`).
- `String` that names a counter type → `CounterType`.
- `String` that names a color → `Color`.
- `String` that names a P/T modifier, damage amount, count → `Amount` / `PtModifier` / `Amount.CountOf`.
- `String` that names a duration ("until end of turn", "this turn") → `Duration`.
- `String` that encodes a closed set of English variants ("up"/"down", "first"/"second", "tapped"/"untapped") → a real enum.
- `String` that concatenates a "description" of what this effect is → a typed record variant, even if you have to introduce a new sealed alternative.

If the existing record shape forces a `String` because there's no structural slot for this data yet, **add the structural slot**. A free-text `SetCharacteristic.description` or `Prevent.description` is a migration debt, not a landing pad. Don't extend those strings; carve out a typed variant and narrow the free-text fallback over time.

Exception, documented: a literal card name (`Rite of Flame`, a meld partner). Comment the field `/// Literal card name — the only legitimate String in this module.` so grep for that phrase finds all of them.

### 3. Reuse before invention — *and* split when appropriate

Two counterweighted rules. Apply both.

**Reuse side:**

- Grep for the oracle-text verb/phrase you're capturing. Chances are there's already a `SUBJECT` / `SELECTOR` / `AMOUNT` / `DURATION` / `ZONE` / `TriggerEvent` handling it.
- If an existing parser almost fits, extend it (add a branch to its `anyOf`, add a new record arm to its sealed type) rather than forking a near-duplicate.
- Never write a new parser for "X or Y SUBJECT" when `PLAYER_OBJECT_FREE_TRIGGER` / `PLAYER_ACTOR_AND_CHAIN` / `VERB_OR_VERB_SUBJECT` already fan out shared-subject disjunctions.

**Split side** — equally important:

A broad parser with many `anyOf` branches is a warning sign, not a goal. **Split** when the branches encode genuinely different semantics even if their surface shape is similar:

- `SET_BASE_PT` (single value) vs `SET_BASE_PT_OR` (disjunctive choice) — different records, not a flag on a shared record.
- `REPLACE` (always-on replacement) vs `REPLACE_NEXT_TIME` (one-shot replacement) — different semantics, not a `boolean onlyNextTime` on a single record.
- `PLAYER_CASTS` vs `PLAYER_COPIES` — copy events aren't cast events (rule 707), so they need distinct `TriggerEvent` variants that compose via a peer-list combinator.

The test: if a downstream consumer (the engine, the rules checker, a test) would want to case-match on the two variants separately, they deserve separate types. If the only thing a flag does is tag which parse branch matched, the flag is masking a missing type.

Don't split just for parse-site convenience — split when the *domain* distinguishes them.

### 4. No combo parsers — compose, don't concatenate

**Never write a dedicated parser for "action A and action B"** (e.g., `DESTROY_AND_EXILE`, `COUNTER_AND_DEAL_DAMAGE`, `MODIFY_COST_AND_CANT_BE_COUNTERED`). That pattern conflates two things the domain keeps separate and dead-ends future reuse.

The correct shape:

1. A parser for action A, emitting `Effect.A`.
2. A parser for action B, emitting `Effect.B`.
3. Both registered in a shared action list (the EFFECT / CLAUSE dispatcher).
4. A single **generic** combinator that parses `"<action> and <action>"` — or `"<action>, <action>, and <action>"`, or `"<action> or <action>"` — pulling its leaves from that shared list.

Concretely: if two effects share a subject ("target creature gains flying and gets +1/+1"), the combinator is `SUBJECT.flatMap(subj -> objectVerbBody(subj).atLeastOnceDelimitedBy("and"))` over the already-registered verb bodies. If they don't share a subject ("Counter target spell and ~ deals 3 damage to target creature"), `EFFECT_SEQUENCE`'s "and" delimiter already handles it — you just need to prevent greedier parsers (SUBJECT conjunction, MAY's pay arm) from swallowing the boundary.

Signs you're about to write a combo parser:

- The new parser name contains `_AND_` or `_OR_` joining two effect names.
- It calls two existing leaf parsers in sequence and returns a `List<Effect>`.
- It has a hard-coded "and"/"or" literal between two specific cases.

Each is a cue to step back: add the missing leaf instead, register it in the shared list, and reach for the existing combinator. Yes, even if a fallback combinator doesn't yet exist — write *that*, once, and reuse it for every future pair.

Exception: when the pair truly isn't composable because one emits a `List<Effect>` that fans out peer restrictions sharing a subject (e.g., `CANT_ATTACK_OR_BLOCK` → AttackRestriction + CantBlock), still prefer a generic peer-list combinator over a hand-rolled pair. If you can't factor it that far this round, document the debt with a comment pointing at the generic combinator that should replace it.

### 5. Dispatch order

If the parser shares a prefix with another:

- Run `grep` in `EffectParsers.java` (or the relevant file) for the shared first phrase. List every match.
- Identify which parser wins if you register at the bottom of the `anyOf`. If a greedier one fires first, the new one is dead code.
- Common shadowers: `MAY` (`"You may <verb>"`), `SUBJECT` "and"-conjunction (consumes the "and" that was meant to delimit effects), `SET_BASE_PT` vs `SET_BASE_PT_OR` vs `DOUBLE_PT`, any `IF_CONDITION`-prefixed effect.
- Fix by reordering (more specific before generic) or by adding a `notFollowedBy` on the greedier one.

Run the failing-card parse **both before and after** registering the new parser: before to confirm your diagnosis, after to confirm the fix.

### 6. Record hygiene

New or modified record:

- Any wither method (`withX`, `asY`) — is its name the same as the auto-generated accessor for a field? If yes, Java will reject it as an "invalid accessor method". Rename (`asOnlyNextTime()` vs the accessor `onlyNextTime()`).
- Convenience constructors that shrink the parameter list are fine; make sure they delegate to the canonical constructor and keep field semantics consistent.
- If you added a nullable field, annotate it `@Nullable`. On a qualified inner type, the annotation goes between the qualifier and the type: `Zone.@Nullable Source`, not `@Nullable Zone.Source`.

### 7. dot-parse idioms

Per the project's oracle `CLAUDE.md`:

- Multi-word phrases → single `phrase("…")`, not chained `.then(word(…))`.
- Optional tail → `optionallyFollowedBy(x, wither)`, not `.followedBy(x.optional())`.
- Sentence-start-capable tokens → Title case in `phrase()` (matches both cases). Strictly mid-sentence → lowercase.
- Return a constant on match → `thenReturn(value)`, not `.map(_ -> value)`.
- Ignore a prefix/suffix in a sequence → `prefix.then(parser)` / `parser.followedBy(suffix)`, don't add an unused lambda parameter.

### 8. Stay specialized; widen once, at the end

Keep each parser's generic parameter as narrow as possible — **the most specialized subtype you can**. Widening to the sealed-interface super type happens **exactly once**, at the composition site (the enclosing `anyOf`, or an explicitly-typed variable), where covariance absorbs the subtype parsers automatically. Every `.map(x -> (Super) x)`, pre-widening `.<Super>map(…)`, or `(Super) new …` cast inside a lambda is a smell: it hoists the widen earlier than necessary and erases the narrow type you could have used for case-matches, tests, or downstream parsers.

The rule: every `.map(factory)` / `.thenReturn(value)` should have the *most specialized* factory return type the lambda allows. Only the outer composition site names the super type.

**Bad — cast in the lambda widens at the innermost point:**

```java
// KeywordParsers.java, QUALITY arm (what this was before)
sequence(
        anyOf(string("non-"), string("Non-")).then(SUBTYPE),
        CARD_TYPE,
        (st, ct) -> (ProtectionQuality) new ProtectionQuality.OfNonSubtypeOfCardType(st, ct))
```

**Bad — pre-widening type witness on `.map`:**

```java
// Same arm, half-fixed: the cast moved but still pre-widens.
phrase("the color of").then(…).followedBy(word("choice"))
        .<ProtectionQuality>map(ProtectionQuality.ChosenColor::new)
```

**Good — narrow all the way through; widen only at the enclosing `anyOf`:**

```java
// KeywordParsers.java, final form — each arm is Parser<subtype>,
// and Parser.anyOf covariantly collects them into Parser<ProtectionQuality>
// via the declared variable type.
private static final Parser<ProtectionQuality> QUALITY = Parser.anyOf(
        phrase("the color of").then(…).followedBy(word("choice"))
                .map(ProtectionQuality.ChosenColor::new),         // Parser<ChosenColor>
        phrase("the colors of").then(SUBJECT)
                .map(ProtectionQuality.ColorsOf::new),            // Parser<ColorsOf>
        sequence(
                anyOf(string("non-"), string("Non-")).then(SUBTYPE),
                CARD_TYPE,
                ProtectionQuality.OfNonSubtypeOfCardType::new),   // Parser<OfNonSubtypeOfCardType>
        COLOR.map(ProtectionQuality.OfColor::new),                // Parser<OfColor>
        CARD_TYPE.map(ProtectionQuality.OfCardType::new));        // Parser<OfCardType>
```

**Equally good — top-level static parsers declared at their narrow type, widened once at the dispatcher:**

```java
// EffectParsers.java
static final Parser<Effect.Destroy> DESTROY = …;
static final Parser<Effect.Exile>   EXILE   = …;

// The call site widens via a type witness on anyOf (or the enclosing variable's type).
Parser.<Effect>anyOf(DESTROY, EXILE, …)
```

**Why this matters:**

- Keeps the narrow `Parser<Effect.Destroy>` reusable — another parser that needs specifically a Destroy (e.g., a `DESTROY.optionallyFollowedBy(DURATION, Destroy::withDuration)`) can still call withers on it without a downcast.
- The single widen at the composition site is the only place the super-type name appears — if you later rename the sealed root or rearrange the hierarchy, there's exactly one touch-point.
- Internal casts and `<Super>`-witnessed `.map` survive refactors silently by erasing information; narrow types surface the mismatch at compile time.

Signs you're widening too early:

- Your `.map(…)` or `.thenReturn(…)` has an explicit `.<Super>` type witness and the resulting `Parser<Super>` is *immediately* passed to an outer `anyOf`/assignment that could have driven inference.
- A lambda body starts with `(Super) new Subtype(…)` — the cast is the widen.
- The resulting `.map(x -> x)` or `.map(x -> (Super) x)` just for retyping exists anywhere.

Fix: drop the witness / cast, and let the enclosing `Parser.<Super>anyOf(…)` or `Parser<Super> FOO = …` composition site do the widening once.

Exception — genuinely ambiguous `anyOf` where multiple arms return unrelated subtypes that share only `Object` as a super-type, or where Java's inference needs a hint. In that narrow case, the type witness belongs on `anyOf` itself (`Parser.<Super>anyOf(…)`), not on the individual arms.

### 9. Verify

- Compile: `./mvnw -q install -DskipTests` — silent success.
- Tests: `./mvnw -pl mtg-engine test` — 3245+ and still passing.
- The target card: `./mtg parse "Card Name"` — produces a structured AST, not `FAILED`.
- Sibling cards from the same failure pattern — if you fixed Fiend Binder, also run the other "defending player controls" cards you identified.

If any sibling regresses, the fix is too narrow or too broad — revisit.

## When to invoke

- After writing any new parser or record variant in `oracle.parser` / `oracle.domain`.
- After editing an existing parser in a way that changes what it accepts or emits.
- Before reporting a card as "fixed" or closing a batch.

Not needed for pure rename, comment-only, or one-line literal tweaks.
