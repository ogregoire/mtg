package be.imgn.mtg.engine.ability.internal.parser.selector;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.ControllerClause;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;

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
        Optional<ControllerClause> controller) {

    /// Returns whether the given game object matches all criteria of this selector.
    ///
    /// Checks the type matcher, qualifiers (negations, status, supertypes),
    /// and with-clauses (mana value, power, toughness).
    public boolean matches(GameObject object) {
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
                comparison.test(object.manaValue().value(), value);
            case WithClause.Power(var comparison, var value) ->
                object.power() != null && comparison.test(object.power().value(), value);
            case WithClause.Toughness(var comparison, var value) ->
                object.toughness() != null && comparison.test(object.toughness().value(), value);
            case WithClause.Ability ignored ->
                throw new UnsupportedOperationException("Ability with-clause not yet supported");
            case WithClause.Counter ignored ->
                throw new UnsupportedOperationException("Counter with-clause not yet supported");
        };
    }
}
