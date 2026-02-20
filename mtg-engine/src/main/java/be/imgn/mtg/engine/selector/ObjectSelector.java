package be.imgn.mtg.engine.selector;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.TypedObject;

/// Selects game objects based on various criteria.
///
/// @param quantifier how many objects to select
/// @param qualifiers modifiers like "target", "nonland", status conditions
/// @param typeMatcher the type(s) to match
/// @param withClauses additional criteria like "with mana value 3 or less"
/// @param controller optional controller restriction
public record ObjectSelector(
        Quantifier quantifier,
        List<Qualifier> qualifiers,
        TypeMatcher typeMatcher,
        List<WithClause> withClauses,
        @Nullable ControllerClause controller)
        implements Selector {

    @Override
    public boolean matches(Selectable selectable, Player perspective) {
        return selectable instanceof GameObject obj && matches(obj, perspective);
    }

    /// Returns whether the given game object matches all criteria of this selector.
    ///
    /// Checks the type matcher, qualifiers (negations, status, supertypes),
    /// with-clauses (mana value, power, toughness), and controller restrictions.
    ///
    /// @param object the game object to test
    /// @param perspective the player from whose perspective the match is evaluated
    /// @return true if the object matches
    public boolean matches(GameObject object, Player perspective) {
        if (!typeMatcher.matches(object)) {
            return false;
        }
        for (var qualifier : qualifiers) {
            if (!matchesQualifier(qualifier, object)) {
                return false;
            }
        }
        for (var clause : withClauses) {
            if (!matchesWithClause(clause, object)) {
                return false;
            }
        }
        if (controller != null && !matchesController(controller, object, perspective)) {
            return false;
        }
        return true;
    }

    private static boolean matchesQualifier(Qualifier qualifier, GameObject object) {
        return switch (qualifier) {
            case Qualifier.Target ignored -> true;
            case Qualifier.Has(var trait) -> trait.test(object);
            case Qualifier.Not(var trait) -> !trait.test(object);
            case Qualifier.Status(var status) -> matchesStatus(status, object);
        };
    }

    private static boolean matchesStatus(StatusType status, GameObject object) {
        if (!(object instanceof Permanent perm)) {
            return false;
        }
        return switch (status) {
            case TAPPED -> perm.isTapped();
            case UNTAPPED -> perm.isUntapped();
            case ATTACKING, BLOCKING, EQUIPPED, ENCHANTED ->
                throw new UnsupportedOperationException("Status " + status + " not yet supported");
        };
    }

    private static boolean matchesWithClause(WithClause clause, GameObject object) {
        return switch (clause) {
            case WithClause.ManaValue(var comparison, var value) ->
                object instanceof TypedObject t && comparison.test(t.manaValue().value(), value);
            case WithClause.Power(var comparison, var value) ->
                object instanceof TypedObject t
                        && t.power() != null
                        && comparison.test(t.power().value(), value);
            case WithClause.Toughness(var comparison, var value) ->
                object instanceof TypedObject t
                        && t.toughness() != null
                        && comparison.test(t.toughness().value(), value);
            case WithClause.Ability ignored ->
                throw new UnsupportedOperationException("Ability with-clause not yet supported");
            case WithClause.Counter ignored ->
                throw new UnsupportedOperationException("Counter with-clause not yet supported");
        };
    }

    private static boolean matchesController(ControllerClause clause, GameObject object, Player perspective) {
        return switch (clause.player()) {
            case YOU -> object.controller().equals(perspective);
            case OPPONENT, EACH_OPPONENT -> !object.controller().equals(perspective);
            default -> true;
        };
    }
}
