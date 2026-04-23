# MTG Project

Multi-module Maven project for MTG.

## Modules

- **mtg-parent**: Parent POM with shared configuration
- **mtg-bom**: Bill of Materials for dependency management
- **mtg-taglet**: Custom Javadoc taglet for `{@mtg.rule}` references
- **mtg-parser**: Parser combinator library for MTG oracle text
- **mtg-engine**: Core game engine (`be.imgn.mtg.engine`)
- **mtg-tools**: Tools and utilities (`be.imgn.mtg.tooling`)
- **mtg-rules-maven-plugin**: Maven plugin to generate HTML from MTG rules
- **mtg-docs**: Aggregated Javadoc and rules documentation

## Working Guidelines

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### Think Before Coding

Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### Simplicity First

Minimum code that solves the problem. Nothing speculative.

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### Surgical Changes

Touch only what you must. Clean up only your own mess.

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### Goal-Driven Execution

Define success criteria. Loop until verified.

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multistep tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

## Build Commands

```bash
# Full build with tests
./mvnw clean verify

# Fast build (skip tests)
./mvnw clean verify -DskipTests

# Check for dependency updates (stable versions only)
./mvnw versions:display-dependency-updates

# Check for plugin updates
./mvnw versions:display-plugin-updates
```

## Code Style

- Use `var` for local variables where appropriate
- Use markdown-style comments (`/// ...` for Javadoc)
- No fully qualified names in code (use imports)
- Never use `Optional` as a method/constructor parameter — use overloads or `@Nullable`
- `equals` pattern (for field-based equality):
  ```java
  @Override public boolean equals(@Nullable Object o) {
      return this == o || o instanceof MyClass other
              && primitiveField == other.primitiveField
              && nonNullableField.equals(other.nonNullableField)
              && Objects.equals(nullableField, other.nullableField);
  }
  ```

## Test Coverage

Each package should maintain:

- **80% line coverage**
- **80% branch coverage**

Run tests with JaCoCo report:
```bash
./mvnw test -pl mtg-engine
```

View coverage report at `mtg-engine/target/site/jacoco/index.html`.

## Documentation

The `wiki/` directory is a separate git repository (GitHub wiki). Use `git -C wiki` for wiki changes — never use `git -C` for the base repository.

### Architecture Documents

Architecture design documents are stored in `wiki/architecture/`. These describe the high-level design for each system:

- `general.md` - Overall architecture overview
- `turn-tracker.md` - Turn structure, phases, steps, priority (Rules 500-514, 117)
- `event-system.md` - EventBus, triggers, replacements
- `zone-system.md` - Zones and zone changes
- `stack-system.md` - The stack and spell/ability resolution
- `state-based-actions.md` - SBAs (Rule 704)
- `layer-system.md` - Continuous effects and layers (Rule 613)
- `combat-system.md` - Combat phase details (Rules 506-511)
- `ability-system.md` - Abilities and their types
- `action-system.md` - Player actions
- `action-processing.md` - Action resolution
- `object-system.md` - Game objects
- `card-parsing.md` - Oracle text parsing
- `cost-system.md` - Costs and payments
- `trigger-system.md` - Trigger conditions taxonomy (Rule 603)

### MTG Comprehensive Rules

The official MTG Comprehensive Rules are stored in `rules/` with dated versions:

- `rules/2026-04-17/full/rules.txt` - Latest rules text file

Use the `mtg-rules` agent to research specific rules or verify implementations.

## Git Workflow

**IMPORTANT**: This project uses **Git Flow**. Always use `git flow` commands for branch management.

When planning a feature, the plan must always include these steps:
1. Create the feature branch with `git flow feature start`
2. Implement the feature
3. Commit the changes
4. Finish the feature with `git flow feature finish`

```bash
# Start a new feature
git flow feature start my-feature

# Finish a feature (merges to develop)
git flow feature finish my-feature

# Start a hotfix
git flow hotfix start fix-description

# Finish a hotfix (merges to main and develop, creates tag)
git flow hotfix finish fix-description

# Start a release
git flow release start 1.0.0

# Finish a release (merges to main and develop, creates tag)
git flow release finish 1.0.0
```

### Commit Messages

- Use imperative mood: "Add feature" not "Added feature"
- Keep first line under 72 characters
- Reference issue numbers when applicable: "Fix card search (#123)"

## Package Structure

This project uses Guice for dependency injection with a clear separation between API and implementation:

- **Public API** (`be.imgn.mtg.engine.turn`): Interfaces, records, enums, and events go in the main package.
- **Internal implementation** (`be.imgn.mtg.engine.turn.internal`): Implementation classes go in the `internal` subpackage, package-private where possible.
- **Guice modules**: Each `internal` package has a `*Module.java` that binds implementations to interfaces, mostly through `@Provides` methods.

Example:
```
be.imgn.mtg.engine.turn/
  ├── Phase.java              # interface
  ├── PhaseType.java          # enum
  ├── PhaseStartedEvent.java  # record
  └── internal/
      ├── DefaultPhase.java   # implementation (package-private)
      └── TurnModule.java     # Guice bindings
```

## MCP Tools

- **IntelliJ Index**: Use `mcp__intellij-index__*` tools for code navigation, finding references/implementations, and safe refactoring.
  - **Renaming classes** must go through `ide_refactor_rename`.
  - **Moving classes** (changing packages) must go through the INDEX MCP tools — never use sed/manual find-replace for import updates.
- **IntelliJ Debugger**: Use `mcp__intellij-debugger__*` tools for setting breakpoints, stepping through code, and inspecting variables.

### Post-Edit Validation

**IMPORTANT**: After creating or editing a Java file, always run `ide_diagnostics` on the file to check for warnings and errors. Fix any issues before considering the task complete.

## Agents

Use specialized agents for MTG-specific and development tasks:

### MTG Domain Agents

- **mtg-rules**: Research MTG Comprehensive Rules, verify rule implementations, look up specific rule numbers, understand card interactions
- **layer-system**: Implement continuous effects, type-changing effects, P/T modifications, ability interactions (Rules 613)
- **event-designer**: Design events, create event hierarchies, implement game flow through EventBus
- **scryfall-lookup**: Look up card data, oracle text, rulings, card images
- **parser**: Write or modify oracle text parsers, work with the dot-parse library, debug parse failures, add new effect/ability parsers

### Development Agents

- **build-validator**: Verify project compiles and tests pass before committing
- **test-writer**: Generate unit tests, improve coverage, create test cases
- **test-runner**: Run tests after code changes, analyze failures
- **code-reviewer**: Review code for quality, patterns, bugs, project conventions
- **refactorer**: Identify code smells, suggest improvements, execute safe refactorings
- **debugger**: Analyze stack traces, find root causes, interactive debugging via IntelliJ
- **architect**: Plan complex features, design components, make architectural decisions
- **git-assistant**: Complex git operations, branching, rebasing (Gitflow workflow)
- **sql-expert**: Construct and execute H2 SQL queries via `./mtg sql`, analyze database schema, extract data patterns from card/rules database
- **Explore**: Quickly find files, search code, answer codebase questions
