# Basic Spell Casting (Option B)

## Goal

Enable the complete spell casting flow: player pays mana → spell goes on stack → priority passes → spell resolves (permanent enters battlefield or instant/sorcery goes to graveyard).

## Scope

- Simple mana cost payment (no cost modifiers, no alternative/additional costs, no X spells)
- Sorcery-speed vs instant-speed timing checks
- Card must be in hand
- Can-pay mana validation
- Stack resolution for permanent spells and instant/sorcery spells
- No modes, no targets, no mana ability window

## Changes

### 1. Validation: `DefaultActionValidator.validateCastSpell()`

Add checks (accumulating errors like `validatePlayLand`):

- `requirePriority(player)` — already present
- `requireInHand(card, player, state)` — reuse existing method
- Sorcery-speed timing: if card types do NOT include instant, require active player + main phase + empty stack
- `requireCanPayManaCost(card, player)` — check `player.canPay(manaCost, costContext)`

### 2. Execution: `DefaultActionExecutor.executeCastSpell()`

- Get card's `ManaCost`, pay via `player.pay(manaCost, costContext)`
- Create `CastEvent(card, ZoneType.HAND, player)`, process through `GameEventProcessor`
- `ZoneChangeResolver.resolveCast()` already handles creating `Spell` and pushing to stack
- Return `Success` with the cast event

Requires `GameEventProcessor` injected into `DefaultActionExecutor`.

### 3. Resolution: `Stack.resolve()` + `DefaultStack`

- Add `resolve()` method to `Stack` interface
- `DefaultStack` gets `GameEventProcessor` and `GameState` injected (Guice handles circular deps via interface proxies)
- `resolve()` implementation:
  - Pop top `StackObject`
  - **Permanent spell**: call `state.battlefield().enter(card, controller)` → get `Permanent` → process `EntersBattlefieldEvent(permanent, ZoneType.STACK)`
  - **Instant/sorcery**: process `PutIntoGraveyardEvent(spell, ZoneType.STACK)`
  - **AbilityOnStack**: just remove (ceases to exist)
- `DefaultTurnTracker`: replace `stack.pop()` with `stack.resolve()`

## Existing Infrastructure Used

| Component | How It's Used |
|-----------|--------------|
| `CastEvent` + `ZoneChangeResolver.resolveCast()` | Card → Spell → stack.push() |
| `Player.canPay()` / `Player.pay()` | Mana cost validation and payment |
| `ManaPool` | Already deducts mana via `Player.pay()` |
| `GameEventProcessor` | Replacement effects, resolution, triggers |
| `EntersBattlefieldEvent` | Permanent ETB |
| `PutIntoGraveyardEvent` | Instant/sorcery to graveyard |
| `Battlefield.enter(Card, Player)` | Creates Permanent from Card |

## Files Modified

- `DefaultActionValidator.java` — add cast spell validation
- `DefaultActionExecutor.java` — implement `executeCastSpell()`
- `Stack.java` — add `resolve()` method
- `DefaultStack.java` — implement `resolve()` with injected deps
- `DefaultTurnTracker.java` — replace `stack.pop()` with `stack.resolve()`
- `ActionModule.java` — wire new dependencies
- `ZoneModule.java` — wire new dependencies for DefaultStack
