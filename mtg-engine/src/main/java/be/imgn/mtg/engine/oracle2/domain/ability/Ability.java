package be.imgn.mtg.engine.oracle2.domain.ability;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.BasicLandType;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Color;
import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.Subtype;
import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// A parsed ability ({@mtg.rule 113}). Sealed at the four ability
/// types of rule 113.3: [Static], [Triggered], [Activated], [Spell].
///
/// Keyword abilities ({@mtg.rule 702}) are *not* a fifth type — each
/// is a shortcut for an ability of one of the four types. The
/// parameter-less keywords live in [StaticKeyword] / [TriggeredKeyword];
/// parameterised keywords are individual records ([Ward], [Toxic],
/// [Equip], …).
///
/// **Pure domain — no string fields, no parsing metadata.** The
/// text→enum mapping for keyword names lives in
/// `oracle2.parser.selector.AbilitySelectorParser`.
public sealed interface Ability permits Ability.Static, Ability.Triggered, Ability.Activated, Ability.Spell {

    /// Rule 702 — static keyword abilities. Sealed via the
    /// implicit-permits rule: every same-file subtype (the
    /// [StaticKeyword] enum and the parameterised static-keyword
    /// records) is auto-permitted.
    sealed interface Static extends Ability {}

    /// Rule 603 / 702 — triggered keyword abilities. Sealed via the
    /// implicit-permits rule.
    sealed interface Triggered extends Ability {}

    /// Rule 602 — activated abilities. `non-sealed` so future
    /// parameterised keywords (Equip, Cycling, …) and written-out
    /// activated abilities can implement it without touching this
    /// file.
    non-sealed interface Activated extends Ability {}

    /// Rule 113.3 — spell abilities. `non-sealed` for the same reason
    /// as [Activated].
    non-sealed interface Spell extends Ability {}

    // ── Written-out ability records (rule 113.3) ──────────────────

    /// A written-out triggered ability ({@mtg.rule 603}). Sealed at
    /// the three trigger words of {@mtg.rule 603.1}: [When], [Whenever],
    /// [At]. The trigger word is encoded in the arm type so it
    /// round-trips back into oracle text without a string field.
    ///
    /// Each arm carries an optional `interveningIf` predicate
    /// ({@mtg.rule 603.4}, "When/Whenever/At [event], if [condition],
    /// [effect]"). The slot is `@Nullable` and currently always `null`
    /// at parse time until the [Condition] hierarchy gains concrete
    /// arms; the field is preserved so call sites that already build
    /// the AST don't need to be touched again when intervening-if
    /// support lands.
    sealed interface TriggeredAbility extends Triggered
            permits TriggeredAbility.When, TriggeredAbility.Whenever, TriggeredAbility.At {

        /// The trigger event that fires this ability. Same shape across
        /// all three arms.
        TriggerEvent event();

        /// The optional intervening-if predicate ({@mtg.rule 603.4}).
        /// Always `null` today; non-null once Condition has concrete
        /// arms.
        @Nullable
        Condition interveningIf();

        /// The effect sentences the ability resolves with. Same shape
        /// across all three arms.
        List<Effect> effects();

        /// `When [event], [effects]` — fires once on a one-shot
        /// specific event (creature enters, dies, casts, …).
        record When(TriggerEvent event, @Nullable Condition interveningIf, List<Effect> effects)
                implements TriggeredAbility {
            public When {
                requireNonNull(event);
                requireNonNull(effects);
                effects = List.copyOf(effects);
            }

            public When(TriggerEvent event, List<Effect> effects) {
                this(event, null, effects);
            }
        }

        /// `Whenever [condition], [effects]` — fires every time the
        /// condition holds (attacks, gains life, casts a spell of
        /// type X, …).
        record Whenever(TriggerEvent event, @Nullable Condition interveningIf, List<Effect> effects)
                implements TriggeredAbility {
            public Whenever {
                requireNonNull(event);
                requireNonNull(effects);
                effects = List.copyOf(effects);
            }

            public Whenever(TriggerEvent event, List<Effect> effects) {
                this(event, null, effects);
            }
        }

        /// `At [phase], [effects]` — fires at a turn-phase boundary.
        /// In oracle text always `At the beginning of [step/phase]`.
        record At(TriggerEvent event, @Nullable Condition interveningIf, List<Effect> effects)
                implements TriggeredAbility {
            public At {
                requireNonNull(event);
                requireNonNull(effects);
                effects = List.copyOf(effects);
            }

            public At(TriggerEvent event, List<Effect> effects) {
                this(event, null, effects);
            }
        }
    }

    /// An activated ability ({@mtg.rule 602}) — "Cost: Effect."
    /// Flavor-word prefixes ("Sleight of Hand — " on Tymora's
    /// Invoker) carry no game function per {@mtg.rule 207.2d} and
    /// are silently consumed by the parser.
    record ActivatedAbility(Cost cost, List<Effect> effects) implements Activated {
        public ActivatedAbility {
            requireNonNull(cost);
            requireNonNull(effects);
            effects = List.copyOf(effects);
        }
    }

    /// A spell ability ({@mtg.rule 113.3a}) — bare effect sentence(s)
    /// on an instant or sorcery. The body is one or more period-
    /// terminated [Effect] sentences.
    record SpellAbility(List<Effect> effects) implements Spell {
        public SpellAbility {
            requireNonNull(effects);
            effects = List.copyOf(effects);
        }
    }

    // ── Parameter-less keyword enums (rule 702) ────────────────────

    /// All parameter-less static keyword abilities ({@mtg.rule 702}).
    /// Each constant corresponds to one keyword. Order is
    /// alphabetical except where a longer-prefix keyword must
    /// precede a shorter one in parser dispatch — currently no such
    /// overlap exists, so plain alphabetical works.
    enum StaticKeyword implements Static {
        AFTERMATH,
        ASCEND,
        ASSIST,
        BANDING,
        CHANGELING,
        COMPLEATED,
        CONVOKE,
        DEATHTOUCH,
        DECAYED,
        DEFENDER,
        DELVE,
        DEVOID,
        DOUBLE_STRIKE,
        EPIC,
        FEAR,
        FIRST_STRIKE,
        FLASH,
        FLYING,
        FOR_MIRRODIN,
        FUSE,
        HASTE,
        HEXPROOF,
        HIDDEN_AGENDA,
        HORSEMANSHIP,
        INDESTRUCTIBLE,
        INFECT,
        INTIMIDATE,
        LIFELINK,
        LIVING_METAL,
        MENACE,
        PARTNER,
        PHASING,
        READ_AHEAD,
        REACH,
        RETRACE,
        RIOT,
        SHADOW,
        SHROUD,
        SKULK,
        SOLVED,
        SPLIT_SECOND,
        SUNBURST,
        TRAMPLE,
        UMBRA_ARMOR,
        UNDAUNTED,
        VIGILANCE,
        WITHER
    }

    /// All parameter-less triggered keyword abilities ({@mtg.rule 702}).
    enum TriggeredKeyword implements Triggered {
        BATTLE_CRY,
        CASCADE,
        DAYBOUND,
        DEMONSTRATE,
        DETHRONE,
        EVOLVE,
        EXALTED,
        EXTORT,
        FLANKING,
        GRAVESTORM,
        HAUNT,
        INGEST,
        LIVING_WEAPON,
        MELEE,
        MENTOR,
        MYRIAD,
        NIGHTBOUND,
        PERSIST,
        PROWESS,
        SOULBOND,
        STORM,
        TRAINING,
        UNDYING,
        VISIT
    }

    // ── Parameterised static-keyword records (rule 702) ────────────

    /// 702.164 — "Toxic N" deals N poison counters when this creature
    /// deals combat damage to a player. `n` is null when the ability
    /// is referenced in a condition check ("has toxic") and the
    /// specific level is irrelevant.
    record Toxic(@Nullable Integer n) implements Static {}

    /// 702.14 — "[type]walk" evasion ("islandwalk", "swampwalk", "legendary
    /// landwalk", …). The walked-land descriptor is whatever
    /// [ObjectSelector] the oracle phrase yields.
    record Landwalk(ObjectSelector selector) implements Static {
        public Landwalk {
            requireNonNull(selector);
        }
    }

    /// 702.5 — Aura's attachment restriction. `target` is the selector
    /// describing what this Aura may attach to ("creature", "creature
    /// you control", "player"). [Selector] is the broadest root since
    /// "Enchant player" attaches to a player slot.
    record Enchant(Selector target) implements Static {
        public Enchant {
            requireNonNull(target);
        }
    }

    /// 702.16 — "Protection from [quality]" static ability. The
    /// quality is usually a color, a subtype, a card type, or one of
    /// the special quality keywords (monocolored, multicolored,
    /// everything). Each quality variant indexes a distinct registry,
    /// so the slot is a sealed [Quality] interface.
    record Protection(Quality quality) implements Static {
        public Protection {
            requireNonNull(quality);
        }

        public sealed interface Quality {
            /// Color quality — "protection from red" ({@mtg.rule 702.16e}).
            record OfColor(Color color) implements Quality {
                public OfColor {
                    requireNonNull(color);
                }
            }

            /// Subtype quality — "protection from Goblins", "protection
            /// from Elves". Plural form in oracle text maps to the
            /// singular subtype.
            record OfSubtype(Subtype subtype) implements Quality {
                public OfSubtype {
                    requireNonNull(subtype);
                }
            }

            /// Card-type quality — "protection from creatures",
            /// "protection from artifacts". Oracle text uses the
            /// plural form; the canonical [CardType] enum value is
            /// singular.
            record OfCardType(CardType cardType) implements Quality {
                public OfCardType {
                    requireNonNull(cardType);
                }
            }

            /// Stateless quality markers — "protection from
            /// monocolored" ({@mtg.rule 702.16i}), "protection from
            /// multicolored" ({@mtg.rule 702.16h}), "protection from
            /// each color" ({@mtg.rule 702.16e}, Iridescent Angel —
            /// shorthand for protection from each of the five
            /// colors). Each variant has no payload beyond its
            /// identity, so they live in a single umbrella enum per
            /// the package convention.
            enum Standard implements Quality {
                MONOCOLORED,
                MULTICOLORED,
                EACH_COLOR
            }

            /// "Protection from X and from Y [and from Z]?" — a
            /// composite quality whose constituents are independent
            /// (the permanent has protection from each of the listed
            /// qualities simultaneously). Kitsune Riftwalker:
            /// "Protection from Spirits and from Arcane". The
            /// constituents themselves may not be `AllOf` (no
            /// nesting); the parser enforces a flat list.
            record AllOf(List<Quality> qualities) implements Quality {
                public AllOf {
                    qualities = List.copyOf(qualities);
                    if (qualities.size() < 2) {
                        throw new IllegalArgumentException(
                                "Quality.AllOf needs at least 2 qualities, got " + qualities.size());
                    }
                }
            }
        }
    }

    /// Continuous P/T modifier ({@mtg.rule 613.1d}, layer 7c). "[subject]
    /// get(s) ±N/±N" — applies a power/toughness delta to all matching
    /// permanents while the source is on the battlefield. Bad Moon
    /// ("Black creatures get +1/+1."), Night of Souls' Betrayal
    /// ("All creatures get -1/-1.").
    record ModifyPT(Selector subject, int power, int toughness) implements Static {
        public ModifyPT {
            requireNonNull(subject);
        }
    }

    /// Continuous ability-grant ({@mtg.rule 613.1f}, layer 6).
    /// "[subject] have/has [ability]" — grants the named ability to
    /// every matching permanent while the source is on the
    /// battlefield. Concordant Crossroads / Mass Hysteria ("All
    /// creatures have haste.").
    record GainAbility(Selector subject, Ability ability) implements Static {
        public GainAbility {
            requireNonNull(subject);
            requireNonNull(ability);
        }
    }

    /// Continuous ability-removal ({@mtg.rule 613.1f}, layer 6).
    /// "[subject] lose [ability]" — removes the named ability from
    /// every matching permanent while the source is on the
    /// battlefield. Gravity Sphere ("All creatures lose flying.").
    record LoseAbility(Selector subject, Ability ability) implements Static {
        public LoseAbility {
            requireNonNull(subject);
            requireNonNull(ability);
        }
    }

    /// Continuous color-set ({@mtg.rule 613.1c}, layer 5). "[subject]
    /// are [color]" — sets the color of matching permanents, removing
    /// existing colors. Darkest Hour ("All creatures are black."),
    /// Ghostflame Sliver ("All Slivers are colorless.").
    record SetColors(Selector subject, Colors colors) implements Static {
        public SetColors {
            requireNonNull(subject);
            requireNonNull(colors);
        }

        /// What color(s) the subject is set to. A single color is the
        /// most common form; [Standard.COLORLESS] is the no-color
        /// variant. Multi-color and "the chosen color" land here as
        /// new arms when the cards that need them appear.
        public sealed interface Colors {
            /// Single-color variant — "are black", "are red".
            record Of(Color color) implements Colors {
                public Of {
                    requireNonNull(color);
                }
            }

            /// Stateless color markers — "are colorless", "are all
            /// colors".
            enum Standard implements Colors {
                COLORLESS,
                /// All five colors at once. Transguild Courier:
                /// "~ is all colors.".
                ALL_COLORS
            }
        }
    }

    /// Continuous type-change ({@mtg.rule 613.1d}, layer 4). "[subject]
    /// are [BasicLandType]" — sweeping replacement that turns each
    /// matching land into the named basic land type (loses all other
    /// land subtypes, gains the matching mana ability per
    /// {@mtg.rule 305.7}). Blood Moon / Magus of the Moon ("Nonbasic
    /// lands are Mountains."), Harbinger of the Seas ("Nonbasic lands
    /// are Islands.").
    record SetBasicLandType(Selector subject, BasicLandType landType) implements Static {
        public SetBasicLandType {
            requireNonNull(subject);
            requireNonNull(landType);
        }
    }

    /// Cost-modification umbrella ({@mtg.rule 117.7}). Sealed at the
    /// two directional arms — [DecreaseCost] for reductions
    /// ({@mtg.rule 601.2f}) and [IncreaseCost] for increases
    /// ({@mtg.rule 601.2g}). They share the same `(source, amount)`
    /// shape but resolve at distinct cost-calculation sub-steps and
    /// have different floors (reductions cap at zero, increases
    /// don't), so they're separate types rather than a flag on a
    /// shared shape.
    sealed interface ModifyCost extends Static {
        CostSource source();

        Cost.ManaCost amount();
    }

    /// Cost reduction ({@mtg.rule 601.2f}). "[source] cost [amount]
    /// less [to cast]?." — reduces the total cost of the matching
    /// spells, or the variable cost of a keyword ability
    /// ({@mtg.rule 702.1a}), by `amount` mana. Helm of Awakening
    /// ("Spells cost {1} less to cast."), Memory Crystal ("Buyback
    /// costs cost {2} less.").
    record DecreaseCost(CostSource source, Cost.ManaCost amount) implements ModifyCost {
        public DecreaseCost {
            requireNonNull(source);
            requireNonNull(amount);
        }
    }

    /// Cost increase ({@mtg.rule 601.2g}). "[source] cost [amount]
    /// more [to cast]?." — increases the total cost of the matching
    /// spells, or the variable cost of a keyword ability
    /// ({@mtg.rule 702.1a}), by `amount` mana. Sphere of Resistance
    /// ("Spells cost {1} more to cast.").
    record IncreaseCost(CostSource source, Cost.ManaCost amount) implements ModifyCost {
        public IncreaseCost {
            requireNonNull(source);
            requireNonNull(amount);
        }
    }

    /// What [DecreaseCost] / [IncreaseCost] target: spells matching
    /// a [Selector] ("Spells you cast cost …") or the variable cost
    /// of a keyword ability ("Buyback costs cost …",
    /// {@mtg.rule 702.1a}). The two arms have distinct resolution
    /// semantics, so they are separate types rather than a flag on
    /// a shared shape.
    sealed interface CostSource {
        /// Spells matching the selector — "Spells cost X less to
        /// cast.", "Creature spells you cast cost {1} less.".
        record Spells(Selector selector) implements CostSource {
            public Spells {
                requireNonNull(selector);
            }
        }
    }

    /// Keyword abilities whose variable cost can be referenced
    /// collectively in oracle text ({@mtg.rule 702.1a}). One constant
    /// per cost-bearing keyword; the enum implements [CostSource]
    /// directly so the source slot reads
    /// `DecreaseCost[source=BUYBACK, …]`. New constants land here as
    /// cards demand them.
    enum KeywordCost implements CostSource {
        BUYBACK
    }

    /// Maximum hand size modifier ({@mtg.rule 402.2}). "[who] [have|has]
    /// [size] maximum hand size." Adjusts the per-player maximum hand
    /// size (default 7) at the cleanup-step discard check. Graceful
    /// Adept / Spellbook ("You have no maximum hand size.").
    record MaximumHandSize(Selector who, HandSize handSize) implements Static {
        public MaximumHandSize {
            requireNonNull(who);
            requireNonNull(handSize);
        }

        /// What the maximum is set to. Only the "no maximum" form
        /// lands today; numeric ("is N") and delta ("+N") forms get
        /// dedicated arms when cards demand them.
        public sealed interface HandSize {
            /// Stateless markers — "no maximum hand size" removes the
            /// 7-card cap entirely.
            enum Standard implements HandSize {
                NONE
            }
        }
    }

    /// Continuous supertype-removal ({@mtg.rule 613.1d}, layer 4).
    /// "[subject] are no longer [supertype]" — strips the named
    /// supertype from every matching permanent. Melting ("All lands
    /// are no longer snow.").
    record RemoveSupertype(Selector subject, Supertype supertype) implements Static {
        public RemoveSupertype {
            requireNonNull(subject);
            requireNonNull(supertype);
        }
    }

    /// Continuous "enters tapped" replacement ({@mtg.rule 614}).
    /// "[subject] enter(s) tapped" — every matching permanent enters
    /// the battlefield tapped instead of untapped. Orb of Dreams
    /// ("Permanents enter tapped.").
    record EnterTapped(Selector subject) implements Static {
        public EnterTapped {
            requireNonNull(subject);
        }
    }

    /// 702.40 — "Affinity for [type]" cost-reduction. The reduction
    /// target may be a subtype (Tangle Golem: "Affinity for Forests")
    /// or a card type (Frogmite, Myr Enforcer: "Affinity for
    /// artifacts"). Distinct sealed variants since each indexes a
    /// different type registry.
    record Affinity(For target) implements Static {
        public Affinity {
            requireNonNull(target);
        }

        public Affinity(Subtype subtype) {
            this(new For.OfSubtype(subtype));
        }

        public Affinity(CardType cardType) {
            this(new For.OfCardType(cardType));
        }

        public sealed interface For {
            record OfSubtype(Subtype subtype) implements For {
                public OfSubtype {
                    requireNonNull(subtype);
                }
            }

            record OfCardType(CardType cardType) implements For {
                public OfCardType {
                    requireNonNull(cardType);
                }
            }
        }
    }

    // ── Parameterised triggered-keyword records (rule 702) ─────────

    /// 702.21 — "Whenever this becomes the target of a spell or
    /// ability an opponent controls, counter it unless that player
    /// pays [cost]." Most print as a mana cost ("Ward {2}") but the
    /// em-dash form carries an arbitrary non-mana cost (Sire of Seven
    /// Deaths: "Ward—Pay 7 life.").
    record Ward(Cost cost) implements Triggered {
        public Ward {
            requireNonNull(cost);
        }
    }

    /// 702.115 — "Support N" (Lead by Example: "Support 2"). When this
    /// creature enters, put a +1/+1 counter on each of up to N other
    /// target creatures.
    record Support(int count) implements Triggered {}

    /// 702.130 — "Afflict N" (Khenra Eternal: "Afflict 1"). Whenever
    /// this creature becomes blocked, defending player loses N life.
    record Afflict(int n) implements Triggered {}

    /// 702.45 — "Bushido N" (Devoted Retainer: "Bushido 1"). Whenever
    /// this creature blocks or becomes blocked, it gets +N/+N until
    /// end of turn.
    record Bushido(int n) implements Triggered {}

    /// 702.135 — "Afterlife N" (Debtors' Transport: "Afterlife 2").
    /// When this creature dies, create N 1/1 white-and-black Spirit
    /// creature tokens with flying.
    record Afterlife(int n) implements Triggered {}

    /// "Firebending N" — Avatar universes-beyond numeric keyword (Mai
    /// and Zuko: "Firebending 3"). Functional rules match the
    /// Flanking-style triggered pattern.
    record Firebending(int n) implements Triggered {}

    // ── Parameterised activated-keyword records (rule 702) ─────────

    /// 702.6 — "Equip [type]? [cost]" activates to attach this
    /// Equipment to a target creature. The cost may be mana only
    /// ("Equip {2}") or include non-mana elements ("Equip—Discard a
    /// card.", Murderer's Axe). The optional `restriction` narrows
    /// the attachable creature — by named subtype (Steelclaw Lance:
    /// "Equip Knight {1}") or by supertype (Blackblade Reforged:
    /// "Equip legendary creature {3}").
    record Equip(@Nullable Restriction restriction, Cost cost) implements Activated {
        public Equip {
            requireNonNull(cost);
        }

        public Equip(Cost cost) {
            this(null, cost);
        }

        /// Equip-target restriction. Models the supertype-restricted
        /// equips ("legendary creature", "creature token") and named-
        /// subtype equips without a free-text fallback.
        public sealed interface Restriction {
            record OfSubtype(Subtype subtype) implements Restriction {
                public OfSubtype {
                    requireNonNull(subtype);
                }
            }

            enum LegendaryCreature implements Restriction {
                LEGENDARY_CREATURE
            }

            enum CreatureToken implements Restriction {
                CREATURE_TOKEN
            }
        }
    }

    /// 702.29 — "Cycling [cost]" — pay cost, discard this card to
    /// draw a card. Cost is usually mana, but some variants take
    /// non-mana costs.
    record Cycling(Cost cost) implements Activated {
        public Cycling {
            requireNonNull(cost);
        }
    }

    /// 702.131 — "Outlast [cost]" (Disowned Ancestor: "Outlast {1}{B}").
    /// Pay cost as a sorcery to put a +1/+1 counter on this creature.
    record Outlast(Cost cost) implements Activated {
        public Outlast {
            requireNonNull(cost);
        }
    }

    /// 702.122 — "Crew N" Vehicle activation (Debris Beetle: "Crew 2").
    /// Tap any number of other creatures you control with total power
    /// ≥ N to turn this Vehicle into an artifact creature until end
    /// of turn. `power` is the aggregate-power threshold, not a mana
    /// cost.
    record Crew(Amount power) implements Activated {
        public Crew {
            requireNonNull(power);
        }
    }

    /// 702.77 — "Reinforce N—[cost]" (Burrenton Bombardier: "Reinforce
    /// 2—{2}{W}"). Pay cost and discard this card to put N +1/+1
    /// counters on target creature.
    record Reinforce(int count, Cost cost) implements Activated {
        public Reinforce {
            requireNonNull(cost);
        }
    }

    /// 702.139 — "Encore [cost]" (Broodmate Tyrant). Pay cost and
    /// exile this card from your graveyard to create a copy of it
    /// for each opponent that attacks that opponent this turn, then
    /// sacrifice the tokens at the beginning of the next end step.
    record Encore(Cost cost) implements Activated {
        public Encore {
            requireNonNull(cost);
        }
    }
}
