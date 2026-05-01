package be.imgn.mtg.engine.mana;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.effect.Effect;

/// Sealed interface for all mana-adding effects.
///
/// This represents the different ways mana can be added to a player's pool:
/// - Exact mana symbols like "Add {G}{G}."
/// - Selection from options like "Add {U} or {B}."
/// - Any color combination like "Add two mana of any color."
public sealed interface AddManaEffect extends Effect
        permits AddManaEffect.Exact, AddManaEffect.Variable, AddManaEffect.Selection, AddManaEffect.Combination {

    @Override
    default boolean addsMana() {
        return true;
    }

    /// An effect that adds exact mana to a player's mana pool.
    ///
    /// Examples: "Add {G}.", "Add {G}{G}.", "Add {W}{U}{B}{R}{G}."
    ///
    /// @param mana the mana types to add
    record Exact(List<ManaType> mana) implements AddManaEffect {
        /// Creates a new Exact effect.
        public Exact {
            requireNonNull(mana, "mana");
            if (mana.isEmpty()) {
                throw new IllegalArgumentException("mana cannot be empty");
            }
            mana = List.copyOf(mana);
        }
    }

    /// An effect that adds X mana of a single type to a player's mana pool.
    ///
    /// Example: "Add X {G}." → mana=GREEN
    ///
    /// @param mana the mana type to add
    record Variable(ManaType mana) implements AddManaEffect {
        /// Creates a new Variable effect.
        public Variable {
            requireNonNull(mana, "mana");
        }
    }

    /// Effect that adds mana from a selection of options to a player's pool.
    ///
    /// The player chooses one option from the available selections, and all mana
    /// in that option is added to their pool.
    ///
    /// Examples:
    /// - "Add {U} or {B}." → options=`[[BLUE], [BLACK]]`, amount=null
    /// - "Add {R}{R}, {R}{G}, or {G}{G}." → options=`[[RED, RED], [RED, GREEN], [GREEN, GREEN]]`, amount=null
    /// - "Add two mana of any one color." → options=`[[WHITE], [BLUE], ...]`, amount=2
    /// - "Add X mana of any one color." → options=`[[WHITE], [BLUE], ...]`, amount=X
    ///
    /// @param options the available mana options to choose from
    /// @param amount for "any one color", the amount to add of the chosen color; null for explicit options
    record Selection(List<List<ManaType>> options, @Nullable Amount amount) implements AddManaEffect {

        /// The five color options for "any one color" effects.
        private static final List<List<ManaType>> ANY_ONE_COLOR_OPTIONS = List.of(
                List.of(ManaType.WHITE),
                List.of(ManaType.BLUE),
                List.of(ManaType.BLACK),
                List.of(ManaType.RED),
                List.of(ManaType.GREEN));

        /// Creates a new effect to add mana from a selection.
        public Selection {
            requireNonNull(options, "options");
            if (options.isEmpty()) {
                throw new IllegalArgumentException("Options cannot be empty");
            }
            for (var option : options) {
                if (option.isEmpty()) {
                    throw new IllegalArgumentException("Each option must contain at least one mana type");
                }
            }
            // Defensive copy
            options = options.stream().map(List::copyOf).toList();
        }

        /// Creates an effect for explicit mana options.
        ///
        /// @param options the available mana options
        public Selection(List<List<ManaType>> options) {
            this(options, null);
        }

        /// Creates a selection for "add N mana of any one color".
        ///
        /// @param amount the amount of mana to add
        /// @return a selection with one option per color
        public static Selection anyOneColor(Amount amount) {
            requireNonNull(amount, "amount");
            return new Selection(ANY_ONE_COLOR_OPTIONS, amount);
        }
    }

    /// Effect that adds mana of allowed types to a player's pool.
    ///
    /// Each mana can be a different type from the allowed set, chosen independently at resolution.
    ///
    /// Examples:
    /// - "Add two mana in any combination of colors." → all 5 colors allowed
    /// - "Add three mana in any combination of {R} and/or {G}." → only RED and GREEN allowed
    /// - "Add X mana in any combination of {W} and/or {U}." → variable amount
    ///
    /// @param amount the amount of mana to add
    /// @param allowedTypes the set of mana types that can be chosen
    record Combination(Amount amount, Set<ManaType.Colored> allowedTypes) implements AddManaEffect {

        /// All five colors for "mana of any color" effects.
        private static final Set<ManaType.Colored> ALL_COLORS =
                Collections.unmodifiableSet(EnumSet.allOf(ManaType.Colored.class));

        /// Creates a new effect to add mana of any combination.
        public Combination {
            requireNonNull(amount, "amount");
            requireNonNull(allowedTypes, "allowedTypes");
            if (allowedTypes.isEmpty()) {
                throw new IllegalArgumentException("allowedTypes cannot be empty");
            }
            // Defensive properly ordered copy
            allowedTypes = Collections.unmodifiableSet(EnumSet.copyOf(allowedTypes));
        }

        /// Creates an effect for "add N mana of any color" or "add N mana in any combination of colors".
        ///
        /// @param amount the amount of mana to add
        /// @return an effect allowing any of the 5 colors
        public static Combination anyColor(Amount amount) {
            return new Combination(amount, ALL_COLORS);
        }
    }
}
