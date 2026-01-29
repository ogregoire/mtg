package be.imgn.mtg.engine.ability.internal.parser.selector;

import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.object.TypedObject;

/// Matching logic for [TypeMatcher] against game objects.
///
/// Separated from [TypeMatcher] to avoid naming conflicts between the TypeMatcher records
/// (Permanent, Spell, Card) and the game object interfaces.
final class TypeMatching {

    private TypeMatching() {}

    static boolean matches(TypeMatcher matcher, TypedObject object) {
        return switch (matcher) {
            case TypeMatcher.Permanent _ -> object instanceof Permanent;
            case TypeMatcher.Spell _ -> object instanceof Spell;
            case TypeMatcher.Card _ -> object instanceof Card;
            case TypeMatcher.Token _ -> object instanceof Permanent perm && perm.source() instanceof Token;
            case TypeMatcher.Target _ ->
                object instanceof Permanent perm
                        && (perm.types().isCreature() || perm.types().isPlaneswalker());
            case TypeMatcher.Single(var type) ->
                object instanceof Permanent perm && perm.types().contains(type);
            case TypeMatcher.Or(var matchers) ->
                object instanceof Permanent && matchers.stream().anyMatch(m -> matches(m, object));
            case TypeMatcher.CardWithType(var types) ->
                object instanceof Card card
                        && types.stream().anyMatch(t -> card.types().contains(t));
            case TypeMatcher.CardWithPermanentType _ ->
                object instanceof Card card && card.types().isPermanentType();
            case TypeMatcher.CardWithSpellType _ ->
                object instanceof Card card && card.types().isSpellType();
            case TypeMatcher.SpellWithType(var types) ->
                object instanceof Spell spell
                        && types.stream().anyMatch(t -> spell.types().contains(t));
            case TypeMatcher.SpellWithPermanentType _ ->
                object instanceof Spell spell && spell.types().isPermanentType();
        };
    }
}
