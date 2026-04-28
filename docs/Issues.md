# Issues

Open issues to address later. Each entry describes a known incorrect behavior in the parser or engine — not a recurring design friction (those live in [Frictions.md](Frictions.md)) and not a cleanup suggestion (those live in [Recommendations.md](Recommendations.md)).

---

## 1. "As [permanent] enters" is parsed as a triggered ability

**Where:** `OracleParser.TRIGGERED` (`OracleParser.java:294-329`).

**Symptom:** Oracle text starting with "As [subject] enters, …" (e.g. Sol Grail: "As this artifact enters, choose a color.") is matched by the `TRIGGERED` parser, which lists `phrase("As").thenReturn("as")` alongside `When` / `Whenever` / `At` as a trigger word. The result is an `Ability.TriggeredAbility` with `triggerWord = "as"`.

**Why it's wrong:** Per **rule 614.1c**, "As [this permanent] enters …" / "[This permanent] enters as …" / "[This permanent] enters with …" are **replacement effects**, not triggered abilities. They modify the ETB event itself rather than triggering off of it. The semantic distinctions that follow:

- A replacement effect applies *as* the permanent enters — choices made under "as it enters" are part of the ETB event and visible to other replacement effects (e.g. the chosen color is on the permanent the moment it's on the battlefield).
- A triggered ability ("when it enters") fires *after* the permanent has already entered, goes on the stack, and resolves separately.

The current parser collapses both into the same `TriggeredAbility` shape, distinguishable only by the `triggerWord` string `"as"`. Engine consumers cannot reliably treat these as replacement effects without string-matching the trigger word.

**Affected cards:** any "As [subject] enters" card — choose-a-color cards (Sol Grail, Adaptive Automaton, Cavern of Souls), choose-a-creature-type cards, "enters with N counters" cards parsed via the As-form, etc.

**Why it has been left as-is:** the engine has no `Ability.Replacement` type today, only `Static` / `Triggered` / `Activated` / `Spell` (rule 113.3 enumerates four ability types; replacement effects are produced by static abilities per rule 614.1, so they would naturally land under `Static`). Modeling this correctly requires:

1. Extending the AST — either a dedicated `Ability.ReplacementAbility` (or making the existing `Effect.Replace` reachable from a top-level "as enters" parser arm wrapped in a `StaticAbility`).
2. Removing the `"as"` arm from `TRIGGERED` and routing those cards through the new shape.
3. Migrating any engine consumer that currently special-cases `triggerWord.equals("as")`.

**Effort:** ~half a day, blocked on deciding the AST shape (replacement-as-static vs. dedicated replacement ability).
