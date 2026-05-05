package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.jspecify.annotations.Nullable;

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

    /// A written-out triggered ability ({@mtg.rule 603}). The
    /// `triggerWord` slot captures the literal `When` / `Whenever` /
    /// `At` token that introduced the trigger so it round-trips back
    /// into oracle text. `interveningIf` is the optional
    /// {@mtg.rule 603.4} predicate; today it is always `null` until
    /// the [Condition] hierarchy gets concrete arms.
    record TriggeredAbility(
            String triggerWord,
            TriggerEvent event,
            @Nullable Condition interveningIf,
            List<Effect> effects) implements Triggered {
        public TriggeredAbility {
            requireNonNull(triggerWord);
            requireNonNull(event);
            requireNonNull(effects);
            effects = List.copyOf(effects);
        }

        public TriggeredAbility(String triggerWord, TriggerEvent event, List<Effect> effects) {
            this(triggerWord, event, null, effects);
        }
    }

    /// An activated ability ({@mtg.rule 602}) — "Cost: Effect."
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
