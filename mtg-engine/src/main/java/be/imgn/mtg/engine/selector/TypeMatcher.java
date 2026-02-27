package be.imgn.mtg.engine.selector;

import java.util.List;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.selector.internal.TypeMatching;

/// Matches objects in oracle text by game object type and/or card type.
///
/// There are two orthogonal dimensions:
/// - **Game object type**: permanent, card, spell, ability, token ({@mtg.rule 109})
/// - **Card type**: creature, artifact, enchantment, etc. ({@mtg.rule 205})
///
/// When only a card type is specified (e.g., "target creature"), the game object type is
/// implicitly permanent. Use `instanceof` to determine the game object type:
/// - `instanceof Permanent` — explicit permanent
/// - `instanceof Spell` — spell on the stack
/// - `instanceof Card` — card in any zone
/// - `instanceof AbilityOnStack` — ability on the stack
/// - Otherwise (Single, Or, Target) — implicitly permanent
public sealed interface TypeMatcher {

    /// Returns whether the given game object matches this type matcher.
    ///
    /// Checks both the game object type (permanent, spell, card, ability) and the card type
    /// (creature, artifact, etc.) as appropriate for this matcher.
    default boolean matches(GameObject object) {
        return TypeMatching.matches(this, object);
    }

    /// Matches a single specific card type (e.g., "creature", "artifact").
    /// Game object type is implicitly permanent.
    record Single(Type type) implements TypeMatcher {}

    /// Matches any of the given types (e.g., "artifact or enchantment").
    /// Game object type is implicitly permanent.
    record Or(List<TypeMatcher> matchers) implements TypeMatcher {}

    /// Matches any permanent (explicit game object type, no card type restriction).
    record Permanent() implements TypeMatcher {}

    /// Matches any spell on the stack (game object type).
    record Spell() implements TypeMatcher {}

    /// Matches any game object regardless of type. Used as a generic targetable object matcher.
    /// Specific target restrictions (e.g., Rule 115.4) are applied at the parser level.
    record Target() implements TypeMatcher {}

    /// Matches any card in any zone (game object type, no card type restriction).
    /// Used in "discard a card", "mill 3 cards", "exile a card from your graveyard", etc.
    record Card() implements TypeMatcher {}

    /// Matches a token permanent on the battlefield.
    /// A token is a permanent whose source is a [be.imgn.mtg.engine.object.Token].
    record Token() implements TypeMatcher {}

    /// Matches a card with specific card type(s) (e.g., "creature card", "instant or sorcery card").
    record CardWithType(List<Type> types) implements TypeMatcher {}

    /// Matches a card with any permanent card type (e.g., "permanent card").
    record CardWithPermanentType() implements TypeMatcher {}

    /// Matches a card with any spell card type (e.g., "spell card").
    record CardWithSpellType() implements TypeMatcher {}

    /// Matches a spell with specific card type(s) (e.g., "creature spell").
    record SpellWithType(List<Type> types) implements TypeMatcher {}

    /// Matches a spell with any permanent card type (e.g., "permanent spell").
    record SpellWithPermanentType() implements TypeMatcher {}

    /// Matches any ability on the stack (activated or triggered).
    record Ability() implements TypeMatcher {}

    /// Matches an activated ability on the stack.
    record ActivatedAbility() implements TypeMatcher {}

    /// Matches a triggered ability on the stack.
    record TriggeredAbility() implements TypeMatcher {}
}
