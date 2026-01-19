package be.imgn.mtg.engine.ability.internal.parser.selector;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.ControllerClause;

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
        Optional<ControllerClause> controller) {}
