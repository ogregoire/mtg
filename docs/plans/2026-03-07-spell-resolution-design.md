# Instant/Sorcery Spell Resolution Design

## Goal

When an instant or sorcery spell resolves, execute its effects against the game state before putting it into the graveyard. Includes basic targeting (chosen at cast time, checked at resolution).

## Architecture

Three new components connect the existing Effect data model to game state changes:

1. **SpellAbility (record)** — replaces the current SpellAbility interface. Stores parsed `List<Effect>` on instant/sorcery cards as part of their `Abilities` collection. Copied to Spell at cast time.

2. **EffectExecutor** — a service that switches on the sealed `Effect` type and translates each effect into game state changes. Reuses existing events (DrawEvent, DiscardEvent) where possible. Unimplemented effects throw `UnsupportedOperationException`.

3. **SpellContext** — a record on `Spell` carrying casting-time decisions (target choices, later: modes, X value, etc.). Set during casting, read at resolution.

## Resolution Flow

```
Stack.resolve()
  -> pop top spell
  -> if instant/sorcery:
      1. Get SpellAbility from spell.abilities()
      2. Get SpellContext from spell.context()
      3. Check target legality (Rule 608.2b)
         -> All targets illegal? Fizzle (graveyard, no effects)
         -> Some legal? Continue with legal ones
      4. Build ResolutionContext(gameState, controller, spellContext, eventProcessor)
      5. For each effect in SpellAbility.effects():
         -> EffectExecutor.execute(effect, resolutionContext)
      6. Put spell to graveyard (PutIntoGraveyardEvent)
  -> if permanent: battlefield.enter() (unchanged)
```

## Key Types

### SpellAbility (replaces interface with record)

```java
public record SpellAbility(
    AbilityId id,
    String oracleText,
    List<Effect> effects
) implements Ability {}
```

### SpellContext

```java
public record SpellContext(
    TargetChoices targets
) {
    public static SpellContext empty() {
        return new SpellContext(TargetChoices.empty());
    }
}
```

### Targeting

Targeting integrates with the existing `Subject` / `Selector` / `Choice` systems:

- **Subject.Select(Selector)** — already used in effect records to describe what can be targeted (e.g., "target creature" parses to `Subject.Select(ObjectSelector(...))`)
- **ObjectSelector.matches(GameObject, Player)** — evaluates whether a game object is a legal target
- **Player.choose(Choice)** — presents legal targets to the player for selection

**At cast time:** For each effect with a `Subject.Select(selector)`, use the selector to find legal targets via `ObjectSelector.matches()`, present them via `Player.choose(Choice.oneOf(legalTargets))`, and store the chosen target in `SpellContext.targets()`.

**At resolution:** Re-evaluate selector against each chosen target to check legality (Rule 608.2b). Then `EffectExecutor` resolves the `Subject`:
- `Subject.Select` — look up chosen target from `SpellContext`
- `Subject.Pronoun` — resolve IT/THEM reference
- `Subject.ThatObject` — resolve "that" reference

```java
record TargetChoice(Subject subject, Selectable target) {}
record TargetChoices(List<TargetChoice> choices) {
    static TargetChoices empty() { return new TargetChoices(List.of()); }
}
```

### EffectExecutor

```java
public interface EffectExecutor {
    void execute(Effect effect, ResolutionContext context);
}
```

### ResolutionContext

```java
public record ResolutionContext(
    GameState gameState,
    Player controller,
    SpellContext spellContext,
    GameEventProcessor eventProcessor
) {}
```

## Starter Effects (6 of 32)

| Effect | Execution |
|--------|-----------|
| DealDamageEffect(Amount, Subject) | Resolve target from SpellContext, deal damage to player or creature |
| DrawEffect(Optional PlayerReference, Amount) | Default to controller, process DrawEvent via GameEventProcessor |
| DestroyEffect(Subject, boolean) | Resolve target, process destroy event via GameEventProcessor |
| GainLifeEffect(Amount, Optional PlayerReference) | Default to controller, modify life total |
| DiscardEffect(Optional PlayerReference, Amount) | Process DiscardEvent via GameEventProcessor |
| CompoundEffect(List Effect) | Iterate sub-effects, execute each in order |

Remaining 26 effect types throw `UnsupportedOperationException` until implemented.

## Scope Exclusions

- Modes (modal spells)
- X values
- Alternative/additional costs
- Copy effects
- Reflexive triggers during resolution
