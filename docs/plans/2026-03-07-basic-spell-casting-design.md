# Basic Spell Casting Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable the complete spell casting flow: player pays mana, spell goes on stack, priority passes, spell resolves (permanent enters battlefield or instant/sorcery goes to graveyard).

**Architecture:** Three changes connect existing building blocks. (1) Validation checks sorcery-speed timing, card-in-hand, and mana affordability. (2) Executor pays mana and processes a CastEvent through GameEventProcessor (ZoneChangeResolver already handles card→spell→stack). (3) Stack.resolve() pops the top spell and delegates to Battlefield.enter() for permanents or processes PutIntoGraveyardEvent for instants/sorceries. Battlefield.enter(Card, Player) becomes the orchestrator for ETB, handling replacement effects and firing events internally.

**Tech Stack:** Java 24, Guice (DI), Mockito + AssertJ (tests), Maven

---

## File Reference

All paths relative to `mtg-engine/src/`:

| Alias | Path |
|-------|------|
| `Validator` | `main/java/be/imgn/mtg/engine/action/internal/DefaultActionValidator.java` |
| `ValidatorTest` | `test/java/be/imgn/mtg/engine/action/internal/DefaultActionValidatorTest.java` |
| `Executor` | `main/java/be/imgn/mtg/engine/action/internal/DefaultActionExecutor.java` |
| `ExecutorTest` | `test/java/be/imgn/mtg/engine/action/internal/DefaultActionExecutorTest.java` |
| `Stack` | `main/java/be/imgn/mtg/engine/zone/Stack.java` |
| `DefaultStack` | `main/java/be/imgn/mtg/engine/zone/internal/DefaultStack.java` |
| `DefaultStackTest` | `test/java/be/imgn/mtg/engine/zone/internal/DefaultStackTest.java` |
| `Battlefield` | `main/java/be/imgn/mtg/engine/zone/Battlefield.java` |
| `DefaultBattlefield` | `main/java/be/imgn/mtg/engine/zone/internal/DefaultBattlefield.java` |
| `DefaultBattlefieldTest` | `test/java/be/imgn/mtg/engine/zone/internal/DefaultBattlefieldTest.java` |
| `TurnTracker` | `main/java/be/imgn/mtg/engine/turn/internal/DefaultTurnTracker.java` |
| `TurnTrackerTest` | `test/java/be/imgn/mtg/engine/turn/internal/TurnTrackerTest.java` |
| `ActionModule` | `main/java/be/imgn/mtg/engine/action/internal/ActionModule.java` |
| `SharedZonesModule` | `main/java/be/imgn/mtg/engine/zone/internal/SharedZonesModule.java` |
| `SpecialActionHandler` | `main/java/be/imgn/mtg/engine/action/internal/DefaultSpecialActionHandler.java` |
| `ZoneChangeResolver` | `main/java/be/imgn/mtg/engine/resolver/internal/ZoneChangeResolver.java` |

---

## Task 1: Validate CastSpell — card in hand + sorcery-speed timing

**Files:**
- Modify: `Validator`
- Modify: `ValidatorTest`

### Step 1: Write failing tests

Add to the existing `CastSpellValidationTests` nested class in `ValidatorTest`. The test setup needs `Hand`, `Stack`, and `Types` mocks. Add a `setupLegalCast` helper similar to the existing `setupLegalLandPlay`.

```java
// In CastSpellValidationTests:

private Hand hand;
private Stack stack;

@BeforeEach
void setUp() {
    hand = mock(Hand.class);
    stack = mock(Stack.class);
    when(gameState.hand(player1)).thenReturn(hand);
    when(gameState.stack()).thenReturn(stack);
}

private Card createInstant() {
    return Card.builder()
            .owner(player1)
            .controller(player1)
            .name("Lightning Bolt")
            .type(Type.INSTANT)
            .build();
}

private Card createSorcery() {
    return Card.builder()
            .owner(player1)
            .controller(player1)
            .name("Divination")
            .type(Type.SORCERY)
            .build();
}

private Card createCreature() {
    return Card.builder()
            .owner(player1)
            .controller(player1)
            .name("Grizzly Bears")
            .type(Type.CREATURE)
            .power(Value.of(2))
            .toughness(Value.of(2))
            .build();
}

private void setupLegalCast(Card card) {
    when(turnTracker.hasPriority(player1)).thenReturn(true);
    when(turnTracker.activePlayer()).thenReturn(player1);
    when(turnTracker.currentPhase()).thenReturn(Phase.MAIN);
    when(stack.all()).thenReturn(List.of());
    when(hand.contains(card)).thenReturn(true);
}
```

Update existing tests and add new ones:

```java
@Test
void isLegalForInstantWhenPlayerHasPriority() {
    var card = createInstant();
    setupLegalCast(card);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isEqualTo(new ValidationResult.Legal());
}

@Test
void isLegalForSorceryDuringMainPhaseWithEmptyStack() {
    var card = createSorcery();
    setupLegalCast(card);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isEqualTo(new ValidationResult.Legal());
}

@Test
void isIllegalWhenCardNotInHand() {
    var card = createInstant();
    setupLegalCast(card);
    when(hand.contains(card)).thenReturn(false);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
    var illegal = (ValidationResult.Illegal) result;
    assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.NOT_IN_ZONE);
}

@Test
void isIllegalForSorcerySpeedWhenNotActivePlayer() {
    var card = createCreature();
    setupLegalCast(card);
    var otherPlayer = mock(Player.class);
    when(turnTracker.activePlayer()).thenReturn(otherPlayer);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
    var illegal = (ValidationResult.Illegal) result;
    assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
}

@Test
void isIllegalForSorcerySpeedWhenNotInMainPhase() {
    var card = createSorcery();
    setupLegalCast(card);
    when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
    var illegal = (ValidationResult.Illegal) result;
    assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
}

@Test
void isIllegalForSorcerySpeedWhenStackNotEmpty() {
    var card = createCreature();
    setupLegalCast(card);
    when(stack.all()).thenReturn(List.of(mock(Spell.class)));
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
    var illegal = (ValidationResult.Illegal) result;
    assertThat(illegal.errors()).anyMatch(e -> e.type() == IllegalActionType.WRONG_TIMING);
}

@Test
void isLegalForInstantDuringCombatPhase() {
    var card = createInstant();
    setupLegalCast(card);
    when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isEqualTo(new ValidationResult.Legal());
}

@Test
void isLegalForInstantWithNonEmptyStack() {
    var card = createInstant();
    setupLegalCast(card);
    when(stack.all()).thenReturn(List.of(mock(Spell.class)));
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isEqualTo(new ValidationResult.Legal());
}

@Test
void collectsMultipleViolationsForSorcerySpeed() {
    var card = createSorcery();
    var otherPlayer = mock(Player.class);
    when(turnTracker.hasPriority(player1)).thenReturn(false);
    when(turnTracker.activePlayer()).thenReturn(otherPlayer);
    when(turnTracker.currentPhase()).thenReturn(Phase.COMBAT);
    when(stack.all()).thenReturn(List.of(mock(Spell.class)));
    when(hand.contains(card)).thenReturn(false);
    when(gameState.hand(player1)).thenReturn(hand);
    when(gameState.stack()).thenReturn(stack);
    var action = new PlayerAction.CastSpell(player1, card);

    var result = validator.validate(action, gameState);

    assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
    var illegal = (ValidationResult.Illegal) result;
    assertThat(illegal.errors()).hasSizeGreaterThanOrEqualTo(3);
}
```

The existing `isLegalWhenPlayerHasPriority` and `isIllegalWhenPlayerDoesNotHavePriority` tests need to be replaced by these more comprehensive tests. Remove them.

### Step 2: Run tests to verify they fail

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultActionValidatorTest" -DfailIfNoTests=false`
Expected: Compilation errors or test failures because `validateCastSpell` only checks priority.

### Step 3: Implement validateCastSpell

In `Validator`, change `validateCastSpell` to accept `GameState` and accumulate checks:

```java
private ValidationResult validateCastSpell(PlayerAction.CastSpell cast, GameState state) {
    var card = cast.card();
    var player = cast.player();

    // Instants can be cast anytime with priority; non-instants need sorcery-speed timing
    if (card.types().isInstant()) {
        return ValidationResult.merge(
                requirePriority(player),
                requireInHand(card, player, state));
    }

    return ValidationResult.merge(
            requirePriority(player),
            requireInHand(card, player, state),
            requireActivePlayer(player),
            requirePhase(Phase.MAIN),
            requireEmptyStack(state));
}
```

Update the `requireInHand` error message to be generic ("Card is not in player's hand") or keep it as-is since both land and spell use it.

Update the switch in `validate()` to pass `state`:
```java
case PlayerAction.CastSpell cast -> validateCastSpell(cast, state);
```

### Step 4: Run tests to verify they pass

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultActionValidatorTest" -DfailIfNoTests=false`
Expected: All tests PASS.

### Step 5: Run `ide_diagnostics` on modified files

### Step 6: Commit

```
Add cast spell validation for timing and card-in-hand
```

---

## Task 2: Implement executeCastSpell — pay mana + process CastEvent

**Files:**
- Modify: `Executor` — add `GameEventProcessor` dependency, implement `executeCastSpell`
- Modify: `ExecutorTest` — replace "throws UnsupportedOperationException" test with real tests
- Modify: `ActionModule` — wire `GameEventProcessor` into `DefaultActionExecutor`

### Step 1: Write failing tests

Replace the existing `CastSpellExecutionTests` in `ExecutorTest`:

```java
@Nested
@DisplayName("CastSpell execution")
class CastSpellExecutionTests {

    @Test
    void paysManaCostAndProcessesCastEvent() {
        var manaCost = ManaCost.parse("{1}{R}");
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .type(Type.INSTANT)
                .manaCost(manaCost)
                .build();
        var action = new PlayerAction.CastSpell(player, card);

        executor.execute(action, gameState);

        verify(player).pay(eq(manaCost), any(CostContext.class));
        verify(eventProcessor).process(any(CastEvent.class));
    }

    @Test
    void returnsSuccessWithCastEvent() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .type(Type.INSTANT)
                .manaCost(ManaCost.parse("{R}"))
                .build();
        var action = new PlayerAction.CastSpell(player, card);

        var result = executor.execute(action, gameState);

        assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        var success = (ExecutionResult.Success) result;
        assertThat(success.events()).hasSize(1);
        assertThat(success.events().getFirst()).isInstanceOf(CastEvent.class);
    }

    @Test
    void castEventHasCorrectFields() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Grizzly Bears")
                .type(Type.CREATURE)
                .manaCost(ManaCost.parse("{1}{G}"))
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();
        var action = new PlayerAction.CastSpell(player, card);

        executor.execute(action, gameState);

        var captor = ArgumentCaptor.forClass(CastEvent.class);
        verify(eventProcessor).process(captor.capture());
        var event = captor.getValue();
        assertThat(event.card()).isEqualTo(card);
        assertThat(event.from()).isEqualTo(ZoneType.HAND);
        assertThat(event.caster()).isEqualTo(player);
    }

    @Test
    void skipsPaymentForZeroManaCost() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Ornithopter")
                .type(Type.CREATURE)
                .type(Type.ARTIFACT)
                .manaCost(ManaCost.empty())
                .power(Value.of(0))
                .toughness(Value.of(2))
                .build();
        var action = new PlayerAction.CastSpell(player, card);

        var result = executor.execute(action, gameState);

        assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        verify(eventProcessor).process(any(CastEvent.class));
    }
}
```

The test setUp needs to be updated to include `GameEventProcessor` and wire it into the executor:

```java
// Add to fields:
private GameEventProcessor eventProcessor;

// Update setUp:
eventProcessor = mock(GameEventProcessor.class);
executor = new DefaultActionExecutor(turnTracker, specialActionHandler, abilityManager, eventProcessor);
```

### Step 2: Run tests to verify they fail

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultActionExecutorTest" -DfailIfNoTests=false`
Expected: Compilation error — constructor doesn't accept `GameEventProcessor` yet.

### Step 3: Implement

In `Executor`, add `GameEventProcessor` field and constructor parameter:

```java
private final GameEventProcessor eventProcessor;

DefaultActionExecutor(
        TurnTracker turnTracker, SpecialActionHandler specialActionHandler,
        AbilityManager abilityManager, GameEventProcessor eventProcessor) {
    this.turnTracker = turnTracker;
    this.specialActionHandler = specialActionHandler;
    this.abilityManager = abilityManager;
    this.eventProcessor = eventProcessor;
}
```

Implement `executeCastSpell`:

```java
private ExecutionResult executeCastSpell(PlayerAction.CastSpell cast, GameState state) {
    var card = cast.card();
    var player = cast.player();

    // Pay mana cost
    var manaCost = card.manaCost();
    if (manaCost != null && !manaCost.isEmpty()) {
        var context = new CostContext(player, card);
        player.pay(manaCost, context);
    }

    // Process cast event — ZoneChangeResolver handles card→spell→stack
    var castEvent = new CastEvent(card, ZoneType.HAND, player);
    eventProcessor.process(castEvent);

    return new ExecutionResult.Success(List.of(castEvent));
}
```

Update the switch in `doExecute` to pass `state`:
```java
case PlayerAction.CastSpell cast -> executeCastSpell(cast, state);
```

In `ActionModule`, update the `provideActionExecutor` method:

```java
@Provides
@Singleton
ActionExecutor provideActionExecutor(
        TurnTracker turnTracker, SpecialActionHandler specialActionHandler,
        AbilityManager abilityManager, GameEventProcessor eventProcessor) {
    return new DefaultActionExecutor(turnTracker, specialActionHandler, abilityManager, eventProcessor);
}
```

### Step 4: Run tests to verify they pass

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultActionExecutorTest" -DfailIfNoTests=false`
Expected: All tests PASS.

### Step 5: Run `ide_diagnostics` on modified files

### Step 6: Commit

```
Implement spell casting execution with mana payment
```

---

## Task 3: Battlefield.enter(Card, Player) orchestrates ETB

Currently `DefaultBattlefield.enter(Card, Player)` creates a Permanent and calls `enter(Permanent)` directly. It needs to process the ETB event through `GameEventProcessor` instead, so replacement effects and triggers fire.

**Files:**
- Modify: `DefaultBattlefield` — inject `GameEventProcessor`, change `enter(Card, Player)` and `enter(Token, Player)`
- Modify: `DefaultBattlefieldTest` — add tests for event processing
- Modify: `SharedZonesModule` — wire `GameEventProcessor`
- Modify: `SpecialActionHandler` — simplify `playLand` to delegate to `Battlefield.enter`
- Modify: `ZoneChangeResolver` — update `resolveEntersBattlefield` to not re-remove source from zone when entering from stack

### Step 1: Write failing tests for Battlefield ETB orchestration

In `DefaultBattlefieldTest`, add `GameEventProcessor` mock and tests:

```java
// Update fields and setUp:
GameEventProcessor eventProcessor;

@BeforeEach
void setUp() {
    player1 = mock(Player.class);
    player2 = mock(Player.class);
    eventProcessor = mock(GameEventProcessor.class);
    battlefield = new DefaultBattlefield(new ObjectStore(), eventProcessor);
}
```

Add new nested class:

```java
@Nested
class EnterFromCard {

    @Test
    void processesEntersBattlefieldEvent() {
        var card = createCreatureCard(player1, "Bear");

        battlefield.enter(card, player1);

        verify(eventProcessor).process(any(EntersBattlefieldEvent.class));
    }

    @Test
    void eventContainsCorrectPermanent() {
        var card = createCreatureCard(player1, "Bear");

        var permanent = battlefield.enter(card, player1);

        var captor = ArgumentCaptor.forClass(EntersBattlefieldEvent.class);
        verify(eventProcessor).process(captor.capture());
        var event = (EntersBattlefieldEvent) captor.getValue();
        assertThat(event.permanent()).isEqualTo(permanent);
    }
}
```

### Step 2: Run tests to verify they fail

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultBattlefieldTest" -DfailIfNoTests=false`
Expected: Compilation error — constructor doesn't accept `GameEventProcessor`.

### Step 3: Implement

In `DefaultBattlefield`:

```java
private final ObjectStore store;
private final GameEventProcessor eventProcessor;

public DefaultBattlefield(ObjectStore store, GameEventProcessor eventProcessor) {
    this.store = store;
    this.eventProcessor = eventProcessor;
}

@Override
public Permanent enter(Card card, Player controller) {
    var permanent = Permanent.fromCard(card, controller).build();
    var event = new EntersBattlefieldEvent(permanent, ZoneType.HAND);
    eventProcessor.process(event);
    return permanent;
}

@Override
public Permanent enter(Token token, Player controller) {
    var permanent = Permanent.fromToken(token, controller).build();
    var event = new EntersBattlefieldEvent(permanent, ZoneType.BATTLEFIELD);
    eventProcessor.process(event);
    return permanent;
}

// enter(Permanent) stays as-is — low-level add, called by ZoneChangeResolver
```

Note: The `from` zone for `enter(Card, Player)` defaults to `ZoneType.HAND` here. When called from Stack resolution, we'll use a different method signature (Task 4 will call `enter(Permanent)` low-level via the event processor chain, or we add an overload). Actually — looking at this more carefully, `Stack.resolve()` should call `battlefield.enter(card, controller)` but the `from` zone should be `STACK`, not `HAND`. We need an overload:

```java
// In Battlefield interface, add:
Permanent enter(Card card, Player controller, ZoneType from);

// In DefaultBattlefield:
@Override
public Permanent enter(Card card, Player controller) {
    return enter(card, controller, ZoneType.HAND);
}

@Override
public Permanent enter(Card card, Player controller, ZoneType from) {
    var permanent = Permanent.fromCard(card, controller).build();
    var event = new EntersBattlefieldEvent(permanent, from);
    eventProcessor.process(event);
    return permanent;
}
```

Update `SharedZonesModule`:

```java
@Provides
@Singleton
Battlefield provideBattlefield(ObjectStore store, GameEventProcessor eventProcessor) {
    return new DefaultBattlefield(store, eventProcessor);
}
```

Simplify `DefaultSpecialActionHandler.playLand()`:

```java
@Override
public ExecutionResult playLand(PlayerAction.PlayLand action, GameState state) {
    var land = action.land();
    var player = action.player();

    // Record the land play
    eventBus.post(new LandPlayedEvent(player, land));

    // Battlefield handles permanent creation and ETB event processing
    state.battlefield().enter(land, player);

    return new ExecutionResult.Success(List.of());
}
```

### Step 4: Run tests to verify they pass

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultBattlefieldTest,DefaultSpecialActionHandlerTest" -DfailIfNoTests=false`
Expected: All tests PASS. Some existing tests may need their `DefaultBattlefield` constructor calls updated to include the mock `GameEventProcessor`.

### Step 5: Run full test suite

Run: `./mvnw test -pl mtg-engine`
Expected: All tests PASS.

### Step 6: Run `ide_diagnostics` on modified files

### Step 7: Commit

```
Make Battlefield.enter orchestrate ETB event processing
```

---

## Task 4: Stack.resolve() — resolve top of stack

**Files:**
- Modify: `Stack` — add `resolve()` method
- Modify: `DefaultStack` — inject `GameEventProcessor` and `GameState`, implement `resolve()`
- Modify: `DefaultStackTest` — add resolution tests
- Modify: `SharedZonesModule` — wire new dependencies
- Modify: `TurnTracker` — replace `stack.pop()` with `stack.resolve()`
- Modify: `TurnTrackerTest` — update stack resolution tests

### Step 1: Write failing tests for Stack.resolve()

In `DefaultStackTest`, add `GameEventProcessor` and `GameState` mocks, update constructor:

```java
GameEventProcessor eventProcessor;
GameState gameState;
Battlefield battlefield;

@BeforeEach
void setUp() {
    player = mock(Player.class);
    eventProcessor = mock(GameEventProcessor.class);
    gameState = mock(GameState.class);
    battlefield = mock(Battlefield.class);
    when(gameState.battlefield()).thenReturn(battlefield);
    stack = new DefaultStack(new ObjectStore(), eventProcessor, gameState);
}
```

Add new nested class:

```java
@Nested
class ResolveOperations {

    @Test
    void resolveOnEmptyStackDoesNothing() {
        stack.resolve();

        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void resolvePermanentSpellEntersBattlefield() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Grizzly Bears")
                .type(Type.CREATURE)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();
        var spell = Spell.fromCard(card, player).build();
        stack.push(spell);

        stack.resolve();

        verify(battlefield).enter(card, player, ZoneType.STACK);
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void resolveInstantSpellGoesToGraveyard() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .type(Type.INSTANT)
                .build();
        var spell = Spell.fromCard(card, player).build();
        stack.push(spell);

        stack.resolve();

        verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void resolveSorcerySpellGoesToGraveyard() {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Divination")
                .type(Type.SORCERY)
                .build();
        var spell = Spell.fromCard(card, player).build();
        stack.push(spell);

        stack.resolve();

        verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void resolveAbilityOnStackJustRemovesIt() {
        var ability = createAbility();
        stack.push(ability);

        stack.resolve();

        assertThat(stack.isEmpty()).isTrue();
        // No events processed for abilities — they just cease to exist
    }

    @Test
    void resolveOnlyResolvesTopItem() {
        var spell1 = createSpell("Spell 1");
        var spell2 = createSpell("Spell 2");
        stack.push(spell1);
        stack.push(spell2);

        stack.resolve();

        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.peek()).contains(spell1);
    }
}
```

### Step 2: Run tests to verify they fail

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultStackTest" -DfailIfNoTests=false`
Expected: Compilation errors — `resolve()` doesn't exist yet.

### Step 3: Implement

In `Stack` interface, add:

```java
/// Resolves the top object on the stack ({@mtg.rule 608}).
///
/// For permanent spells, the card enters the battlefield.
/// For instant/sorcery spells, the card is put into the graveyard.
/// For abilities, they simply cease to exist.
void resolve();
```

In `DefaultStack`:

```java
private final ObjectStore store;
private final GameEventProcessor eventProcessor;
private final GameState gameState;

public DefaultStack(ObjectStore store, GameEventProcessor eventProcessor, GameState gameState) {
    this.store = store;
    this.eventProcessor = eventProcessor;
    this.gameState = gameState;
}

@Override
public void resolve() {
    var top = pop();
    if (top.isEmpty()) {
        return;
    }

    switch (top.get()) {
        case Spell spell -> resolveSpell(spell);
        case AbilityOnStack _ -> {} // Abilities cease to exist
    }
}

private void resolveSpell(Spell spell) {
    if (!(spell.source() instanceof Card card)) {
        return;
    }

    if (spell.types().isPermanentType()) {
        gameState.battlefield().enter(card, spell.controller(), ZoneType.STACK);
    } else {
        eventProcessor.process(new PutIntoGraveyardEvent(spell, ZoneType.STACK));
    }
}
```

Update `SharedZonesModule`:

```java
@Provides
@Singleton
Stack provideStack(ObjectStore store, GameEventProcessor eventProcessor, GameState gameState) {
    return new DefaultStack(store, eventProcessor, gameState);
}
```

### Step 4: Run tests to verify they pass

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultStackTest" -DfailIfNoTests=false`
Expected: All tests PASS.

### Step 5: Run `ide_diagnostics` on modified files

### Step 6: Commit

```
Add Stack.resolve() for spell and ability resolution
```

---

## Task 5: TurnTracker uses Stack.resolve()

**Files:**
- Modify: `TurnTracker` — replace `stack.pop()` with `stack.resolve()`
- Modify: `TurnTrackerTest` — update stack resolution tests to verify `resolve()` instead of `pop()`

### Step 1: Write failing tests

In `TurnTrackerTest`, update the `StackResolution` nested class to verify `stack.resolve()` instead of `stack.pop()`:

```java
@Test
void resolvesTopOfStackWhenAllPlayersPassWithNonEmptyStack() {
    var tracker = createTracker(List.of(player1, player2));
    when(stack.isEmpty()).thenReturn(false, true);

    tracker.startGame(player1);

    tracker.passPriority(player1);
    tracker.passPriority(player2);

    verify(stack, times(1)).resolve();
}

// Similar updates for other tests: replace verify(stack).pop() with verify(stack).resolve()
```

### Step 2: Run tests to verify they fail

Run: `./mvnw test -pl mtg-engine -Dtest="TurnTrackerTest" -DfailIfNoTests=false`
Expected: Failures because `stack.resolve()` is not called yet (still calls `pop()`).

### Step 3: Implement

In `DefaultTurnTracker.handleAllPlayersPassed()`, replace:

```java
stack.pop();
// TODO: Actually resolve, not just pop
```

with:

```java
stack.resolve();
```

Also in `endTurnEarly()`, replace:

```java
while (!stack.isEmpty()) {
    stack.pop();
    // TODO: Actually exile, not just remove
}
```

with:

```java
while (!stack.isEmpty()) {
    stack.pop();
    // TODO: Exile these objects instead of just removing them
}
```

Keep `pop()` in `endTurnEarly()` since ending a turn early exiles stack objects per Rule 717.1a — it does not resolve them.

### Step 4: Run tests to verify they pass

Run: `./mvnw test -pl mtg-engine -Dtest="TurnTrackerTest" -DfailIfNoTests=false`
Expected: All tests PASS.

### Step 5: Run full test suite

Run: `./mvnw test -pl mtg-engine`
Expected: All tests PASS.

### Step 6: Run `ide_diagnostics` on modified files

### Step 7: Commit

```
Use Stack.resolve() in TurnTracker for spell resolution
```

---

## Task 6: Full build verification

### Step 1: Run full build with tests

Run: `./mvnw clean verify`
Expected: BUILD SUCCESS with all tests passing.

### Step 2: Commit all remaining changes (if any)

```
Implement basic spell casting end-to-end
```
