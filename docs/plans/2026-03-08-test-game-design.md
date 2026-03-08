# TestGame Design

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a TestGame test utility that wraps a real Guice-backed game, connects to the H2 card database, and provides a fluent API for writing card interaction scenarios.

**Architecture:** TestGame lives in mtg-engine test sources. It bootstraps a real game via Guice (GameModule → GameFactory), wraps GameState/TurnTracker/ActionExecutor, and exposes convenience methods for state setup, game advancement, and player actions. A CardFetcher helper queries the on-disk H2 database and converts results to Card objects using parser combinators for type lines and ManaCost.parse() for mana costs.

**Tech Stack:** Guice, H2, JDBI, dot-parse (parser combinators), JUnit 5

---

## Prerequisite Refactoring

### ActivatedAbility: interface → record

Convert `ActivatedAbility` from a `non-sealed interface` to a `record` implementing `Ability`. Absorb all fields and the `create` factory method from `ParsedActivatedAbility`. Delete `ParsedActivatedAbility`. The default methods (`isManaAbility()`, `isLoyaltyAbility()`, `canActivate()`) become regular methods on the record.

Files:
- Modify: `be.imgn.mtg.engine.ability.ActivatedAbility` — convert to record
- Delete: `be.imgn.mtg.engine.ability.internal.parser.ParsedActivatedAbility`
- Modify: `ActivatedAbilityParser` — return `ActivatedAbility` instead of `ParsedActivatedAbility`
- Update all references

### SpellAbilityParser

Location: `be.imgn.mtg.engine.ability.internal.parser.SpellAbilityParser`

Parser combinator that parses oracle text for instant/sorcery cards into `SpellAbility` records. Parses effect text (delegating to `EffectParser`) and wraps the result in a `SpellAbility`.

### AbilityParser

Location: `be.imgn.mtg.engine.ability.internal.parser.AbilityParser`

Main entry point for parsing oracle text into abilities. Aggregates `ActivatedAbilityParser` and `SpellAbilityParser`. Takes full oracle text, splits by newline (each line is a separate ability), tries each parser, returns `List<Ability>`.

```java
public static List<Ability> parse(String oracleText) { ... }
```

## Components

### TestGame

Location: `mtg-engine/src/test/java/be/imgn/mtg/engine/game/TestGame.java`

Not an implementation of Game. A test utility that wraps the real game infrastructure.

```java
var game = TestGame.create();
var bear = game.createPermanent("Grizzly Bears");
game.advanceTo(PhaseType.COMBAT);
game.attack(bear);
```

### CardFetcher

Interface: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/CardFetcher.java`
Implementation: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcher.java`
Module: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/CardModule.java`

Connects to the on-disk H2 database at `~/Library/Application Support/mtg-engine/cards.mv.db`. Queries cards by name via JDBI (replicates CardQueryDao.findByExactName logic). Converts database rows to Card objects using TypeLineParser and ManaCost.parse(). CardModule provides Guice bindings for CardFetcher and its dependencies.

### TypeLineParser

Location: `mtg-engine/src/main/java/be/imgn/mtg/engine/characteristics/internal/TypeLineParser.java`

Parser combinator (dot-parse) that parses type lines like `"Legendary Creature — Human Soldier"` into Supertypes, Types, and Subtypes. Uses OracleParser.word() for matching known enum values. Lives in main sources since parsing type lines is a general capability, not test-specific.

## API

### State Setup

- `createPermanent(String cardName)` → Permanent — fetch from DB, enter battlefield under player 1
- `createPermanent(String cardName, Player controller)` → Permanent
- `addToHand(String cardName)` → Card — fetch and put in player's hand
- `addToHand(String cardName, Player player)` → Card
- `addToGraveyard(String cardName)` → Card
- `addToGraveyard(String cardName, Player player)` → Card
- `setLife(Player, int)` → void

### Game Advancement

- `advanceTo(PhaseType)` — pass priority until reaching the target phase
- `advanceTo(StepType)` — pass priority until reaching the target step
- `passPriority()` — pass priority once

### Actions

- `castSpell(Card card)` — cast from hand to stack
- `castSpell(Card card, Selectable... targets)` — cast with targets
- `playLand(Card card)` — play a land
- `resolveTopOfStack()` — resolve top spell/ability
- `attack(Permanent... creatures)` — declare attackers
- `block(Permanent blocker, Permanent attacker)` — declare blocker

### Access

- `player1()` / `player2()` — the two players
- `battlefield()` / `stack()` — zone access
- `gameState()` — full game state access

## Database Connection

DefaultCardFetcher connects to the existing on-disk database using H2's AUTO_SERVER=TRUE mode. If the database doesn't exist, throws a clear error.

The query replicates `CardQueryDao.findByExactName`: case-insensitive name match on the card table.

## Card Conversion Pipeline

Database row → Card object:

1. **Name**: direct mapping
2. **Mana cost**: `ManaCost.parse(manaCostString)`
3. **Type line**: `TypeLineParser` (parser combinator) → Types, Supertypes, Subtypes
4. **Power/toughness**: parse to `Value`
5. **Loyalty/defense**: parse to `Value`
6. **Oracle text**: parsed through `AbilityParser.parse(oracleText)` to produce `List<Ability>`; also stored as `rulesText` on the Card
7. **Colors**: mapped from color string (e.g., "WU" → WHITE, BLUE)

## TypeLineParser Details

Parser combinator using dot-parse, following existing project patterns:

```
type_line     = supertypes types [" — " subtypes]
supertypes    = supertype*           (Legendary, Basic, Snow, World)
types         = type+               (Creature, Instant, Artifact, etc.)
subtypes      = subtype (" " subtype)*
```

Uses `OracleParser.word()` for matching Supertype and Type enum values. Subtypes are created as `Subtype` instances from remaining words after the dash.
