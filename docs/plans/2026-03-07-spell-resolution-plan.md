# Instant/Sorcery Spell Resolution Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** When an instant or sorcery spell resolves, execute its effects (deal damage, draw cards, destroy, etc.) against the game state before putting it into the graveyard.

**Architecture:** Convert SpellAbility from interface to record storing parsed effects. Add SpellContext to Spell carrying target choices. Create an EffectExecutor service that switches on the sealed Effect type. Wire into Stack.resolve() to execute effects before graveyard. Includes basic targeting (chosen at cast time, checked at resolution).

**Tech Stack:** Java 25, Guice (DI), Mockito + AssertJ (tests), Maven

---

## File Reference

All paths relative to `mtg-engine/src/`:

| Alias | Path |
|-------|------|
| `SpellAbility` | `main/java/be/imgn/mtg/engine/ability/SpellAbility.java` |
| `Ability` | `main/java/be/imgn/mtg/engine/ability/Ability.java` |
| `Spell` | `main/java/be/imgn/mtg/engine/object/Spell.java` |
| `DefaultSpell` | `main/java/be/imgn/mtg/engine/object/internal/DefaultSpell.java` |
| `DefaultStack` | `main/java/be/imgn/mtg/engine/zone/internal/DefaultStack.java` |
| `DefaultStackTest` | `test/java/be/imgn/mtg/engine/zone/internal/DefaultStackTest.java` |
| `CastEvent` | `main/java/be/imgn/mtg/engine/zone/CastEvent.java` |
| `ZoneChangeResolver` | `main/java/be/imgn/mtg/engine/resolver/internal/ZoneChangeResolver.java` |
| `PlayerAction` | `main/java/be/imgn/mtg/engine/action/PlayerAction.java` |
| `Executor` | `main/java/be/imgn/mtg/engine/action/internal/DefaultActionExecutor.java` |
| `ExecutorTest` | `test/java/be/imgn/mtg/engine/action/internal/DefaultActionExecutorTest.java` |
| `SharedZonesModule` | `main/java/be/imgn/mtg/engine/zone/internal/SharedZonesModule.java` |
| `Effect` | `main/java/be/imgn/mtg/engine/ability/internal/parser/effect/Effect.java` |
| `Subject` | `main/java/be/imgn/mtg/engine/ability/internal/parser/reference/Subject.java` |
| `Amount` | `main/java/be/imgn/mtg/engine/ability/internal/parser/selector/Amount.java` |
| `PlayerReference` | `main/java/be/imgn/mtg/engine/selector/PlayerReference.java` |
| `DamageEvent` | `main/java/be/imgn/mtg/engine/combat/DamageEvent.java` |
| `DamageTarget` | `main/java/be/imgn/mtg/engine/combat/DamageTarget.java` |
| `DrawEvent` | `main/java/be/imgn/mtg/engine/zone/DrawEvent.java` |
| `DiscardEvent` | `main/java/be/imgn/mtg/engine/zone/DiscardEvent.java` |
| `DiesEvent` | `main/java/be/imgn/mtg/engine/zone/DiesEvent.java` |

---

## Task 1: Convert SpellAbility from interface to record

Currently `SpellAbility` is a `non-sealed interface` extending `Ability`. Convert it to a record.

**Files:**
- Modify: `SpellAbility` (interface → record)
- Modify: `Ability` (update permits clause from interface to record — should work as-is since records can implement sealed interfaces)
- Check/update: any files referencing `SpellAbility`

### Step 1: Find all references to SpellAbility

Search for all usages of `SpellAbility` across the codebase using `ide_find_references`. Key files likely include:
- `Ability.java` (permits clause)
- `StaticAbilityScanner` (switch case on SpellAbility)
- Tests

### Step 2: Write a test

Create `mtg-engine/src/test/java/be/imgn/mtg/engine/ability/SpellAbilityTest.java`:

```java
package be.imgn.mtg.engine.ability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.DrawEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

class SpellAbilityTest {

    @Test
    void storesEffectsAndOracleText() {
        var effect = new DrawEffect(java.util.Optional.empty(), new Amount.Exact(2));
        var ability = new SpellAbility(new AbilityId(), "Draw two cards.", List.of(effect));

        assertThat(ability.oracleText()).isEqualTo("Draw two cards.");
        assertThat(ability.effects()).containsExactly(effect);
        assertThat(ability.id()).isNotNull();
    }

    @Test
    void implementsAbility() {
        var ability = new SpellAbility(new AbilityId(), "Gain 3 life.", List.of());

        assertThat(ability).isInstanceOf(Ability.class);
    }
}
```

### Step 3: Run tests to see them fail

Run: `./mvnw test -pl mtg-engine -Dtest="SpellAbilityTest" -DfailIfNoTests=false`
Expected: Compilation error — SpellAbility is still an interface.

### Step 4: Convert SpellAbility to record

Replace `SpellAbility.java` entirely:

```java
package be.imgn.mtg.engine.ability;

import java.util.List;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;

/// A spell ability ({@mtg.rule 113.3a}).
///
/// Spell abilities are the instructions followed when an instant or sorcery spell resolves.
/// They are part of a spell on the stack, not independent abilities.
///
/// @param id the unique ability identifier
/// @param oracleText the original rules text
/// @param effects the effects that execute when the spell resolves
/// @see Ability
public record SpellAbility(AbilityId id, String oracleText, List<Effect> effects) implements Ability {}
```

Update any switch cases that reference `SpellAbility` — likely in `StaticAbilityScanner` where `case SpellAbility ignored -> {}` should become `case SpellAbility _ -> {}` (should work with records too).

### Step 5: Run tests

Run: `./mvnw test -pl mtg-engine -Dtest="SpellAbilityTest" -DfailIfNoTests=false`
Expected: PASS

### Step 6: Run full tests to check for regressions

Run: `./mvnw test -pl mtg-engine`

### Step 7: Run `ide_diagnostics` on modified files

### Step 8: Commit

```
Convert SpellAbility from interface to record
```

---

## Task 2: Add SpellContext and TargetChoices to Spell

Create the SpellContext record that carries casting-time decisions, and add it to Spell.

**Files:**
- Create: `main/java/be/imgn/mtg/engine/spell/SpellContext.java`
- Create: `main/java/be/imgn/mtg/engine/spell/TargetChoices.java`
- Create: `main/java/be/imgn/mtg/engine/spell/TargetChoice.java`
- Modify: `Spell` — add `SpellContext context()` method
- Modify: `DefaultSpell` — add context field, builder support
- Modify: `DefaultSpell.fromCard()` — default to `SpellContext.empty()`

### Step 1: Write failing test

Add to an existing test or create `mtg-engine/src/test/java/be/imgn/mtg/engine/spell/SpellContextTest.java`:

```java
package be.imgn.mtg.engine.spell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SpellContextTest {

    @Test
    void emptyContextHasNoTargets() {
        var context = SpellContext.empty();

        assertThat(context.targets().choices()).isEmpty();
    }

    @Test
    void contextStoresTargetChoices() {
        var targets = new TargetChoices(List.of());
        var context = new SpellContext(targets);

        assertThat(context.targets()).isEqualTo(targets);
    }
}
```

Also test that Spell carries context. In an existing Spell test or `DefaultSpellTest`, add:

```java
@Test
void spellFromCardHasEmptyContext() {
    var card = Card.builder()
            .owner(player)
            .controller(player)
            .name("Lightning Bolt")
            .type(Type.INSTANT)
            .build();
    var spell = Spell.fromCard(card, player).build();

    assertThat(spell.context()).isEqualTo(SpellContext.empty());
}

@Test
void spellBuilderAcceptsContext() {
    var context = new SpellContext(new TargetChoices(List.of()));
    var card = Card.builder()
            .owner(player)
            .controller(player)
            .name("Lightning Bolt")
            .type(Type.INSTANT)
            .build();
    var spell = Spell.fromCard(card, player).context(context).build();

    assertThat(spell.context()).isEqualTo(context);
}
```

### Step 2: Run tests to see them fail

### Step 3: Implement

**Create `SpellContext.java`:**

```java
package be.imgn.mtg.engine.spell;

/// Context for a spell on the stack, carrying casting-time decisions.
///
/// @param targets the targets chosen when the spell was cast
public record SpellContext(TargetChoices targets) {

    /// Returns an empty context with no targets.
    public static SpellContext empty() {
        return new SpellContext(TargetChoices.empty());
    }
}
```

**Create `TargetChoices.java`:**

```java
package be.imgn.mtg.engine.spell;

import java.util.List;

import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;

/// The target choices made when casting a spell.
///
/// @param choices the ordered list of target choices
public record TargetChoices(List<TargetChoice> choices) {

    /// Returns empty target choices.
    public static TargetChoices empty() {
        return new TargetChoices(List.of());
    }

    /// Finds the chosen target for a given subject, if any.
    public java.util.Optional<Selectable> findTarget(Subject subject) {
        return choices.stream()
                .filter(c -> c.subject().equals(subject))
                .map(TargetChoice::target)
                .findFirst();
    }
}
```

**Create `TargetChoice.java`:**

```java
package be.imgn.mtg.engine.spell;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.selector.Selectable;

/// A single target choice: the subject of an effect mapped to the chosen target.
///
/// @param subject the effect's subject that requires targeting
/// @param target the chosen target (a game object or player)
public record TargetChoice(Subject subject, Selectable target) {}
```

**Add to `Spell.java`** (after `source()` method):

```java
/// Returns the spell context carrying casting-time decisions.
///
/// @return the spell context, never null
SpellContext context();
```

**Update `DefaultSpell.java`:**

Add field:
```java
private final SpellContext context;
```

In constructor, read from builder:
```java
this.context = builder.context;
```

Add getter:
```java
@Override
public SpellContext context() {
    return context;
}
```

In `fromCard()` builder chain, add:
```java
// No explicit context() call — builder defaults to SpellContext.empty()
```

In Builder class, add field and method:
```java
private SpellContext context = SpellContext.empty();

public Builder context(SpellContext context) {
    this.context = context;
    return this;
}
```

Import `SpellContext` in Spell.java and DefaultSpell.java:
```java
import be.imgn.mtg.engine.spell.SpellContext;
```

### Step 4: Run tests

### Step 5: Run full test suite

### Step 6: Run `ide_diagnostics`

### Step 7: Commit

```
Add SpellContext with target choices to Spell
```

---

## Task 3: Carry SpellContext through CastSpell → CastEvent → Spell

The SpellContext needs to flow from the player action through to the Spell on the stack.

**Files:**
- Modify: `PlayerAction` — add SpellContext to CastSpell record
- Modify: `CastEvent` — add SpellContext field
- Modify: `ZoneChangeResolver` — pass context to Spell builder in `resolveCast()`
- Modify: `Executor` — pass context from action to CastEvent
- Modify: `ExecutorTest` — update CastSpell construction to include SpellContext
- Modify: `ValidatorTest` — update CastSpell construction

### Step 1: Update tests first

In `ExecutorTest`, update all `new PlayerAction.CastSpell(player, card)` to include `SpellContext.empty()`:

```java
new PlayerAction.CastSpell(player, card, SpellContext.empty())
```

Similarly update `ValidatorTest`.

Add a new test in `ExecutorTest`:

```java
@Test
void castEventCarriesSpellContext() {
    var target = mock(Selectable.class);
    var subject = new Subject.Select(mock(Selector.class));
    var context = new SpellContext(new TargetChoices(List.of(new TargetChoice(subject, target))));
    var card = Card.builder()
            .owner(player).controller(player)
            .name("Lightning Bolt").type(Type.INSTANT)
            .manaCost(ManaCost.parse("{R}"))
            .build();
    var action = new PlayerAction.CastSpell(player, card, context);

    executor.execute(action, gameState);

    var captor = ArgumentCaptor.forClass(CastEvent.class);
    verify(eventProcessor).process(captor.capture());
    assertThat(captor.getValue().context()).isEqualTo(context);
}
```

### Step 2: Run tests to see compilation errors

### Step 3: Implement

**Update `PlayerAction.CastSpell`:**

```java
/// @param player the player casting
/// @param card the card being cast as a spell
/// @param context the spell context with targets and other casting decisions
record CastSpell(Player player, Card card, SpellContext context) implements PlayerAction {}
```

Add import for `SpellContext`.

**Update `CastEvent`:**

Read `CastEvent.java` first. Add a `SpellContext context` field to the record.

**Update `DefaultActionExecutor.executeCastSpell()`:**

Pass `cast.context()` to CastEvent:

```java
var castEvent = new CastEvent(card, ZoneType.HAND, player, cast.context());
```

**Update `ZoneChangeResolver.resolveCast()`:**

Currently:
```java
var newSpell = Spell.fromCard(card, event.caster()).build();
```

Change to:
```java
var newSpell = Spell.fromCard(card, event.caster()).context(event.context()).build();
```

### Step 4: Run tests

### Step 5: Run full test suite — fix any compilation errors in other test files that construct CastSpell or CastEvent

### Step 6: Run `ide_diagnostics`

### Step 7: Commit

```
Carry SpellContext through casting flow to Spell on stack
```

---

## Task 4: Create EffectExecutor framework

Create the EffectExecutor interface and DefaultEffectExecutor with a switch that throws UnsupportedOperationException for all effects. Also create ResolutionContext.

**Files:**
- Create: `main/java/be/imgn/mtg/engine/resolver/EffectExecutor.java`
- Create: `main/java/be/imgn/mtg/engine/resolver/ResolutionContext.java`
- Create: `main/java/be/imgn/mtg/engine/resolver/internal/DefaultEffectExecutor.java`
- Create: `test/java/be/imgn/mtg/engine/resolver/internal/DefaultEffectExecutorTest.java`
- Modify: resolver Guice module to bind EffectExecutor

### Step 1: Write failing test

```java
package be.imgn.mtg.engine.resolver.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.TapEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.resolver.ResolutionContext;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.state.GameState;

class DefaultEffectExecutorTest {

    private DefaultEffectExecutor executor;
    private GameEventProcessor eventProcessor;
    private GameState gameState;
    private Player controller;
    private ResolutionContext context;

    @BeforeEach
    void setUp() {
        executor = new DefaultEffectExecutor();
        eventProcessor = mock(GameEventProcessor.class);
        gameState = mock(GameState.class);
        controller = mock(Player.class);
        context = new ResolutionContext(gameState, controller, SpellContext.empty(), eventProcessor);
    }

    @Nested
    class UnsupportedEffects {

        @Test
        void throwsForUnimplementedEffect() {
            var effect = new TapEffect(new Subject.Select(mock(be.imgn.mtg.engine.selector.Selector.class)));

            assertThatThrownBy(() -> executor.execute(effect, context))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
```

### Step 2: Run to see compilation error

### Step 3: Implement

**Create `ResolutionContext.java`:**

```java
package be.imgn.mtg.engine.resolver;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.state.GameState;

/// Context available during spell/ability resolution.
///
/// @param gameState the current game state
/// @param controller the spell/ability controller
/// @param spellContext the casting-time decisions (targets, etc.)
/// @param eventProcessor the event processor for game state changes
public record ResolutionContext(
        GameState gameState,
        Player controller,
        SpellContext spellContext,
        GameEventProcessor eventProcessor) {}
```

**Create `EffectExecutor.java`:**

```java
package be.imgn.mtg.engine.resolver;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;

/// Executes parsed effects against the game state during spell/ability resolution.
public interface EffectExecutor {

    /// Executes the given effect.
    ///
    /// @param effect the effect to execute
    /// @param context the resolution context
    void execute(Effect effect, ResolutionContext context);
}
```

**Create `DefaultEffectExecutor.java`:**

```java
package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.ability.internal.parser.effect.*;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.resolver.ResolutionContext;

/// Default implementation that switches on the sealed [Effect] type.
final class DefaultEffectExecutor implements EffectExecutor {

    @Override
    public void execute(Effect effect, ResolutionContext context) {
        switch (effect) {
            case CompoundEffect e -> e.effects().forEach(sub -> execute(sub, context));
            default -> throw new UnsupportedOperationException(
                    "Effect not yet implemented: " + effect.getClass().getSimpleName());
        }
    }
}
```

Note: Start with `default` throwing for all effects and `CompoundEffect` delegating. Task 5 adds the actual implementations.

**Wire in Guice module:** Find the resolver module (likely `main/java/be/imgn/mtg/engine/resolver/internal/ResolverModule.java`) and add:

```java
@Provides
@Singleton
EffectExecutor provideEffectExecutor() {
    return new DefaultEffectExecutor();
}
```

### Step 4: Run tests

### Step 5: Run `ide_diagnostics`

### Step 6: Commit

```
Create EffectExecutor framework with ResolutionContext
```

---

## Task 5: Implement starter effects

Add the 6 starter effect implementations to DefaultEffectExecutor.

**Files:**
- Modify: `DefaultEffectExecutor` — add effect handlers
- Modify: `DefaultEffectExecutorTest` — add tests per effect

### Step 1: Write failing tests for each effect

Add nested classes to `DefaultEffectExecutorTest`. For each effect, set up the context with appropriate mocks.

**Helper method for resolving Amount:**

```java
private int resolveAmount(Amount amount) {
    return switch (amount) {
        case Amount.Exact exact -> exact.value();
        default -> throw new UnsupportedOperationException("Only exact amounts supported");
    };
}
```

**GainLifeEffect test (simplest — no targets):**

```java
@Nested
class GainLifeEffectTests {

    @Test
    void controllerGainsLife() {
        var effect = new GainLifeEffect(Optional.empty(), new Amount.Exact(3));

        executor.execute(effect, context);

        verify(controller).gainLife(3);
    }

    @Test
    void specifiedPlayerGainsLife() {
        var effect = new GainLifeEffect(Optional.of(PlayerReference.YOU), new Amount.Exact(5));

        executor.execute(effect, context);

        verify(controller).gainLife(5);
    }
}
```

**DrawEffect test:**

```java
@Nested
class DrawEffectTests {

    @Test
    void controllerDrawsCards() {
        var library = mock(Library.class);
        var card1 = mock(Card.class);
        var card2 = mock(Card.class);
        when(gameState.library(controller)).thenReturn(library);
        when(library.peek()).thenReturn(Optional.of(card1), Optional.of(card2));

        var effect = new DrawEffect(Optional.empty(), new Amount.Exact(2));

        executor.execute(effect, context);

        verify(eventProcessor, times(2)).process(any(DrawEvent.class));
    }
}
```

**DealDamageEffect test (with targeting):**

```java
@Nested
class DealDamageEffectTests {

    @Test
    void dealsNonCombatDamageToTargetPlayer() {
        var targetPlayer = mock(Player.class);
        var selector = mock(Selector.class);
        var subject = new Subject.Select(selector);
        var targets = new TargetChoices(List.of(new TargetChoice(subject, targetPlayer)));
        var ctx = new ResolutionContext(gameState, controller, new SpellContext(targets), eventProcessor);

        var effect = new DealDamageEffect(new Amount.Exact(3), subject);

        executor.execute(effect, ctx);

        verify(eventProcessor).process(any(DamageEvent.class));
    }
}
```

**DestroyEffect test (with targeting):**

```java
@Nested
class DestroyEffectTests {

    @Test
    void destroysTargetPermanent() {
        var permanent = mock(Permanent.class);
        when(permanent.controller()).thenReturn(controller);
        var selector = mock(Selector.class);
        var subject = new Subject.Select(selector);
        var targets = new TargetChoices(List.of(new TargetChoice(subject, permanent)));
        var ctx = new ResolutionContext(gameState, controller, new SpellContext(targets), eventProcessor);

        var effect = new DestroyEffect(subject, true);

        executor.execute(effect, ctx);

        verify(eventProcessor).process(any(DiesEvent.class));
    }
}
```

**DiscardEffect test:**

```java
@Nested
class DiscardEffectTests {

    @Test
    void controllerDiscardsCard() {
        var hand = mock(Hand.class);
        var card = mock(Card.class);
        when(gameState.hand(controller)).thenReturn(hand);
        when(hand.stream()).thenReturn(java.util.stream.Stream.of(card));
        when(controller.choose(any())).thenReturn(List.of(card));

        var effect = new DiscardEffect(Optional.empty(), new Amount.Exact(1));

        executor.execute(effect, context);

        verify(eventProcessor).process(any(DiscardEvent.class));
    }
}
```

**CompoundEffect test:**

```java
@Nested
class CompoundEffectTests {

    @Test
    void executesAllSubEffects() {
        var effect = new CompoundEffect(List.of(
                new GainLifeEffect(Optional.empty(), new Amount.Exact(3)),
                new GainLifeEffect(Optional.empty(), new Amount.Exact(2))
        ));

        executor.execute(effect, context);

        verify(controller).gainLife(3);
        verify(controller).gainLife(2);
    }
}
```

### Step 2: Run tests to see them fail

### Step 3: Implement each effect handler

Update `DefaultEffectExecutor.execute()`:

```java
@Override
public void execute(Effect effect, ResolutionContext context) {
    switch (effect) {
        case DealDamageEffect e -> executeDealDamage(e, context);
        case DrawEffect e -> executeDraw(e, context);
        case DestroyEffect e -> executeDestroy(e, context);
        case GainLifeEffect e -> executeGainLife(e, context);
        case DiscardEffect e -> executeDiscard(e, context);
        case CompoundEffect e -> e.effects().forEach(sub -> execute(sub, context));
        default -> throw new UnsupportedOperationException(
                "Effect not yet implemented: " + effect.getClass().getSimpleName());
    }
}
```

**GainLifeEffect:**

```java
private void executeGainLife(GainLifeEffect effect, ResolutionContext ctx) {
    var player = resolvePlayer(effect.player(), ctx);
    var amount = resolveAmount(effect.amount());
    player.gainLife(amount);
}
```

**DrawEffect:**

```java
private void executeDraw(DrawEffect effect, ResolutionContext ctx) {
    var player = resolvePlayer(effect.player(), ctx);
    var amount = resolveAmount(effect.amount());
    var library = ctx.gameState().library(player);
    for (int i = 0; i < amount; i++) {
        library.peek().ifPresent(card ->
                ctx.eventProcessor().process(new DrawEvent(card, player)));
    }
}
```

**DealDamageEffect:**

```java
private void executeDealDamage(DealDamageEffect effect, ResolutionContext ctx) {
    var target = resolveSubject(effect.target(), ctx);
    var amount = resolveAmount(effect.amount());
    var damageTarget = switch (target) {
        case Player p -> new DamageTarget.PlayerTarget(p);
        case Permanent p -> {
            if (p.types().isCreature()) yield new DamageTarget.CreatureTarget(p);
            else if (p.types().isPlaneswalker()) yield new DamageTarget.PlaneswalkerTarget(p);
            else yield new DamageTarget.BattleTarget(p);
        }
        default -> throw new IllegalStateException("Cannot deal damage to: " + target);
    };
    // source is null for now — would be the spell's source card
    ctx.eventProcessor().process(new DamageEvent(null, damageTarget, amount, false, false));
}
```

Note: The `source` for DamageEvent ideally should be the spell/card. For now pass `null` and add a TODO — we can refine this when DamageEvent resolution is fully implemented.

**DestroyEffect:**

```java
private void executeDestroy(DestroyEffect effect, ResolutionContext ctx) {
    var target = resolveSubject(effect.subject(), ctx);
    if (target instanceof Permanent permanent) {
        ctx.eventProcessor().process(new DiesEvent(permanent, new DiesEvent.DeathCause.Destroyed()));
    }
}
```

**DiscardEffect:**

```java
private void executeDiscard(DiscardEffect effect, ResolutionContext ctx) {
    var player = resolvePlayer(effect.player(), ctx);
    var amount = resolveAmount(effect.amount());
    var hand = ctx.gameState().hand(player);
    var cards = hand.stream().toList();
    if (cards.isEmpty()) return;
    var choice = Choice.upTo(amount, cards.stream()
            .map(c -> new Option<>(c, c.name()))
            .toList(), "Choose cards to discard");
    var chosen = player.choose(choice);
    for (var card : chosen) {
        ctx.eventProcessor().process(new DiscardEvent(card, player));
    }
}
```

**Helper methods:**

```java
private Player resolvePlayer(Optional<PlayerReference> ref, ResolutionContext ctx) {
    if (ref.isEmpty() || ref.get() == PlayerReference.YOU) {
        return ctx.controller();
    }
    // TODO: Handle OPPONENT, EACH_PLAYER, etc.
    throw new UnsupportedOperationException("Player reference not yet implemented: " + ref.get());
}

private int resolveAmount(Amount amount) {
    return switch (amount) {
        case Amount.Exact exact -> exact.value();
        case Amount.Variable _ -> throw new UnsupportedOperationException("Variable amounts not yet implemented");
        case Amount.Reference _ -> throw new UnsupportedOperationException("Reference amounts not yet implemented");
    };
}

private Selectable resolveSubject(Subject subject, ResolutionContext ctx) {
    return switch (subject) {
        case Subject.Select select -> ctx.spellContext().targets().findTarget(subject)
                .orElseThrow(() -> new IllegalStateException("No target found for subject"));
        case Subject.Pronoun _ -> throw new UnsupportedOperationException("Pronoun resolution not yet implemented");
        case Subject.ThatObject _ -> throw new UnsupportedOperationException("ThatObject resolution not yet implemented");
    };
}
```

### Step 4: Run tests

### Step 5: Run `ide_diagnostics`

### Step 6: Commit

```
Implement starter effects in EffectExecutor
```

---

## Task 6: Wire Stack.resolve() to execute effects

Modify `DefaultStack.resolveSpell()` to find the SpellAbility, execute its effects, then put the spell in the graveyard.

**Files:**
- Modify: `DefaultStack` — inject EffectExecutor, update resolveSpell()
- Modify: `DefaultStackTest` — add resolution tests with effects
- Modify: `SharedZonesModule` — wire EffectExecutor

### Step 1: Write failing tests

Add to `DefaultStackTest`:

```java
@Nested
class SpellEffectResolution {

    @Test
    void executesSpellAbilityEffectsBeforeGraveyard() {
        var drawEffect = new DrawEffect(Optional.empty(), new Amount.Exact(1));
        var spellAbility = new SpellAbility(new AbilityId(), "Draw a card.", List.of(drawEffect));
        var card = Card.builder()
                .owner(player).controller(player)
                .name("Opt").type(Type.INSTANT)
                .addAbility(spellAbility)
                .build();
        var spell = Spell.fromCard(card, player).build();
        stack.push(spell);

        // Mock library for draw
        var library = mock(Library.class);
        var drawnCard = mock(Card.class);
        when(gameState.library(player)).thenReturn(library);
        when(library.peek()).thenReturn(Optional.of(drawnCard));

        stack.resolve();

        // Verify draw happened before graveyard
        var inOrder = inOrder(eventProcessor);
        inOrder.verify(eventProcessor).process(any(DrawEvent.class));
        inOrder.verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
    }

    @Test
    void spellWithoutSpellAbilityJustGoesToGraveyard() {
        var card = Card.builder()
                .owner(player).controller(player)
                .name("Mystery Spell").type(Type.SORCERY)
                .build();
        var spell = Spell.fromCard(card, player).build();
        stack.push(spell);

        stack.resolve();

        verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
    }

    @Test
    void fizzlesWhenAllTargetsIllegal() {
        var selector = mock(Selector.class);
        var subject = new Subject.Select(selector);
        var targetPermanent = mock(Permanent.class);
        var spellAbility = new SpellAbility(new AbilityId(), "Destroy target creature.",
                List.of(new DestroyEffect(subject, true)));
        var targets = new TargetChoices(List.of(new TargetChoice(subject, targetPermanent)));
        var context = new SpellContext(targets);
        var card = Card.builder()
                .owner(player).controller(player)
                .name("Murder").type(Type.INSTANT)
                .addAbility(spellAbility)
                .build();
        var spell = Spell.fromCard(card, player).context(context).build();
        stack.push(spell);

        // Target no longer on battlefield — selector doesn't match
        when(selector.matches(targetPermanent, player)).thenReturn(false);

        stack.resolve();

        // Should go to graveyard without executing effects
        verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
        verify(eventProcessor, never()).process(any(DiesEvent.class));
    }
}
```

### Step 2: Run tests to see them fail

### Step 3: Implement

**Inject EffectExecutor into DefaultStack:**

```java
private final EffectExecutor effectExecutor;

public DefaultStack(ObjectStore store, GameEventProcessor eventProcessor,
                    GameState gameState, EffectExecutor effectExecutor) {
    this.store = store;
    this.eventProcessor = eventProcessor;
    this.gameState = gameState;
    this.effectExecutor = effectExecutor;
}
```

**Update resolveSpell():**

```java
private void resolveSpell(Spell spell) {
    switch (spell.source()) {
        case Card card -> {
            if (spell.types().isPermanentType()) {
                gameState.battlefield().enter(card, spell.controller(), ZoneType.STACK);
            } else {
                executeSpellEffects(spell);
                eventProcessor.process(new PutIntoGraveyardEvent(spell, ZoneType.STACK));
            }
        }
        case CardCopy _ -> throw new UnsupportedOperationException("CardCopy resolution not yet implemented");
    }
}

private void executeSpellEffects(Spell spell) {
    // Find SpellAbility among the spell's abilities
    var spellAbility = spell.abilities().stream()
            .filter(a -> a instanceof SpellAbility)
            .map(SpellAbility.class::cast)
            .findFirst()
            .orElse(null);

    if (spellAbility == null || spellAbility.effects().isEmpty()) {
        return;
    }

    // Check target legality (Rule 608.2b)
    var targets = spell.context().targets();
    if (!targets.choices().isEmpty() && !hasLegalTarget(targets, spell.controller())) {
        return; // Fizzle — all targets illegal
    }

    // Execute effects
    var context = new ResolutionContext(gameState, spell.controller(), spell.context(), eventProcessor);
    for (var effect : spellAbility.effects()) {
        effectExecutor.execute(effect, context);
    }
}

private boolean hasLegalTarget(TargetChoices targets, Player controller) {
    return targets.choices().stream().anyMatch(choice -> {
        if (choice.subject() instanceof Subject.Select select) {
            return select.selector().matches(choice.target(), controller);
        }
        return true; // Non-Select subjects are always "legal"
    });
}
```

**Update SharedZonesModule:**

```java
@Provides
@Singleton
Stack provideStack(GameEventProcessor eventProcessor, GameState gameState, EffectExecutor effectExecutor) {
    return new DefaultStack(new ObjectStore(), eventProcessor, gameState, effectExecutor);
}
```

### Step 4: Run tests

### Step 5: Fix any constructor calls in other test files that create DefaultStack

### Step 6: Run full test suite

### Step 7: Run `ide_diagnostics`

### Step 8: Commit

```
Wire Stack.resolve() to execute spell effects before graveyard
```

---

## Task 7: Full build verification

### Step 1: Run full build

Run: `./mvnw clean verify`
Expected: BUILD SUCCESS

### Step 2: Commit any remaining changes

```
Implement instant/sorcery spell resolution with effect execution
```
