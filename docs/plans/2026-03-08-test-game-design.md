# TestGame Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a TestGame test utility with database-backed card creation, including prerequisite refactoring of ability parsing and a new type line parser.

**Architecture:** Seven tasks in sequence: (1) refactor ActivatedAbility from interface to record, (2) create SpellAbilityParser, (3) create AbilityParser as aggregating entry point, (4) create TypeLineParser using parser combinators, (5) create CardFetcher interface and CardModule, (6) implement DefaultCardFetcher with database-backed card creation, (7) create TestGame test utility.

**Tech Stack:** Java 25, Guice, H2, JDBI, dot-parse (com.google.mug parser combinators), JUnit 6, AssertJ

---

### Task 1: ActivatedAbility — interface to record

Convert `ActivatedAbility` from a `non-sealed interface` to a `record` implementing `Ability`. Absorb fields and factory method from `ParsedActivatedAbility`. Delete `ParsedActivatedAbility`.

**Files:**
- Modify: `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/ActivatedAbility.java`
- Delete: `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/ParsedActivatedAbility.java`
- Modify: `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/ActivatedAbilityParser.java`
- Modify: all files referencing `ParsedActivatedAbility` or `ActivatedAbility` (use IDE refactoring)
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/ability/internal/parser/ActivatedAbilityParserTest.java` (existing, should still pass)

**Context:**

Current `ActivatedAbility` is a `non-sealed interface extends Ability` with methods: `cost()`, `effect()`, `timing()`, `limit()`, `activatesFrom()`, `isManaAbility()`, `isLoyaltyAbility()`, `canActivate()`. Its only implementation is `ParsedActivatedAbility` — a record in `ability.internal.parser` with the same fields plus a `create(Cost, Effect)` factory method.

Current `Ability` is sealed: `permits SpellAbility, ActivatedAbility, TriggeredAbility, StaticAbility`.

**Step 1: Convert ActivatedAbility to a record**

Replace the interface with a record. Move all fields from `ParsedActivatedAbility` and all default methods from the interface:

```java
package be.imgn.mtg.engine.ability;

import java.util.Set;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.zone.ZoneType;

/// An activated ability ({@mtg.rule 113.3b}).
///
/// Activated abilities have the format "Cost: Effect." They are activated by a player
/// who has priority and can pay the cost.
///
/// @param id the unique ability identifier
/// @param oracleText the original oracle text
/// @param cost the activation cost
/// @param effect the effect produced on resolution
/// @param timing when this ability can be activated
/// @param limit how often this ability can be activated
/// @param activatesFrom the zones this ability can be activated from
public record ActivatedAbility(
        AbilityId id,
        String oracleText,
        Cost cost,
        Effect effect,
        ActivationTiming timing,
        ActivationLimit limit,
        Set<ZoneType> activatesFrom)
        implements Ability {

    /// Creates an ActivatedAbility from a parsed cost and effect.
    ///
    /// Automatically determines timing and limit:
    /// - Loyalty cost → SORCERY timing, once per turn
    /// - Mana ability effect (no target, adds mana) → MANA_ABILITY timing
    /// - Otherwise → INSTANT timing, unlimited
    ///
    /// @param cost the parsed cost
    /// @param effect the parsed effect
    /// @return the constructed activated ability
    public static ActivatedAbility create(Cost cost, Effect effect) {
        var timing = cost.isLoyaltyCost()
                ? ActivationTiming.SORCERY
                : effect.isManaAbilityEffect() ? ActivationTiming.MANA_ABILITY : ActivationTiming.INSTANT;
        var limit = cost.isLoyaltyCost() ? OncePerTurn.INSTANCE : ActivationLimit.UNLIMITED;
        var oracleText = cost.description() + ": " + "...";
        return new ActivatedAbility(
                new AbilityId(), oracleText, cost, effect, timing, limit, Set.of(ZoneType.BATTLEFIELD));
    }

    /// Returns true if this is a mana ability ({@mtg.rule 605.1a}).
    public boolean isManaAbility() {
        return !isLoyaltyAbility() && effect.isManaAbilityEffect();
    }

    /// Returns true if this is a loyalty ability ({@mtg.rule 606.1}).
    public boolean isLoyaltyAbility() {
        return cost.isLoyaltyCost();
    }

    /// Returns true if this ability can currently be activated ({@mtg.rule 602.2}).
    public boolean canActivate(ActivationContext context) {
        return limit.canActivate(id, context.tracker());
    }
}
```

**Step 2: Update ActivatedAbilityParser**

Change return types from `ParsedActivatedAbility` to `ActivatedAbility`:

```java
package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.cost.internal.CostParser;

/// Parser for activated abilities in oracle text ({@mtg.rule 113.3b}).
public final class ActivatedAbilityParser {

    private ActivatedAbilityParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses an activated ability: "Cost: Effect."
    public static final Parser<ActivatedAbility> ACTIVATED_ABILITY =
            sequence(CostParser.COST.followedBy(":"), EffectParser.EFFECT, ActivatedAbility::create);

    /// Parses an activated ability from oracle text, skipping whitespace.
    public static ActivatedAbility parse(String oracleText) {
        return ACTIVATED_ABILITY.parseSkipping(WHITESPACE, oracleText);
    }
}
```

**Step 3: Delete ParsedActivatedAbility.java**

Delete `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/ParsedActivatedAbility.java`.

**Step 4: Update all references**

Use `ide_find_references` on `ParsedActivatedAbility` and update any remaining imports. Key files to check:
- Test files referencing `ParsedActivatedAbility`
- Any files importing `ParsedActivatedAbility`

**Step 5: Run tests to verify**

Run: `./mvnw test -pl mtg-engine -Dtest="ActivatedAbilityParserTest,ActivatedAbilityHandlerTest,ManaAbilityHandlerTest,LoyaltyAbilityHandlerTest,DefaultAbilityManagerTest,DefaultActionExecutorTest,DefaultActionValidatorTest" -DfailIfNoTests=false`
Expected: All tests pass.

**Step 6: Run ide_diagnostics on changed files**

Check `ActivatedAbility.java` and `ActivatedAbilityParser.java` for warnings/errors.

**Step 7: Commit**

```bash
git add -A
git commit -m "Convert ActivatedAbility from interface to record"
```

---

### Task 2: SpellAbilityParser

Create a parser combinator that parses oracle text for instant/sorcery cards into `SpellAbility` records.

**Files:**
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/SpellAbilityParser.java`
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/ability/internal/parser/SpellAbilityParserTest.java`

**Context:**

`SpellAbility` is a record: `SpellAbility(AbilityId id, String oracleText, List<Effect> effects) implements Ability`.

`EffectParser.EFFECT` parses a single effect. Spell abilities consist of one or more effects. Multiple effects on a single line are separated by commas or are compound: "Deal 3 damage to target creature and you gain 3 life."

For now, SpellAbilityParser handles single-effect spell text. The key difference from ActivatedAbilityParser: there is no cost prefix — the entire text is the effect.

**Step 1: Write the test**

```java
package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DrawEffect;

@DisplayName("SpellAbilityParser")
class SpellAbilityParserTest {

    @Nested
    @DisplayName("Single effect spells")
    class SingleEffect {

        @Test
        @DisplayName("\"Deal 3 damage to any target.\" → DealDamageEffect")
        void dealDamage() {
            var ability = SpellAbilityParser.parse("Deal 3 damage to any target.");

            assertThat(ability.id()).isNotNull();
            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DealDamageEffect.class);
        }

        @Test
        @DisplayName("\"Destroy target creature.\" → DestroyEffect")
        void destroy() {
            var ability = SpellAbilityParser.parse("Destroy target creature.");

            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DestroyEffect.class);
        }

        @Test
        @DisplayName("\"Draw two cards.\" → DrawEffect")
        void draw() {
            var ability = SpellAbilityParser.parse("Draw two cards.");

            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DrawEffect.class);
        }
    }

    @Nested
    @DisplayName("Ability properties")
    class Properties {

        @Test
        @DisplayName("parsed spell ability stores oracle text")
        void storesOracleText() {
            var ability = SpellAbilityParser.parse("Deal 3 damage to any target.");

            assertThat(ability.oracleText()).isEqualTo("Deal 3 damage to any target.");
        }

        @Test
        @DisplayName("different parsed abilities have different IDs")
        void uniqueIds() {
            var a1 = SpellAbilityParser.parse("Deal 3 damage to any target.");
            var a2 = SpellAbilityParser.parse("Deal 3 damage to any target.");

            assertThat(a1.id()).isNotEqualTo(a2.id());
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -pl mtg-engine -Dtest="SpellAbilityParserTest" -DfailIfNoTests=false`
Expected: Compilation error — `SpellAbilityParser` does not exist.

**Step 3: Write the implementation**

```java
package be.imgn.mtg.engine.ability.internal.parser;

import java.util.List;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.SpellAbility;

/// Parser for spell abilities in oracle text ({@mtg.rule 113.3a}).
///
/// Spell abilities are the instructions on an instant or sorcery spell.
/// Unlike activated abilities, they have no cost prefix — the entire text
/// describes the effect.
///
/// Examples:
/// - "Deal 3 damage to any target."
/// - "Destroy target creature."
/// - "Draw two cards."
public final class SpellAbilityParser {

    private SpellAbilityParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses a spell ability from a single line of oracle text.
    public static final Parser<SpellAbility> SPELL_ABILITY =
            EffectParser.EFFECT.map(effect ->
                    new SpellAbility(new AbilityId(), "", List.of(effect)));

    /// Parses a spell ability from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed spell ability
    public static SpellAbility parse(String oracleText) {
        var ability = SPELL_ABILITY.parseSkipping(WHITESPACE, oracleText);
        return new SpellAbility(ability.id(), oracleText, ability.effects());
    }
}
```

**Step 4: Run tests**

Run: `./mvnw test -pl mtg-engine -Dtest="SpellAbilityParserTest" -DfailIfNoTests=false`
Expected: All tests pass.

**Step 5: Run ide_diagnostics**

Check `SpellAbilityParser.java` and `SpellAbilityParserTest.java`.

**Step 6: Commit**

```bash
git add mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/SpellAbilityParser.java mtg-engine/src/test/java/be/imgn/mtg/engine/ability/internal/parser/SpellAbilityParserTest.java
git commit -m "Add SpellAbilityParser for instant/sorcery oracle text"
```

---

### Task 3: AbilityParser

Create the main entry point for parsing oracle text into abilities. Aggregates `ActivatedAbilityParser` and `SpellAbilityParser`.

**Files:**
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/AbilityParser.java`
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/ability/internal/parser/AbilityParserTest.java`

**Context:**

Oracle text on cards is split by newlines (`\n`). Each line is a separate ability. The parser tries activated ability first (has ":" separator), falls back to spell ability.

Some lines may not parse (keywords like "Flying", "Haste", or text we don't support yet). These should be silently skipped — return only the abilities we can parse.

**Step 1: Write the test**

```java
package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.SpellAbility;

@DisplayName("AbilityParser")
class AbilityParserTest {

    @Nested
    @DisplayName("Single line")
    class SingleLine {

        @Test
        @DisplayName("activated ability line → ActivatedAbility")
        void activatedAbility() {
            var abilities = AbilityParser.parse("{T}: Add {G}.");

            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(ActivatedAbility.class);
        }

        @Test
        @DisplayName("spell effect line → SpellAbility")
        void spellAbility() {
            var abilities = AbilityParser.parse("Deal 3 damage to any target.");

            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(SpellAbility.class);
        }
    }

    @Nested
    @DisplayName("Multiple lines")
    class MultipleLines {

        @Test
        @DisplayName("two activated abilities on separate lines")
        void twoActivated() {
            var abilities = AbilityParser.parse("{T}: Add {G}.\n{T}: Add {R}.");

            assertThat(abilities).hasSize(2);
            assertThat(abilities).allSatisfy(a -> assertThat(a).isInstanceOf(ActivatedAbility.class));
        }
    }

    @Nested
    @DisplayName("Unparseable lines")
    class UnparseableLines {

        @Test
        @DisplayName("keyword ability line is silently skipped")
        void keywordSkipped() {
            var abilities = AbilityParser.parse("Flying");

            assertThat(abilities).isEmpty();
        }

        @Test
        @DisplayName("parseable + unparseable lines → only parseable returned")
        void mixedLines() {
            var abilities = AbilityParser.parse("Flying\n{T}: Add {G}.");

            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(ActivatedAbility.class);
        }

        @Test
        @DisplayName("empty oracle text → empty list")
        void emptyText() {
            var abilities = AbilityParser.parse("");

            assertThat(abilities).isEmpty();
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -pl mtg-engine -Dtest="AbilityParserTest" -DfailIfNoTests=false`
Expected: Compilation error — `AbilityParser` does not exist.

**Step 3: Write the implementation**

```java
package be.imgn.mtg.engine.ability.internal.parser;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.ability.Ability;

/// Main entry point for parsing oracle text into abilities.
///
/// Splits oracle text by newlines and tries each parser in order:
/// 1. [ActivatedAbilityParser] — "Cost: Effect." format
/// 2. [SpellAbilityParser] — standalone effect text
///
/// Lines that cannot be parsed (keywords, unsupported text) are silently skipped.
public final class AbilityParser {

    private AbilityParser() {}

    /// Parses oracle text into a list of abilities.
    ///
    /// @param oracleText the full oracle text (may contain newlines)
    /// @return the parsed abilities, never null (may be empty)
    public static List<Ability> parse(String oracleText) {
        if (oracleText == null || oracleText.isBlank()) {
            return List.of();
        }

        var abilities = new ArrayList<Ability>();
        for (var line : oracleText.split("\n")) {
            var trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            var ability = tryParseLine(trimmed);
            if (ability != null) {
                abilities.add(ability);
            }
        }
        return List.copyOf(abilities);
    }

    private static Ability tryParseLine(String line) {
        // Try activated ability first (has ":" separator)
        try {
            return ActivatedAbilityParser.parse(line);
        } catch (Exception _) {
            // Not an activated ability
        }

        // Try spell ability (standalone effect)
        try {
            return SpellAbilityParser.parse(line);
        } catch (Exception _) {
            // Not a parseable effect
        }

        return null;
    }
}
```

**Step 4: Run tests**

Run: `./mvnw test -pl mtg-engine -Dtest="AbilityParserTest" -DfailIfNoTests=false`
Expected: All tests pass.

**Step 5: Run ide_diagnostics**

Check `AbilityParser.java`.

**Step 6: Commit**

```bash
git add mtg-engine/src/main/java/be/imgn/mtg/engine/ability/internal/parser/AbilityParser.java mtg-engine/src/test/java/be/imgn/mtg/engine/ability/internal/parser/AbilityParserTest.java
git commit -m "Add AbilityParser as aggregating entry point for oracle text parsing"
```

---

### Task 4: TypeLineParser

Create a parser combinator that parses MTG type lines into Supertypes, Types, and Subtypes.

**Files:**
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/characteristics/internal/TypeLineParser.java`
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/characteristics/internal/TypeLineParserTest.java`

**Context:**

Type lines follow the pattern: `[Supertypes] Types [— Subtypes]`

Examples:
- `"Creature — Human Soldier"` → Types: [CREATURE], Subtypes: [HUMAN, SOLDIER]
- `"Legendary Creature — Human Soldier"` → Supertypes: [LEGENDARY], Types: [CREATURE], Subtypes: [HUMAN, SOLDIER]
- `"Basic Land — Forest"` → Supertypes: [BASIC], Types: [LAND], Subtypes: [FOREST]
- `"Instant"` → Types: [INSTANT], no supertypes, no subtypes
- `"Legendary Artifact Creature — Golem"` → Supertypes: [LEGENDARY], Types: [ARTIFACT, CREATURE], Subtypes: [GOLEM]
- `"Legendary Planeswalker — Jace"` → Supertypes: [LEGENDARY], Types: [PLANESWALKER], Subtypes: [JACE]

The `oracle-words.properties` file already has entries for all `Supertype`, `Type`, `CreatureType`, `ArtifactType`, `EnchantmentType`, `BasicLandType`, `NonBasicLandType`, `PlaneswalkerType`, and `SpellType` enum values. Use `OracleParser.word(enumValue)` to generate parsers for each.

The `Subtype` sealed interface permits: `ArtifactType`, `CreatureType`, `EnchantmentType`, `LandType` (sealed, permits `BasicLandType`, `NonBasicLandType`), `PlaneswalkerType`, `SpellType`.

The parser result should be a record holding the parsed components:

```java
public record ParsedTypeLine(Supertypes supertypes, Types types, Subtypes subtypes) {}
```

**Step 1: Write the test**

```java
package be.imgn.mtg.engine.characteristics.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;

@DisplayName("TypeLineParser")
class TypeLineParserTest {

    @Nested
    @DisplayName("Simple type lines")
    class SimpleTypeLines {

        @Test
        @DisplayName("\"Instant\" → Types: [INSTANT]")
        void instant() {
            var result = TypeLineParser.parse("Instant");

            assertThat(result.types()).contains(Type.INSTANT);
            assertThat(result.supertypes().isEmpty()).isTrue();
            assertThat(result.subtypes().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("\"Sorcery\" → Types: [SORCERY]")
        void sorcery() {
            var result = TypeLineParser.parse("Sorcery");

            assertThat(result.types()).contains(Type.SORCERY);
        }
    }

    @Nested
    @DisplayName("Types with subtypes")
    class TypesWithSubtypes {

        @Test
        @DisplayName("\"Creature — Human Soldier\" → Types: [CREATURE], Subtypes: [HUMAN, SOLDIER]")
        void creatureSubtypes() {
            var result = TypeLineParser.parse("Creature — Human Soldier");

            assertThat(result.types()).contains(Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.HUMAN, CreatureType.SOLDIER);
        }

        @Test
        @DisplayName("\"Land — Forest\" → basic land type")
        void basicLand() {
            var result = TypeLineParser.parse("Land — Forest");

            assertThat(result.types()).contains(Type.LAND);
            assertThat(result.subtypes()).contains(BasicLandType.FOREST);
        }
    }

    @Nested
    @DisplayName("Supertypes")
    class SupertypeTests {

        @Test
        @DisplayName("\"Legendary Creature — Human Soldier\"")
        void legendary() {
            var result = TypeLineParser.parse("Legendary Creature — Human Soldier");

            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.HUMAN, CreatureType.SOLDIER);
        }

        @Test
        @DisplayName("\"Basic Land — Forest\"")
        void basicLand() {
            var result = TypeLineParser.parse("Basic Land — Forest");

            assertThat(result.supertypes()).contains(Supertype.BASIC);
            assertThat(result.types()).contains(Type.LAND);
            assertThat(result.subtypes()).contains(BasicLandType.FOREST);
        }
    }

    @Nested
    @DisplayName("Multiple types")
    class MultipleTypes {

        @Test
        @DisplayName("\"Artifact Creature — Golem\"")
        void artifactCreature() {
            var result = TypeLineParser.parse("Artifact Creature — Golem");

            assertThat(result.types()).contains(Type.ARTIFACT, Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.GOLEM);
        }

        @Test
        @DisplayName("\"Legendary Artifact Creature — Golem\"")
        void legendaryArtifactCreature() {
            var result = TypeLineParser.parse("Legendary Artifact Creature — Golem");

            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.ARTIFACT, Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.GOLEM);
        }
    }

    @Nested
    @DisplayName("Planeswalker")
    class PlaneswalkerTests {

        @Test
        @DisplayName("\"Legendary Planeswalker — Jace\"")
        void planeswalker() {
            var result = TypeLineParser.parse("Legendary Planeswalker — Jace");

            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.PLANESWALKER);
            assertThat(result.subtypes()).contains(PlaneswalkerType.JACE);
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -pl mtg-engine -Dtest="TypeLineParserTest" -DfailIfNoTests=false`
Expected: Compilation error — `TypeLineParser` does not exist.

**Step 3: Write the implementation**

The parser uses `OracleParser.word()` to build parsers for each enum value, then combines them. The challenge is that supertypes and types are both on the left side of the dash, so we parse supertypes first (greedy), then types.

Since the number of subtype enums is large (200+ creature types), build the subtype parser once by iterating all enum values.

```java
package be.imgn.mtg.engine.characteristics.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.OracleParser;
import be.imgn.mtg.engine.characteristics.ArtifactType;
import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.EnchantmentType;
import be.imgn.mtg.engine.characteristics.NonBasicLandType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.SpellType;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Parses MTG type lines into supertypes, types, and subtypes.
///
/// Type lines follow the pattern: `[Supertypes] Types [— Subtypes]`
///
/// Examples:
/// - `"Creature — Human Soldier"`
/// - `"Legendary Artifact Creature — Golem"`
/// - `"Basic Land — Forest"`
/// - `"Instant"`
public final class TypeLineParser {

    private TypeLineParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses a type line string into its components.
    ///
    /// @param typeLine the type line to parse
    /// @return the parsed type line components
    public static ParsedTypeLine parse(String typeLine) {
        // Split on " — " (em dash with spaces)
        var parts = typeLine.split(" — ", 2);
        var leftSide = parts[0].trim();
        var rightSide = parts.length > 1 ? parts[1].trim() : "";

        // Parse left side: supertypes then types
        var supertypes = new ArrayList<Supertype>();
        var types = new ArrayList<Type>();
        var words = leftSide.split(" ");

        for (var word : words) {
            var supertype = tryParseSupertype(word);
            if (supertype != null) {
                supertypes.add(supertype);
            } else {
                var type = tryParseType(word);
                if (type != null) {
                    types.add(type);
                }
            }
        }

        // Parse right side: subtypes
        var subtypes = new ArrayList<Subtype>();
        if (!rightSide.isEmpty()) {
            for (var word : rightSide.split(" ")) {
                var subtype = tryParseSubtype(word.trim());
                if (subtype != null) {
                    subtypes.add(subtype);
                }
            }
        }

        return new ParsedTypeLine(
                Supertypes.of(supertypes.toArray(new Supertype[0])),
                Types.of(types.toArray(new Type[0])),
                Subtypes.of(subtypes.toArray(new Subtype[0])));
    }

    private static Supertype tryParseSupertype(String word) {
        for (var value : Supertype.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        return null;
    }

    private static Type tryParseType(String word) {
        for (var value : Type.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        return null;
    }

    private static Subtype tryParseSubtype(String word) {
        // Try all subtype enums
        for (var value : CreatureType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : ArtifactType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : EnchantmentType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : BasicLandType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : NonBasicLandType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : PlaneswalkerType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        for (var value : SpellType.values()) {
            if (matchesOracleWord(value, word)) return value;
        }
        return null;
    }

    private static boolean matchesOracleWord(Enum<?> value, String word) {
        try {
            OracleParser.word(value).parseSkipping(WHITESPACE, word);
            return true;
        } catch (Exception _) {
            return false;
        }
    }
}
```

Note: This implementation splits on spaces and matches word-by-word rather than using a full parser combinator pipeline. This is simpler and works for type lines. If the user wants a pure combinator approach, the implementer should adapt this to use `Parser.anyOf()` with `OracleParser.word()` entries, but the word-by-word approach is more maintainable given the 200+ subtype values.

**Important:** Check if `Supertypes.of(Supertype...)`, `Types.of(Type...)`, and `Subtypes.of(Subtype...)` factory methods exist. If not, use the builder pattern or constructors available on those classes. The implementer must read those classes to find the correct construction method.

**Step 4: Create the ParsedTypeLine record**

```java
package be.imgn.mtg.engine.characteristics.internal;

import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;

/// Result of parsing a type line.
///
/// @param supertypes the parsed supertypes (may be empty)
/// @param types the parsed types
/// @param subtypes the parsed subtypes (may be empty)
public record ParsedTypeLine(Supertypes supertypes, Types types, Subtypes subtypes) {}
```

**Step 5: Run tests**

Run: `./mvnw test -pl mtg-engine -Dtest="TypeLineParserTest" -DfailIfNoTests=false`
Expected: All tests pass.

**Step 6: Run ide_diagnostics**

Check `TypeLineParser.java` and `ParsedTypeLine.java`.

**Step 7: Update module-info.java if needed**

The `characteristics.internal` package is NOT exported (it's internal). However, check if it needs to be opened to Guice. Currently it is not in the `opens` list — it should not need to be since TypeLineParser has no Guice bindings.

**Step 8: Commit**

```bash
git add mtg-engine/src/main/java/be/imgn/mtg/engine/characteristics/internal/TypeLineParser.java mtg-engine/src/main/java/be/imgn/mtg/engine/characteristics/internal/ParsedTypeLine.java mtg-engine/src/test/java/be/imgn/mtg/engine/characteristics/internal/TypeLineParserTest.java
git commit -m "Add TypeLineParser for parsing MTG type lines"
```

---

### Task 5: CardFetcher interface and CardModule

Create the `CardFetcher` interface in the `card` package, `DefaultCardFetcher` stub and `CardModule` in `card.internal`.

**Files:**
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/CardFetcher.java`
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/CardModule.java`
- Create: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcher.java` (stub)
- Modify: `mtg-engine/src/main/java/module-info.java` — add `opens be.imgn.mtg.engine.card.internal to com.google.guice;`

**Context:**

The `card` package already exists with `CardDefinition`, `CardDefinitionRepository`, `CardLayout`, `DeckDefinition`, `FaceDefinition`. The `card.internal` package does not exist yet.

`CardFetcher` fetches a card from the database by name and returns a fully constructed `Card` object (with parsed types, mana cost, abilities, etc.).

**Step 1: Create the CardFetcher interface**

```java
package be.imgn.mtg.engine.card;

import java.util.Optional;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Fetches cards from the card database by name.
///
/// Looks up card data, parses types, mana costs, and abilities,
/// and returns a fully constructed [Card] object.
public interface CardFetcher {

    /// Fetches a card by exact name.
    ///
    /// @param name the card name (case-insensitive)
    /// @param owner the player who owns the card
    /// @return the card, or empty if not found
    Optional<Card> fetchByName(String name, Player owner);
}
```

**Step 2: Create a stub DefaultCardFetcher**

```java
package be.imgn.mtg.engine.card.internal;

import java.util.Optional;

import be.imgn.mtg.engine.card.CardFetcher;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

/// Default implementation of [CardFetcher] backed by the H2 card database.
final class DefaultCardFetcher implements CardFetcher {

    @Override
    public Optional<Card> fetchByName(String name, Player owner) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**Step 3: Create CardModule**

```java
package be.imgn.mtg.engine.card.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.card.CardFetcher;

/// Guice module for card-related bindings.
public final class CardModule extends AbstractModule {

    @Provides
    @Singleton
    CardFetcher provideCardFetcher() {
        return new DefaultCardFetcher();
    }
}
```

**Step 4: Update module-info.java**

Add `opens be.imgn.mtg.engine.card.internal to com.google.guice;` in the opens section.

**Step 5: Run build to verify compilation**

Run: `./mvnw compile -pl mtg-engine`
Expected: Compilation succeeds.

**Step 6: Run ide_diagnostics**

Check all three new files.

**Step 7: Commit**

```bash
git add mtg-engine/src/main/java/be/imgn/mtg/engine/card/CardFetcher.java mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcher.java mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/CardModule.java mtg-engine/src/main/java/module-info.java
git commit -m "Add CardFetcher interface and CardModule"
```

---

### Task 6: DefaultCardFetcher implementation

Implement database-backed card fetching with full type line, mana cost, and ability parsing.

**Files:**
- Modify: `mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcher.java`
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcherTest.java`

**Context:**

The on-disk H2 database is at the path returned by `ToolsConfig.dataDir().resolve("cards")` (macOS: `~/Library/Application Support/mtg-engine/cards`). Connection uses `jdbc:h2:file:<path>;AUTO_SERVER=TRUE;ACCESS_MODE_DATA=r` with user `readonly` / password `readonly`.

The `card` table schema (key columns):
- `name` VARCHAR
- `mana_cost` VARCHAR (e.g., `"{1}{G}"`)
- `oracle_text` TEXT
- `type_line` VARCHAR (e.g., `"Creature — Bear"`)
- `power` VARCHAR
- `toughness` VARCHAR
- `loyalty` VARCHAR
- `defense` VARCHAR
- `colors` VARCHAR (e.g., `"G"`, `"WU"`)
- `color_identity` VARCHAR
- `color_indicator` VARCHAR
- `mana_value` FLOAT

Query for exact name match (case-insensitive):
```sql
SELECT * FROM card WHERE LOWER(name) = LOWER(:name) LIMIT 1
```

Card conversion pipeline:
1. Name → `Card.builder().name(name)`
2. Type line → `TypeLineParser.parse(typeLine)` → `.types()`, `.supertypes()`, `.subtypes()`
3. Mana cost → `ManaCost.parse(manaCost)` → `.manaCost()`
4. Power/toughness → `Value.of(Integer.parseInt(power))` → `.power()`, `.toughness()`
5. Oracle text → `AbilityParser.parse(oracleText)` → `.abilities()` and `.rulesText()`
6. Colors → parse single-char color codes → `.colors()`

The implementer must read `ManaCost.parse()`, `Color` enum, and `Card.Builder` to verify exact API.

**Step 1: Write the test**

Tests require the on-disk database to exist. Use JUnit `@EnabledIf` or `assumeTrue` to skip when database is not available.

```java
package be.imgn.mtg.engine.card.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;

@DisplayName("DefaultCardFetcher")
class DefaultCardFetcherTest {

    private static final Path DB_PATH = Path.of(
            System.getProperty("user.home"), "Library", "Application Support", "mtg-engine", "cards.mv.db");

    private final DefaultCardFetcher fetcher = new DefaultCardFetcher();
    private final Player owner = mock(Player.class);

    @BeforeAll
    static void requireDatabase() {
        assumeThat(Files.exists(DB_PATH))
                .as("Card database must exist at %s", DB_PATH)
                .isTrue();
    }

    @Nested
    @DisplayName("fetchByName")
    class FetchByName {

        @Test
        @DisplayName("fetches Grizzly Bears with correct types")
        void grizzlyBears() {
            var card = fetcher.fetchByName("Grizzly Bears", owner);

            assertThat(card).isPresent();
            var c = card.get();
            assertThat(c.name()).isEqualTo("Grizzly Bears");
            assertThat(c.types().isCreature()).isTrue();
            assertThat(c.subtypes()).contains(CreatureType.BEAR);
        }

        @Test
        @DisplayName("fetches Lightning Bolt as instant")
        void lightningBolt() {
            var card = fetcher.fetchByName("Lightning Bolt", owner);

            assertThat(card).isPresent();
            assertThat(card.get().types().isInstant()).isTrue();
            assertThat(card.get().manaCost()).isNotNull();
        }

        @Test
        @DisplayName("returns empty for nonexistent card")
        void notFound() {
            var card = fetcher.fetchByName("Nonexistent Card XYZ", owner);

            assertThat(card).isEmpty();
        }

        @Test
        @DisplayName("case-insensitive name match")
        void caseInsensitive() {
            var card = fetcher.fetchByName("grizzly bears", owner);

            assertThat(card).isPresent();
        }
    }
}
```

**Step 2: Implement DefaultCardFetcher**

The implementer must:
1. Read `ToolsConfig.java` to find the database path logic (or hardcode macOS path + make configurable later)
2. Read `ManaCost.parse()` signature
3. Read `Color` enum to understand color code mapping
4. Read `Card.Builder` to understand exact API
5. Connect via JDBI, query, convert result

Key implementation notes:
- Use JDBI `Jdbi.create(url, "readonly", "readonly")` for connection
- Parse power/toughness: handle `"*"` (variable P/T) by skipping — only parse numeric values
- Colors: `"W"` → `Color.WHITE`, `"U"` → `Color.BLUE`, etc.
- If oracle text is null or empty, skip ability parsing

**Step 3: Run tests**

Run: `./mvnw test -pl mtg-engine -Dtest="DefaultCardFetcherTest" -DfailIfNoTests=false`
Expected: All tests pass (or skipped if no database).

**Step 4: Run ide_diagnostics**

**Step 5: Commit**

```bash
git add mtg-engine/src/main/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcher.java mtg-engine/src/test/java/be/imgn/mtg/engine/card/internal/DefaultCardFetcherTest.java
git commit -m "Implement DefaultCardFetcher with database-backed card creation"
```

---

### Task 7: TestGame

Create the TestGame test utility class.

**Files:**
- Create: `mtg-engine/src/test/java/be/imgn/mtg/engine/game/TestGame.java`
- Test: `mtg-engine/src/test/java/be/imgn/mtg/engine/game/TestGameTest.java`

**Context:**

TestGame wraps a real Guice-backed game. It uses:
- `GameModule` → `GameFactory` → `Game` for bootstrapping
- `GameState` for zone access
- `TurnTracker` for phase/step advancement and priority
- `CardFetcher` for database card lookup
- `CardModule` for Guice bindings

The game is created with 2 players and a minimal format. The `Format` interface has one method: `checkPlayers(List<PlayerData>)`. Create a simple anonymous implementation that accepts 2 players.

`PlayerId` is an interface — create a simple `record TestPlayerId(String name) implements PlayerId {}` inside TestGame.

`Team` is an interface — create a simple `record TestTeam(String name) implements Team {}` inside TestGame.

The Guice injector must install both `GameModule` and `CardModule`. TestGame gets `GameFactory`, `CardFetcher`, and the created game's `GameState` and `TurnTracker`.

**Key question for implementer:** How to get `GameState` and `TurnTracker` from the game injector? The `GameFactory` creates a child injector internally and returns `Game`. The implementer needs to check if there's a way to access the child injector's bindings (GameState, TurnTracker) from the Game instance. If not, TestGame may need to create its own child injector using `GameConfigurationModule` directly.

Read these files before implementing:
- `be.imgn.mtg.engine.game.internal.GameModule` — how GameFactory is provided
- `be.imgn.mtg.engine.game.internal.GameConfigurationModule` — what modules are installed
- `be.imgn.mtg.engine.state.GameState` — what it provides
- `be.imgn.mtg.engine.turn.TurnTracker` — API for advancement

**Step 1: Write the test**

```java
package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.turn.Phase;

@DisplayName("TestGame")
class TestGameTest {

    private static final Path DB_PATH = Path.of(
            System.getProperty("user.home"), "Library", "Application Support", "mtg-engine", "cards.mv.db");

    private TestGame game;

    @BeforeAll
    static void requireDatabase() {
        assumeThat(Files.exists(DB_PATH))
                .as("Card database must exist at %s", DB_PATH)
                .isTrue();
    }

    @BeforeEach
    void setUp() {
        game = TestGame.create();
    }

    @Nested
    @DisplayName("Game setup")
    class GameSetup {

        @Test
        @DisplayName("creates a game with two players")
        void twoPlayers() {
            assertThat(game.player1()).isNotNull();
            assertThat(game.player2()).isNotNull();
            assertThat(game.player1()).isNotEqualTo(game.player2());
        }

        @Test
        @DisplayName("provides access to game state")
        void gameState() {
            assertThat(game.gameState()).isNotNull();
        }

        @Test
        @DisplayName("provides access to battlefield")
        void battlefield() {
            assertThat(game.battlefield()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createPermanent")
    class CreatePermanentTests {

        @Test
        @DisplayName("creates a permanent from a real card name")
        void createsPermanent() {
            var bear = game.createPermanent("Grizzly Bears");

            assertThat(bear).isNotNull();
            assertThat(bear.name()).isEqualTo("Grizzly Bears");
            assertThat(bear.types().isCreature()).isTrue();
        }

        @Test
        @DisplayName("permanent is on the battlefield")
        void onBattlefield() {
            var bear = game.createPermanent("Grizzly Bears");

            assertThat(game.battlefield().contains(bear)).isTrue();
        }
    }

    @Nested
    @DisplayName("Game advancement")
    class Advancement {

        @Test
        @DisplayName("advances to main phase")
        void advanceToMain() {
            game.advanceTo(Phase.MAIN);

            assertThat(game.gameState().turnTracker().currentPhase()).isEqualTo(Phase.MAIN);
        }
    }
}
```

**Step 2: Implement TestGame**

The implementer must:
1. Read GameConfigurationModule to understand how to create the child injector
2. Create the injector with GameModule + CardModule
3. Create a game via GameFactory
4. Extract GameState, TurnTracker, CardFetcher from the injector
5. Start the game via turnTracker.startGame(player1)
6. Implement convenience methods

Key methods:
- `create()` — static factory, creates game with 2 players
- `player1()`, `player2()` — accessors
- `gameState()`, `battlefield()`, `stack()` — zone access
- `createPermanent(String cardName)` — fetches card, enters battlefield under player1
- `createPermanent(String cardName, Player controller)` — fetches card, enters under controller
- `addToHand(String cardName)`, `addToHand(String cardName, Player player)` — put in hand zone
- `addToGraveyard(String cardName)`, `addToGraveyard(String cardName, Player player)` — put in graveyard
- `advanceTo(Phase)` — pass priority until reaching target phase
- `passPriority()` — pass once for active player

**Step 3: Run tests**

Run: `./mvnw test -pl mtg-engine -Dtest="TestGameTest" -DfailIfNoTests=false`
Expected: All tests pass (or skipped if no database).

**Step 4: Run ide_diagnostics**

**Step 5: Run full test suite**

Run: `./mvnw test -pl mtg-engine`
Expected: All existing tests still pass.

**Step 6: Commit**

```bash
git add mtg-engine/src/test/java/be/imgn/mtg/engine/game/TestGame.java mtg-engine/src/test/java/be/imgn/mtg/engine/game/TestGameTest.java
git commit -m "Add TestGame test utility for card interaction scenarios"
```
