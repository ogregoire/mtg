package be.imgn.mtg.engine.oracle;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/// Composable object selector: \[quantifier\] \[qualifier\]* type \[with\]* \[that\]* \[controller\] \[zone\].
public record Selector(
        Quantifier quantifier,
        List<Qualifier> qualifiers,
        TypeExpression type,
        List<WithClause> withClauses,
        List<ThatClause> thatClauses,
        @Nullable ControllerClause controller,
        Zone.@Nullable Named zone) {

    public Selector(
            Quantifier quantifier,
            List<Qualifier> qualifiers,
            TypeExpression type,
            List<WithClause> withClauses,
            @Nullable ControllerClause controller) {
        this(quantifier, qualifiers, type, withClauses, List.of(), controller, null);
    }

    Selector(Quantifier quantifier, TypeExpression type) {
        this(quantifier, List.of(), type, List.of(), List.of(), null, null);
    }

    Selector(Quantifier quantifier, List<Qualifier> qualifiers, TypeExpression type) {
        this(quantifier, qualifiers, type, List.of(), List.of(), null, null);
    }

    Selector(TypeExpression type) {
        this(Quantifier.one(), List.of(), type, List.of(), List.of(), null, null);
    }

    Selector(List<Qualifier> qualifiers, TypeExpression type) {
        this(Quantifier.one(), qualifiers, type, List.of(), List.of(), null, null);
    }

    Selector withWithClause(WithClause wc) {
        return new Selector(quantifier, qualifiers, type, List.of(wc), thatClauses, controller, zone);
    }

    /// Appends a [WithClause] to the selector, preserving any existing
    /// with-clauses. Used by trailing exception parsers (e.g., "except for
    /// commanders") that stack on top of a primary "with" clause.
    Selector addWithClause(WithClause wc) {
        var combined = new ArrayList<>(withClauses);
        combined.add(wc);
        return new Selector(quantifier, qualifiers, type, List.copyOf(combined), thatClauses, controller, zone);
    }

    Selector withThatClause(ThatClause tc) {
        return new Selector(quantifier, qualifiers, type, withClauses, List.of(tc), controller, zone);
    }

    Selector withController(ControllerClause cc) {
        return new Selector(quantifier, qualifiers, type, withClauses, thatClauses, cc, zone);
    }

    Selector withZone(Zone.Named z) {
        return new Selector(quantifier, qualifiers, type, withClauses, thatClauses, controller, z);
    }

    public sealed interface Quantifier {
        enum One implements Quantifier {
            ONE
        }

        enum All implements Quantifier {
            ALL
        }

        enum Each implements Quantifier {
            EACH
        }

        enum Every implements Quantifier {
            EVERY
        }

        enum Another implements Quantifier {
            ANOTHER
        }

        enum Other implements Quantifier {
            OTHER
        }

        enum AnyNumber implements Quantifier {
            ANY_NUMBER
        }

        enum The implements Quantifier {
            THE
        }

        enum Variable implements Quantifier {
            VARIABLE
        }

        record Count(int n) implements Quantifier {}

        record UpTo(int n) implements Quantifier {}

        /// "N or M" — an inclusive range with both endpoints allowed
        /// (e.g., Broken Dam: "Tap one or two target creatures …").
        record Range(int min, int max) implements Quantifier {}

        /// Returns the [One] singleton.
        static Quantifier one() {
            return One.ONE;
        }

        /// Returns the [All] singleton.
        static Quantifier all() {
            return All.ALL;
        }

        /// Returns the [Each] singleton.
        static Quantifier each() {
            return Each.EACH;
        }

        /// Returns the [Every] singleton.
        static Quantifier every() {
            return Every.EVERY;
        }

        /// Returns the [Another] singleton.
        static Quantifier another() {
            return Another.ANOTHER;
        }

        /// Returns the [Other] singleton.
        static Quantifier other() {
            return Other.OTHER;
        }

        /// Creates a [Count] quantifier.
        static Quantifier count(int n) {
            return new Count(n);
        }

        /// Creates an [UpTo] quantifier.
        static Quantifier upTo(int n) {
            return new UpTo(n);
        }

        /// Creates a [Range] quantifier.
        static Quantifier range(int min, int max) {
            return new Range(min, max);
        }

        /// Returns the [AnyNumber] singleton.
        static Quantifier anyNumber() {
            return AnyNumber.ANY_NUMBER;
        }

        /// Returns the [The] singleton.
        static Quantifier the() {
            return The.THE;
        }

        /// Returns the [Variable] singleton.
        static Quantifier variable() {
            return Variable.VARIABLE;
        }
    }

    public sealed interface Qualifier {
        enum Target implements Qualifier {
            TARGET
        }

        record Color(ColorFilter filter) implements Qualifier {}

        /// Multi-color qualifier for "red or green", "black or red", etc.
        record Colors(List<ColorFilter> filters) implements Qualifier {}

        record OfSupertype(Supertype supertype) implements Qualifier {}

        record NegatedSupertype(Supertype supertype) implements Qualifier {}

        record NegatedCardType(CardType type) implements Qualifier {}

        record NegatedSubtype(Subtype subtype) implements Qualifier {}

        /// Status qualifier values — conditions a permanent can have,
        /// or a resolution-history/role tag attached to a card. Used on
        /// selector clauses like "target tapped creature", "a suspended
        /// card", "each noncommander creature", etc.
        enum Status implements Qualifier {
            TAPPED,
            UNTAPPED,
            FACE_DOWN,
            FACE_UP,
            /// Currently in the exile zone.
            EXILED,
            /// Was milled during the current resolution (Heed the Mists).
            MILLED,
            /// Was drawn during the current resolution.
            DRAWN,
            /// Was discarded during the current resolution.
            DISCARDED,
            /// Was revealed during the current resolution.
            REVEALED,
            /// Currently under a suspend counter (Venser's Diffusion).
            SUSPENDED,
            /// The player's commander (Commander format role).
            COMMANDER,
            /// A non-commander permanent.
            NONCOMMANDER,
            /// Positional tags — the last/first/top card in a sequence
            /// (Jandor's Ring: "the last card you drew this turn").
            LAST,
            FIRST,
            TOP
        }

        record CombatStatus(String status) implements Qualifier {}

        enum Historic implements Qualifier {
            HISTORIC
        }

        enum Outlaw implements Qualifier {
            OUTLAW
        }

        enum NegatedOutlaw implements Qualifier {
            NEGATED_OUTLAW
        }

        enum IsToken implements Qualifier {
            IS_TOKEN
        }

        enum NonToken implements Qualifier {
            NON_TOKEN
        }

        enum OtherQ implements Qualifier {
            OTHER
        }

        /// "Enchanted \[permanent\]" — the object currently enchanted by this
        /// Aura. Common on Aura spells: "Destroy target enchanted permanent."
        enum Enchanted implements Qualifier {
            ENCHANTED
        }

        /// "Equipped \[creature\]" — the creature to which this Equipment is
        /// attached.
        enum Equipped implements Qualifier {
            EQUIPPED
        }

        /// A fixed P/T as a selector qualifier — e.g., Aegis of the Meek:
        /// "Target 1/1 creature" means a creature with base power 1 and
        /// toughness 1.
        record PtQualifier(PtValue pt) implements Qualifier {}

        // Singleton aliases for convenience.
        Qualifier TARGET = Target.TARGET;
        Qualifier HISTORIC = Historic.HISTORIC;
        Qualifier OUTLAW = Outlaw.OUTLAW;
        Qualifier NEGATED_OUTLAW = NegatedOutlaw.NEGATED_OUTLAW;
        Qualifier IS_TOKEN = IsToken.IS_TOKEN;
        Qualifier NON_TOKEN = NonToken.NON_TOKEN;
        Qualifier OTHER = OtherQ.OTHER;

        // Factory methods for parameterized qualifiers
        static Qualifier color(ColorFilter filter) {
            return new Color(filter);
        }

        static Qualifier ofSupertype(Supertype supertype) {
            return new OfSupertype(supertype);
        }

        static Qualifier negatedSupertype(Supertype supertype) {
            return new NegatedSupertype(supertype);
        }

        static Qualifier negatedCardType(CardType type) {
            return new NegatedCardType(type);
        }

        static Qualifier negatedSubtype(Subtype subtype) {
            return new NegatedSubtype(subtype);
        }

        static Qualifier combatStatus(String status) {
            return new CombatStatus(status);
        }
    }

    public sealed interface TypeExpression {
        record Single(SingleType type) implements TypeExpression {}

        /// "(Q1 X) or (Q2 Y)" — disjunction of alternatives, each with its
        /// own optional qualifiers and its own type expression. Oracle
        /// text like "enchanted creature or enchantment creature" is
        /// modeled as Or of two [Alternative]s, each carrying the
        /// qualifiers that apply only to that branch. The outer
        /// [Selector]'s qualifiers hold only the SHARED ones
        /// (e.g., `target`).
        record Or(List<Alternative> alternatives) implements TypeExpression {

            /// One disjunct in an [Or]. Qualifiers and with-clauses
            /// are those that apply specifically to this branch (not
            /// shared with siblings); `type` is the branch's type
            /// expression. Per-branch `withClauses` let a disjunct
            /// like "creature with disturb" carry its "with disturb"
            /// clause without leaking it to sibling branches.
            public record Alternative(List<Qualifier> qualifiers, TypeExpression type, List<WithClause> withClauses) {
                public Alternative(List<Qualifier> qualifiers, TypeExpression type) {
                    this(qualifiers, type, List.of());
                }

                public Alternative(TypeExpression type) {
                    this(List.of(), type, List.of());
                }

                public Alternative(SingleType single) {
                    this(List.of(), new Single(single), List.of());
                }

                public Alternative withWithClauses(List<WithClause> withClauses) {
                    return new Alternative(qualifiers, type, withClauses);
                }
            }
        }

        record Compound(List<SingleType> types) implements TypeExpression {}

        /// Creates a [Single] type expression.
        static TypeExpression single(SingleType type) {
            return new Single(type);
        }

        /// Creates an [Or] type expression from alternatives.
        static TypeExpression or(List<Or.Alternative> alternatives) {
            return new Or(alternatives);
        }

        /// Convenience factory: Or of alternatives each having no branch-
        /// specific qualifiers, from a flat list of single types.
        static TypeExpression orOfSingles(List<SingleType> types) {
            return new Or(types.stream().map(Or.Alternative::new).toList());
        }

        /// Creates a [Compound] type expression.
        static TypeExpression compound(List<SingleType> types) {
            return new Compound(types);
        }
    }

    public sealed interface SingleType {
        record OfGameObject(GameObjectType type) implements SingleType {}

        record OfCard(CardType type) implements SingleType {}

        record OfSubtype(Subtype subtype) implements SingleType {}

        record ObjectCard(GameObjectType object, CardType card) implements SingleType {}

        /// "\[subtype\] \[game-object\]" — a game object further constrained by a
        /// subtype (e.g., "Aura spell", "Arcane spell", "Goblin permanent").
        record ObjectSubtype(GameObjectType object, Subtype subtype) implements SingleType {}

        /// Creates an [OfGameObject] single type.
        static SingleType ofGameObject(GameObjectType type) {
            return new OfGameObject(type);
        }

        /// Creates an [OfCard] single type.
        static SingleType ofCard(CardType type) {
            return new OfCard(type);
        }

        /// Creates an [OfSubtype] single type.
        static SingleType ofSubtype(Subtype subtype) {
            return new OfSubtype(subtype);
        }

        /// Creates an [ObjectCard] single type.
        static SingleType objectCard(GameObjectType object, CardType card) {
            return new ObjectCard(object, card);
        }

        /// Creates an [ObjectSubtype] single type.
        static SingleType objectSubtype(GameObjectType object, Subtype subtype) {
            return new ObjectSubtype(object, subtype);
        }
    }

    /// "with \[predicate\]" / "without \[predicate\]" — refinement on a selector's
    /// type. Variants distinguish structured references to a named keyword
    /// ability ([HasAbility]) from free-text predicates
    /// ([HasPredicate]) that the grammar hasn't refined yet.
    public sealed interface WithClause {
        boolean negated();

        /// Convenience factory — builds a [HasPredicate] so legacy
        /// call sites that pass a plain string keep working.
        static WithClause of(boolean negated, String predicate) {
            return new HasPredicate(negated, predicate);
        }

        /// "with \[keyword\]" / "without \[keyword\]" — structural reference to
        /// a keyword [Ability]. The ability reference identifies whether
        /// a candidate object carries that ability.
        record HasAbility(boolean negated, Ability ability) implements WithClause {}

        /// Free-text predicate — fallback when the grammar hasn't yet
        /// refined the phrase into a structured variant ("with a +1/+1
        /// counter on it", "except for commanders").
        record HasPredicate(boolean negated, String predicate) implements WithClause {}
    }

    /// "that \[predicate\]" — relative-clause restriction on the selector
    /// (e.g., "target spell that targets a player", "each creature that
    /// isn't all colors"). The predicate text is captured verbatim for now
    /// until the grammar refines structured variants.
    public record ThatClause(String predicate) {}

    /// The controller/caster relationship at the tail of a selector
    /// (e.g., "creatures you control", "spells you cast"). Structured as a
    /// sealed union so consumers can reason about the relation without parsing
    /// text: [Controls] for control-based selection (the typical case)
    /// and [Casts] for spell-origin selection.
    public sealed interface ControllerClause {
        /// "\[who\] \[don't \]control\[s\]" — the object is currently controlled by
        /// the referenced player(s). `negated=true` for "you don't
        /// control".
        record Controls(Who who, boolean negated) implements ControllerClause {}

        /// "\[who\] cast\[s\]" — the object (typically a spell) was cast by the
        /// referenced player. Rule 113.3a: controller of the spell on the
        /// stack is the caster.
        record Casts(Who who) implements ControllerClause {}

        /// "\[who\] own\[s\]" — the object is currently owned by the referenced
        /// player(s). Rule 108.3 distinguishes ownership from control;
        /// Hurkyl's Recall uses "target player owns" to pick objects the
        /// target player owns regardless of who currently controls them.
        record Owns(Who who) implements ControllerClause {}

        /// The party standing on the left-hand side of the relation.
        enum Who {
            YOU,
            YOUR_TEAM,
            AN_OPPONENT,
            EACH_OPPONENT,
            YOUR_OPPONENTS,
            TARGET_PLAYER,
            TARGET_OPPONENT,
            /// "Enchanted player" — the player enchanted by this Aura
            /// (Curse of Death's Hold: "Creatures enchanted player
            /// controls get -1/-1.").
            ENCHANTED_PLAYER,
            /// "Its controller" — the controller of the permanent
            /// referred to by "it". Used in self-referential land-entry
            /// effects (Tectonic Instability: "tap all lands its
            /// controller controls.").
            ITS_CONTROLLER,
            /// Refers back to a previously-mentioned player — "they control"
            /// after "target player" (e.g., Early Harvest).
            THEY
        }
    }
}
