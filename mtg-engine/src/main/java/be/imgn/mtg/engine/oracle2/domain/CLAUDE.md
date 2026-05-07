# Conventions for `oracle2.domain`

## Valueless markers are enum constants, not no-field records

If a domain shape carries no information beyond "I am of this kind" — no
state, no fields, only the type itself — model it as an `enum` constant,
not as a `record Foo() { }` with an empty parameter list.

```java
// Bad — every `new Chosen()` allocates a fresh instance with no
// distinguishing state. Two parses of the same oracle phrase produce
// two non-identical objects (equal but not the same reference) for
// no benefit.
record Chosen() implements NameSelector {}

// Good — single canonical instance, no needless allocation, intent is
// explicit.
enum Chosen implements NameSelector { CHOSEN }
```

Reach for a record only when the shape carries actual fields.

## Don't create isolated one-constant enums

The previous rule pushes valueless markers toward `enum X { X }`. That's
acceptable only when the enum sits **inside an umbrella** — a sealed
interface or other multi-shape grouping that the constant participates in.
A bare `enum Color { COLOR }` floating in the package, with no surrounding
hierarchy, is the smell this rule targets.

When you'd otherwise scatter several one-constant enums for related
concepts, gather them into one multi-valued enum instead.

### Examples

**Bad** — three isolated single-constant enums for related slot kinds:

```java
enum ChosenColor   { COLOR }
enum ChosenName    { NAME }
enum ChosenQuality { QUALITY }
```

**Better** — one umbrella enum:

```java
enum ChosenSlot { COLOR, NAME, QUALITY }
```

**Best** — when a sealed interface already groups the shapes, the
valueless markers live in a single `Standard` enum nested inside it:

```java
sealed interface NameSelector permits NameSelector.Is, NameSelector.Standard {
    record Is(String name) implements NameSelector {}
    enum Standard implements NameSelector { CHOSEN, HAS_NO_NAME }
}
```

The umbrella `NameSelector` provides the broader grouping; `Standard`
collects every no-payload arm of the sealed interface in one place.

## Default umbrella name: `Standard`

When you need a new umbrella enum for valueless markers under a sealed
interface, name it `Standard`. Picking a generic name on purpose:

- It tells the reader at a glance that this enum is the catch-all home
  for stateless predicates of the enclosing type.
- It avoids inventing a new noun ("Predicate", "Marker", "Special",
  "Kind") per selector — every reader, AI agent, and IDE jump-to
  works the same way across the package.
- A future stateless predicate can land in `Standard` without a new
  enum (and without churn in the `permits` clause).

If a more specific enum already exists in the same sealed interface
and the new constant genuinely fits its semantics, add the constant
there instead. Otherwise, default to `Standard`.
