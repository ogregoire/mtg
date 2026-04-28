package be.imgn.mtg.engine.oracle.domain;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/// Composable object selector: \[quantifier\] \[qualifier\]* objectType \[with\]* \[that\]* \[controller\] \[zone\].
///
/// `objectType` is the [GameObjectType] the selector picks
/// (`PERMANENT` by default, `SPELL` / `CARD` / `ABILITY` / etc. when
/// the oracle text names one). Card-type, subtype, supertype, and
/// other axes that used to live in the legacy `type: TypeExpression`
/// are now expressed as `Qualifier`s in the `qualifiers` list —
/// `Types(IsCardType(CREATURE))` for "creature",
/// `Types(IsSubtype(GOBLIN))` for "Goblin", etc.
///
/// Multi-branch selectors that differ across axes (e.g., "Goblin
/// creature or Knight") are modeled at a layer above this record via
/// [SelectorExpression.Or].
public record Selector(
        Quantifier quantifier,
        List<Qualifier> qualifiers,
        GameObjectType objectType,
        List<WithClause> withClauses,
        List<ThatClause> thatClauses,
        @Nullable ControllerClause controller,
        Zone.@Nullable Named zone) {

    public Selector(
            Quantifier quantifier,
            List<Qualifier> qualifiers,
            GameObjectType objectType,
            List<WithClause> withClauses,
            @Nullable ControllerClause controller) {
        this(quantifier, qualifiers, objectType, withClauses, List.of(), controller, null);
    }

    public Selector(Quantifier quantifier, GameObjectType objectType) {
        this(quantifier, List.of(), objectType, List.of(), List.of(), null, null);
    }

    public Selector(Quantifier quantifier, List<Qualifier> qualifiers, GameObjectType objectType) {
        this(quantifier, qualifiers, objectType, List.of(), List.of(), null, null);
    }

    public Selector(GameObjectType objectType) {
        this(Quantifier.one(), List.of(), objectType, List.of(), List.of(), null, null);
    }

    public Selector(List<Qualifier> qualifiers, GameObjectType objectType) {
        this(Quantifier.one(), qualifiers, objectType, List.of(), List.of(), null, null);
    }

    public Selector withWithClause(WithClause wc) {
        return new Selector(quantifier, qualifiers, objectType, List.of(wc), thatClauses, controller, zone);
    }

    /// Appends a [WithClause] to the selector, preserving any existing
    /// with-clauses. Used by trailing exception parsers (e.g., "except for
    /// commanders") that stack on top of a primary "with" clause.
    public Selector addWithClause(WithClause wc) {
        var combined = new ArrayList<>(withClauses);
        combined.add(wc);
        return new Selector(quantifier, qualifiers, objectType, List.copyOf(combined), thatClauses, controller, zone);
    }

    public Selector withThatClause(ThatClause tc) {
        return new Selector(quantifier, qualifiers, objectType, withClauses, List.of(tc), controller, zone);
    }

    public Selector withController(ControllerClause cc) {
        return new Selector(quantifier, qualifiers, objectType, withClauses, thatClauses, cc, zone);
    }

    public Selector withZone(Zone.Named z) {
        return new Selector(quantifier, qualifiers, objectType, withClauses, thatClauses, controller, z);
    }

    /// Appends a [Qualifier] to the qualifier list, preserving any
    /// existing qualifiers. Used by trailing-position qualifiers like
    /// `of each basic land type` that the prefix-position parser
    /// can't reach (Coalition Victory).
    public Selector addQualifier(Qualifier q) {
        var combined = new ArrayList<>(qualifiers);
        combined.add(q);
        return new Selector(quantifier, List.copyOf(combined), objectType, withClauses, thatClauses, controller, zone);
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

        /// "no \<type\>" — count-zero quantifier used in conditions
        /// like "if you control no artifacts" (Artificer's Epiphany)
        /// and "if there are no cards in graveyards". Distinct from
        /// [Count](0) since the oracle wording is different and the
        /// engine may want to surface "absence" as a typed concept.
        enum None implements Quantifier {
            NONE
        }

        enum The implements Quantifier {
            THE
        }

        enum Variable implements Quantifier {
            VARIABLE
        }

        /// "That many" — back-reference to an amount established
        /// earlier in the resolution (Phyrexian Negator: "Whenever
        /// this creature is dealt damage, sacrifice that many
        /// permanents."). The binding source is typically a prior
        /// damage event or "X cards" clause.
        enum ThatMany implements Quantifier {
            THAT_MANY
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

        /// Returns the [None] singleton.
        static Quantifier none() {
            return None.NONE;
        }

        /// Returns the [The] singleton.
        static Quantifier the() {
            return The.THE;
        }

        /// Returns the [Variable] singleton.
        static Quantifier variable() {
            return Variable.VARIABLE;
        }

        /// Returns the [ThatMany] singleton.
        static Quantifier thatMany() {
            return ThatMany.THAT_MANY;
        }
    }

    public sealed interface Qualifier {
        enum Target implements Qualifier {
            TARGET
        }

        /// Color qualifier — wraps a [ColorMatcher] boolean tree.
        /// A single atom for "blue creature" / "nonblue creature";
        /// `ColorMatcher.Any` for disjunctive "blue or green creature";
        /// `ColorMatcher.All` for conjunctive "nonblue, nongreen creature"
        /// (folded by [#mergeColorQualifiers] in the parser).
        record Colors(ColorMatcher matcher) implements Qualifier {}

        /// Type qualifier — wraps a [TypeMatcher] boolean tree
        /// covering every type axis (card type, subtype, supertype,
        /// game-object class, role) in one place. A single atom for
        /// "creature" / "non-Human creature"; `Any` for "creature or
        /// planeswalker" or mixed-axis "creature or Vehicle"; `All`
        /// for "noncreature, nonland spell" or "Goblin creature"
        /// (folded by the merge step).
        record Types(TypeMatcher matcher) implements Qualifier {}

        /// Status qualifier values — conditions a permanent can have,
        /// or a resolution-history/role tag attached to a card. Used on
        /// selector clauses like "target tapped creature", "a suspended
        /// card", "each noncommander creature", etc.
        enum Status implements Qualifier {
            TAPPED,
            UNTAPPED,
            FACE_DOWN,
            FACE_UP,
            /// Currently phased out (rule 702.26 phasing).
            PHASED_OUT,
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
            /// Transformed — showing its back face (Mutagen Connoisseur:
            /// "for each transformed permanent you control").
            TRANSFORMED,
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

        /// "activated" / "triggered" — source type of an ability target
        /// (Tale's End: "target activated ability, triggered ability, or
        /// legendary spell"). Distinguishes 113.3a activated abilities
        /// from 113.3b triggered abilities when the selector targets an
        /// ability on the stack.
        enum AbilitySource implements Qualifier {
            ACTIVATED,
            TRIGGERED
        }

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

        /// Narrows a PLAYER game-object to a specific role (opponent,
        /// controller, etc.) — used when a heterogeneous target list
        /// includes both object types and a player (Price of Betrayal:
        /// "target artifact, creature, planeswalker, or opponent").
        record PlayerRole(PlayerRef role) implements Qualifier {}

        /// Coverage-axis predicate — "of each basic land type", "of
        /// each color" (Coalition Victory: "if you control a land of
        /// each basic land type and a creature of each color"). The
        /// permanent satisfies the qualifier collectively iff the
        /// player has at least one matching object for every value
        /// of the named axis.
        record OfEach(CoverageAxis axis) implements Qualifier {}

        enum CoverageAxis {
            /// All five basic land types (Plains, Island, Swamp,
            /// Mountain, Forest).
            BASIC_LAND_TYPE,
            /// All five colors (W, U, B, R, G).
            COLOR
        }

        // Singleton aliases for convenience.
        Qualifier TARGET = Target.TARGET;
        Qualifier HISTORIC = Historic.HISTORIC;
        Qualifier OUTLAW = Outlaw.OUTLAW;
        Qualifier NEGATED_OUTLAW = NegatedOutlaw.NEGATED_OUTLAW;
        Qualifier IS_TOKEN = IsToken.IS_TOKEN;
        Qualifier NON_TOKEN = NonToken.NON_TOKEN;
        Qualifier OTHER = OtherQ.OTHER;

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

        /// "commander" — Commander-format role designation used in
        /// type-slot positions (Witch's Clinic: "target commander").
        /// Distinct from [OfSubtype] because commanders are not
        /// a subtype per rule 205.3; the role is chosen at deck
        /// construction.
        record OfRole(Role role) implements SingleType {}

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

        /// Creates an [OfRole] single type.
        static SingleType ofRole(Role role) {
            return new OfRole(role);
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
    /// type. The wrapper carries the polarity ([With] / [Without]); the
    /// inner [Body] sealed type carries the structural payload (keyword
    /// reference, P/T comparison, free-text predicate, …).
    public sealed interface WithClause {
        Body body();

        /// Wraps a body as a positive ("with …") clause.
        static WithClause with(Body body) {
            return new With(body);
        }

        /// Wraps a body as a negative ("without …") clause.
        static WithClause without(Body body) {
            return new Without(body);
        }

        /// Convenience factory — builds a [Body.HasPredicate] so legacy
        /// call sites that pass a plain string keep working.
        static WithClause of(boolean negated, String predicate) {
            return negated ? without(new Body.HasPredicate(predicate)) : with(new Body.HasPredicate(predicate));
        }

        record With(Body body) implements WithClause {}

        record Without(Body body) implements WithClause {}

        /// Structural payload of a [WithClause]. Distinguishes named
        /// keyword references ([HasAbility]) from free-text predicates
        /// ([HasPredicate]) and the various structured comparisons.
        sealed interface Body {

            /// "\[keyword\]" — structural reference to a keyword
            /// [Ability]. The ability reference identifies whether a
            /// candidate object carries that ability.
            record HasAbility(Ability ability) implements Body {}

            /// "\[X\] or \[Y\]" — disjunction of keyword abilities
            /// (Orchard Spirit: "creatures with flying or reach").
            /// Matches when the candidate carries any one of the listed
            /// abilities.
            record HasAnyAbility(List<Ability> abilities) implements Body {}

            /// Free-text predicate — fallback when the grammar hasn't yet
            /// refined the phrase into a structured variant ("a +1/+1
            /// counter on it", "except for commanders").
            record HasPredicate(String predicate) implements Body {}

            /// "the same name as \[reference\]" — name-equality against a
            /// referent permanent (Wake of Destruction: "target land and
            /// all other lands with the same name as that land"). The
            /// reference is a demonstrative subject ("that land", "this
            /// creature"); the static-init cycle with full SUBJECT is
            /// avoided by naming only the demonstrative shape.
            record SameNameAs(String reference) implements Body {}

            /// "named \<card-name\>" — direct name match (Clever
            /// Conjurer: "Untap target permanent not named ~." where ~
            /// is the card's self-reference).
            /// Literal card name — the only legitimate String in this module.
            record HasName(String name) implements Body {}

            /// "mana value of the chosen quality" — back-reference to a
            /// preceding [Effect.ChooseQuality] (Extinction Event:
            /// "Choose odd or even. Exile each creature with mana value
            /// of the chosen quality."). The chosen parity is bound at
            /// resolution; this clause matches any object whose mana
            /// value has that parity.
            record HasManaValueOfChosenQuality() implements Body {}

            /// "the chosen name" — back-reference to a preceding
            /// [Effect.ChooseCardName] (Declaration of Naught: "Counter
            /// target spell with the chosen name.").
            record HasChosenName() implements Body {}

            /// "mana value \[matcher\]" — mana-value comparison using an
            /// [AmountMatcher] (Up the Beanstalk: "a spell with mana
            /// value 5 or greater"). Covers "N or greater", "N or less",
            /// "at least N", "at most N", "exactly N", and bare "N".
            record HasManaValue(AmountMatcher matcher) implements Body {}

            /// "the same mana value as the \[participial\] \[noun\]" —
            /// mana-value equality against a cost-referent (Sanguine
            /// Praetor: "each creature with the same mana value as the
            /// sacrificed creature"). The reference is stored as free
            /// text to avoid a static-init cycle with the full SUBJECT
            /// grammar.
            record SameManaValueAs(String reference) implements Body {}

            /// "\[power|toughness\] \[cmp\] \[reference\]" — structural
            /// P/T comparison against a dynamic value (Blazing Hope:
            /// "with power greater than or equal to your life total").
            /// `aspect` names which characteristic ("power" /
            /// "toughness"); `cmp` captures the comparator; `reference`
            /// is the right-hand side, currently stored as free text so
            /// the full SUBJECT grammar isn't forced through the
            /// WITH-clause path and cause a static-init cycle.
            record PtComparison(Aspect aspect, Comparator cmp, String reference) implements Body {
                public enum Aspect {
                    POWER,
                    TOUGHNESS
                }

                public enum Comparator {
                    LESS_THAN,
                    LESS_THAN_OR_EQUAL,
                    GREATER_THAN,
                    GREATER_THAN_OR_EQUAL,
                    EQUAL
                }
            }
        }
    }

    /// "that \[predicate\]" — relative-clause restriction on the selector
    /// (e.g., "target spell that targets a player", "each creature
    /// that isn't all colors"). Either a structured variant (see
    /// nested records) or a free-text fallback for shapes the grammar
    /// hasn't structured yet.
    public sealed interface ThatClause {
        /// Free-text predicate (fallback for unstructured clauses).
        record Predicate(String predicate) implements ThatClause {}

        /// "of \[that|the chosen\] type" — back-reference to a preceding
        /// [Effect.ChooseType] effect (Distant Melody: "each permanent
        /// you control of that type."). No data — the referent is the
        /// most recently chosen type in the same resolution.
        enum ReferencedType implements ThatClause {
            REFERENCED_TYPE
        }

        /// "from a\[n\] \[card-type\] source" — origin-source restriction
        /// on an ability-target selector (Rust: "Counter target activated
        /// ability from an artifact source."). The source is the
        /// permanent that activated the ability; this clause narrows the
        /// match to a specific card type.
        record FromSourceOfType(CardType type) implements ThatClause {}

        /// "named \<card-name\>" — name-equality restriction (Powerstone
        /// Shard: "each artifact you control named Powerstone Shard.";
        /// Gisela, the Broken Blade: "a creature named Bruna, the
        /// Fading Light"). The name is the one legitimate String in
        /// this module — a literal card name, the meld partner / named-
        /// reference target.
        record NamedAs(String name) implements ThatClause {}

        /// "who has cast \[selector\] this turn" — cast-history relative
        /// clause on a player subject (Ethersworn Canonist: "Each player
        /// who has cast a nonartifact spell this turn …").
        record HasCast(Selector spell) implements ThatClause {}

        /// Legacy constructor for the string-predicate form. Prefer
        /// the structured variants when available.
        static ThatClause of(String predicate) {
            return new Predicate(predicate);
        }
    }

    /// The controller/caster relationship at the tail of a selector
    /// (e.g., "creatures you control", "spells you cast"). Structured as a
    /// sealed union so consumers can reason about the relation without parsing
    /// text: [Controls] for control-based selection (the typical case)
    /// and [Casts] for spell-origin selection.
    public sealed interface ControllerClause {

        /// "\<who\> \<verb\>" — affirmative.
        record Does(Body body) implements ControllerClause {}

        /// "\<who\> \[don't|doesn't\] \<verb\>" — negated.
        record DoesNot(Body body) implements ControllerClause {}

        /// Convenience factory wrapping a [Body] in [Does].
        static ControllerClause does(Body body) {
            return new Does(body);
        }

        /// Convenience factory wrapping a [Body] in [DoesNot].
        static ControllerClause doesNot(Body body) {
            return new DoesNot(body);
        }

        /// Structural payload of a [ControllerClause]. Atomic relations
        /// name a [Who] and the relation kind ([Controls], [Owns],
        /// [Casts], [Discarded], [Attacking]); compound relations
        /// combine sibling bodies via [AllOf] / [AnyOf].
        public sealed interface Body {

            /// "\[who\] control\[s\]" — the object is currently
            /// controlled by the referenced player(s).
            record Controls(PlayerRef who) implements Body {}

            /// "\[who\] own\[s\]" — the object is currently owned by
            /// the referenced player(s) (rule 108.3). Hurkyl's Recall
            /// uses "target player owns" to pick objects regardless of
            /// who currently controls them.
            record Owns(PlayerRef who) implements Body {}

            /// "\[who\] cast\[s\]" — the object (typically a spell) was
            /// cast by the referenced player. Rule 113.3a: controller
            /// of the spell on the stack is the caster. Optional
            /// `fromZone` narrows the source zone ("spells you cast
            /// from your graveyard").
            record Casts(PlayerRef who, @Nullable Zone fromZone) implements Body {
                public Casts(PlayerRef who) {
                    this(who, null);
                }

                public Casts withFromZone(Zone zone) {
                    return new Casts(who, zone);
                }
            }

            /// "\[who\]'ve discarded" — past-tense discard-history
            /// scope (Change of Fortune: "draw a card for each card
            /// you've discarded this turn.").
            record Discarded(PlayerRef who) implements Body {}

            /// "\[who\]'re attacking" — present-progressive attacker
            /// scope (Astral Confrontation: "for each opponent you're
            /// attacking.").
            record Attacking(PlayerRef who) implements Body {}

            /// "\[who\] both \<v1\> and \<v2\>" — conjunction of two or
            /// more atomic bodies. Obelisk of Undoing's "you both own
            /// and control" decomposes into `AllOf([Owns, Controls])`.
            record AllOf(List<Body> bodies) implements Body {}

            /// "\[who\] \<v1\> or \<v2\>" — disjunction of atomic
            /// bodies. Telim'Tor's Edict's "you own or control"
            /// decomposes into `AnyOf([Owns, Controls])`.
            record AnyOf(List<Body> bodies) implements Body {}
        }
    }
}
