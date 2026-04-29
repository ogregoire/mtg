package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.turn.Step;

/// A parsed ability from oracle text.
///
/// The four MTG ability types (rule 113.3) are modelled as sealed interfaces:
/// [Static], [Triggered], [Activated], [Spell].
/// "Written-out" forms have dedicated records ([StaticAbility],
/// [TriggeredAbility], [ActivatedAbility], [SpellAbility]).
///
/// Keyword abilities (rule 702) are *not* a fifth type — each is a shortcut
/// for an ability of one of the four types. No-parameter static keywords are
/// consolidated in [StaticKeyword]; no-parameter triggered keywords in
/// [TriggeredKeyword]. Parameterized keywords are records ([Ward],
/// [Equip], [Landwalk], [Protection], …). Each directly
/// implements its underlying ability type per the rule in 702.Xa.
public sealed interface Ability {

    // ── The four ability types ────────────────────────────────────────

    sealed interface Static extends Ability {}

    sealed interface Triggered extends Ability {}

    sealed interface Activated extends Ability {}

    sealed interface Spell extends Ability {}

    // ── Written-out (non-keyword) ability forms ───────────────────────

    record StaticAbility(String text) implements Static {}

    /// A written-out triggered ability (rule 603). `triggerLimit` caps how
    /// many times per turn the ability can trigger ("This ability triggers
    /// only once each turn." — Mary Jane Watson). `null` for the common
    /// unlimited case. Per-turn is implicit, matching
    /// [Effect.ActivationLimit].
    record TriggeredAbility(
            String triggerWord,
            TriggerEvent event,
            @Nullable Condition interveningIf,
            List<Effect> effects,
            @Nullable Amount triggerLimit)
            implements Triggered {
        public TriggeredAbility(
                String triggerWord, TriggerEvent event, @Nullable Condition interveningIf, List<Effect> effects) {
            this(triggerWord, event, interveningIf, effects, null);
        }

        public TriggeredAbility withTriggerLimit(Amount limit) {
            return new TriggeredAbility(triggerWord, event, interveningIf, effects, limit);
        }
    }

    /// An activated ability (rule 602). `anyPlayerActivation` is set to
    /// the [AnyPlayerActivation] carrying its timing restriction
    /// when oracle text includes the "Any player may activate this
    /// ability \[but only …\]?" modifier (rule 602.5e, Excavation / Well of
    /// Knowledge). `null` for the common controller-only activated
    /// ability.
    record ActivatedAbility(
            Cost cost, List<Effect> effects, @Nullable AnyPlayerActivation anyPlayerActivation) implements Activated {
        public ActivatedAbility(Cost cost, List<Effect> effects) {
            this(cost, effects, null);
        }

        public ActivatedAbility withAnyPlayerActivation(AnyPlayerActivation activation) {
            return new ActivatedAbility(cost, effects, activation);
        }
    }

    /// "Any player may activate this ability \[but only …\]?" — permission
    /// modifier that lifts the controller-only restriction on an
    /// activated ability (rule 602.5e). Variants capture the optional
    /// timing restriction that oracle text can attach.
    sealed interface AnyPlayerActivation {
        /// Plain "Any player may activate this ability." — no
        /// restriction (e.g., Excavation).
        enum Unrestricted implements AnyPlayerActivation {
            UNRESTRICTED
        }

        /// "… but only as a sorcery." — activation is timed as a
        /// sorcery (Scandalmonger, Endbringer's Revel).
        enum AsSorcery implements AnyPlayerActivation {
            AS_SORCERY
        }

        /// "… but only during \[owner\]'s \[step\] step." — activation
        /// windowed to a specific step. `owner` is null and `each` is
        /// true for "during any \[step\] step" (each player's),
        /// Infinite Hourglass. Otherwise owner names a player (e.g.,
        /// Well of Knowledge: "during their draw step").
        record DuringStep(@Nullable Subject owner, boolean each, Step step) implements AnyPlayerActivation {}

        /// "… but only during \[owner\]'s turn \[before the end step\]?."
        /// — activation windowed to the owner's turn. `beforeEndStep`
        /// is true for the Mana Cache form that also excludes the end
        /// step.
        record DuringTurn(Subject owner, boolean beforeEndStep) implements AnyPlayerActivation {}

        /// "… but only if \[condition\]." — activation is gated by a
        /// predicate (Lightning Storm: "but only if Lightning Storm is
        /// on the stack.").
        record IfCondition(Condition condition) implements AnyPlayerActivation {}
    }

    record SpellAbility(List<Effect> effects) implements Spell {}

    /// "As \[subject\] enters, \[effects\]" — replacement-style ETB
    /// ability (rule 614.1c, 614.12). Body effects apply *as* the
    /// permanent enters, not after — they're part of the ETB event
    /// itself, visible to other replacement effects, and do not go on
    /// the stack. Distinct from [TriggeredAbility] with `triggerWord
    /// = "when"`, which fires after the permanent has already entered.
    /// Implements [Static] because replacement effects are produced
    /// by static abilities (rule 614.1).
    record AsEntersAbility(
            TriggerEvent.Enters event, @Nullable Condition interveningIf, List<Effect> effects) implements Static {}

    // ── Other ability structures ──────────────────────────────────────

    record Modal(String quantity, List<Mode> modes) implements Ability {}

    record Chapter(String numerals, List<Effect> effects) implements Ability {}

    record CastingModifier(String text) implements Ability {}

    record Mode(@Nullable Cost cost, List<Effect> effects) {}

    // ── Keyword abilities (rule 702) ─────────────────────────────────
    //
    // Each keyword is a shortcut for a written ability of one of the four
    // types, per rule 702.Xa. No-parameter keywords are consolidated into
    // two enums; parametrized keywords are records.

    // Static keyword abilities (rule 702) ─────────────────────────────

    /// All parameter-less static keyword abilities (rule 702). Each constant
    /// corresponds to a single keyword ability; the name uses SCREAMING_SNAKE
    /// convention matching the canonical oracle text (e.g., FIRST_STRIKE →
    /// "first strike").
    enum StaticKeyword implements Static {
        /// 702.9 — evasion.
        FLYING,
        /// 702.19 — trample combat damage.
        TRAMPLE,
        /// 702.7 — first-strike combat damage step.
        FIRST_STRIKE,
        /// 702.4 — two combat damage steps.
        DOUBLE_STRIKE,
        /// 702.10 — may attack / tap the turn it enters.
        HASTE,
        /// 702.20 — doesn't tap to attack.
        VIGILANCE,
        /// 702.17 — may block flying creatures.
        REACH,
        /// 702.111 — can only be blocked by two or more.
        MENACE,
        /// 702.13 — evasion limited to artifacts/same color.
        INTIMIDATE,
        /// 702.36 — evasion limited to artifacts/black.
        FEAR,
        /// 702.118 — can't be blocked by greater-power creatures.
        SKULK,
        /// 702.2 — destroys anything it damages.
        DEATHTOUCH,
        /// 702.15 — damage dealt also gains life.
        LIFELINK,
        /// 702.11 — can't be targeted by opponents.
        HEXPROOF,
        /// 702.18 — can't be targeted.
        SHROUD,
        /// 702.12 — can't be destroyed.
        INDESTRUCTIBLE,
        /// 702.3 — can't attack.
        DEFENDER,
        /// 702.8 — may be played any time.
        FLASH,
        /// 702.22 — combat grouping.
        BANDING,
        /// 702.26 — may phase out/in.
        PHASING,
        /// 702.31 — evasion over non-horsemanship creatures.
        HORSEMANSHIP,
        /// 702.27 — can only be blocked by creatures with shadow.
        SHADOW,
        /// 702.90 — damage becomes poison/−1 counters.
        INFECT,
        /// 702.80 — damage dealt is via −1/−1 counters.
        WITHER,
        /// 702.73 — every creature type.
        CHANGELING,
        /// 702.114 — colorless.
        DEVOID,
        /// 702.61 — can't be responded to.
        SPLIT_SECOND,
        /// 702.66 — alternative cost using exile-from-graveyard mana.
        DELVE,
        /// 702.51 — alternative cost using tapping creatures.
        CONVOKE,
        /// 702.127 — cast only from graveyard as the aftermath half.
        AFTERMATH,
        /// 702.131 — tracks the city's blessing.
        ASCEND,
        /// 702.147 — exiled zombie tokens.
        DECAYED,
        /// 702.150 — return from graveyard payment.
        COMPLEATED,
        /// 702.169 — solved-case marker.
        SOLVED,
        /// 702.161 — vehicle that's always a creature.
        LIVING_METAL,
        /// 702.163 — equip to a Legendary on ETB.
        FOR_MIRRODIN,
        /// 702.155 — lore counters add one at a time.
        READ_AHEAD,
        /// 702.89 — indestructible aura substitute.
        UMBRA_ARMOR,
        /// 702.102 — split card with fused casting.
        FUSE,
        /// 702.106 — face-down in the command zone.
        HIDDEN_AGENDA,
        /// 702.81 — cast from graveyard by discarding.
        RETRACE,
        /// 702.50 — game-ending spell.
        EPIC,
        /// 702.132 — another player may pay up to {7} of this spell's cost.
        ASSIST,
        /// 702.136 — enters with your choice of a +1/+1 counter or haste.
        RIOT,
        /// 702.173 — Conspiracy-set cost-reduction keyword ("Undaunted":
        /// this spell costs {1} less to cast for each opponent).
        UNDAUNTED,
        /// 702.124 — Commander-format dual-commander keyword (Rograkh,
        /// Son of Rohgahh: "Partner").
        PARTNER,
        /// 702.43 — enters with +1/+1 or charge counters for each color of
        /// mana spent to cast it.
        SUNBURST
    }

    /// 702.164 — deals toxic N poison counters when dealing combat damage.
    /// `n` is null when the ability is used in a condition check ("has
    /// toxic") and the specific level is irrelevant.
    record Toxic(@Nullable Integer n) implements Static {}

    /// 702.16 — protection from one or more qualities. 702.16g makes
    /// "protection from A and from B" shorthand for two separate abilities.
    record Protection(List<ProtectionQuality> qualities) implements Static {}

    /// 702.11d — hexproof variant limited to certain qualities.
    record HexproofFrom(List<ProtectionQuality> qualities) implements Static {}

    /// 702.14 — landwalk evasion, parameterized by the walked-land descriptor.
    record Landwalk(LandSelector selector) implements Static {}

    /// 702.5 — Aura's attachment restriction. `target` is the selector
    /// describing what this Aura may attach to (e.g., "creature", "creature
    /// you control", "nonland permanent"). Player targets (e.g., "Enchant
    /// player") are captured as a bare-type selector.
    record Enchant(Selector target) implements Static {}

    // Triggered keyword abilities ─────────────────────────────────────

    /// All parameter-less triggered keyword abilities (rule 702). Each
    /// constant corresponds to a single triggered keyword ability.
    enum TriggeredKeyword implements Triggered {
        /// 702.108 — +1/+1 on noncreature spells.
        PROWESS,
        /// 702.93 — return with +1/+1 counter.
        UNDYING,
        /// 702.79 — return with −1/−1 counter.
        PERSIST,
        /// 702.83 — +1/+1 when attacking alone.
        EXALTED,
        /// 702.100 — grow when a bigger creature enters.
        EVOLVE,
        /// 702.101 — pay {W/B} on spell cast, drain 1 from each opponent.
        EXTORT,
        /// 702.105 — +1/+1 when attacking the leader.
        DETHRONE,
        /// 702.95 — pair with an unpaired creature.
        SOULBOND,
        /// 702.115 — combat damage to players mills them.
        INGEST,
        /// 702.116 — create attacking copies per opponent.
        MYRIAD,
        /// 702.134 — +1/+1 on attacking creatures.
        MENTOR,
        /// 702.55 — trigger on creature dying.
        HAUNT,
        /// 702.85 — free exile-cast on cast.
        CASCADE,
        /// 702.40 — copy for each spell cast before.
        STORM,
        /// 702.69 — storm-like copy count via graveyard.
        GRAVESTORM,
        /// 702.121 — +1/+0 per attacker to attacking creature.
        MELEE,
        /// 702.149 — +1/+1 when attacking with bigger.
        TRAINING,
        /// 702.145 — day triggers on phase change.
        DAYBOUND,
        /// 702.145 — night triggers on phase change.
        NIGHTBOUND,
        /// 702.144 — opponent may copy triggered result.
        DEMONSTRATE,
        /// 702.159 — attraction-visit effect.
        VISIT,
        /// 702.91 — +1/+0 to other attackers.
        BATTLE_CRY,
        /// 702.92 — ETB-create-Germ-and-attach.
        LIVING_WEAPON,
        /// 702.25 — blocker gets −1/−1 (per 702.25a, triggered).
        FLANKING
    }

    /// 702.21 — "Whenever this becomes the target..., counter unless \[cost\]."
    /// Most print as a mana cost ("Ward {2}") but the em-dash form carries
    /// an arbitrary non-mana cost (Sire of Seven Deaths: "Ward—Pay 7 life.").
    record Ward(Cost cost) implements Triggered {}

    /// 702.115 — "Support N" — when this ETBs, put a +1/+1 counter on
    /// each of up to N other target creatures (Lead by Example: "Support
    /// 2."). Triggered on entry per 702.115a.
    record Support(int count) implements Triggered {}

    /// 702.130 — "Afflict N" — whenever this creature becomes blocked,
    /// defending player loses N life (Khenra Eternal: "Afflict 1").
    record Afflict(int n) implements Triggered {}

    /// "Firebending N" — Avatar universes-beyond numeric keyword
    /// (Mai and Zuko: "Firebending 3"). Functional rules match the
    /// Flanking-style triggered pattern.
    record Firebending(int n) implements Triggered {}

    /// 702.45 — "Bushido N" triggered ability (Devoted Retainer:
    /// "Bushido 1"). Whenever this creature blocks or becomes
    /// blocked, it gets +N/+N until end of turn.
    record Bushido(int n) implements Triggered {}

    /// "Affinity for \[type\]" — rule 702.40 cost-reduction static
    /// keyword (Tangle Golem: "Affinity for Forests"). The spell
    /// costs 1 less to cast for each permanent of the named type
    /// the controller controls.
    /// "Affinity for \[type\]" — rule 702.40 cost-reduction. The
    /// reduction target may be a subtype (Tangle Golem: "Affinity for
    /// Forests") or a card type (Frogmite, Myr Enforcer: "Affinity for
    /// artifacts"). Distinct sealed variants since each indexes a
    /// different type registry.
    record Affinity(For target) implements Static {
        public sealed interface For {
            record OfSubtype(Subtype subtype) implements For {}

            record OfCardType(CardType cardType) implements For {}
        }

        public Affinity(Subtype subtype) {
            this(new For.OfSubtype(subtype));
        }

        public Affinity(CardType cardType) {
            this(new For.OfCardType(cardType));
        }
    }

    // Activated keyword abilities ──────────────────────────────────────

    /// 702.6 — "Equip \[type\]? \[cost\]" activates to attach this Equipment
    /// to a target creature (rule 702.6a). The cost may be mana only
    /// ("Equip {2}") or include non-mana elements ("Equip—Discard a
    /// card.", Murderer's Axe), so the full [Cost] type is used.
    /// The optional `typeRestriction` narrows the attachable creature
    /// — by named subtype (Steelclaw Lance: "Equip Knight {1}") or by
    /// supertype (Blackblade Reforged: "Equip legendary creature {3}").
    record Equip(@Nullable Restriction typeRestriction, Cost cost) implements Activated {
        public Equip(Cost cost) {
            this(null, cost);
        }

        /// Equip-target restriction sealed type. Replaces a bare
        /// [Subtype] field so supertype-restricted equips
        /// ("legendary creature") and future shapes can be modeled
        /// without a free-text fallback.
        public sealed interface Restriction {
            /// "Equip \[Subtype\] \[cost\]" — restricted by creature
            /// subtype (Steelclaw Lance: "Equip Knight {1}").
            record OfSubtype(Subtype subtype) implements Restriction {}

            /// "Equip legendary creature \[cost\]" — restricted to
            /// legendary creatures (Blackblade Reforged).
            enum LegendaryCreature implements Restriction {
                LEGENDARY_CREATURE
            }

            /// "Equip creature token \[cost\]" — restricted to
            /// creature tokens (Team Pennant: "Equip creature token
            /// {1}").
            enum CreatureToken implements Restriction {
                CREATURE_TOKEN
            }
        }
    }

    /// 702.29 — "Cycling \[cost\]" activates to discard this card and draw.
    /// Cost is usually mana, but some variants take non-mana costs too.
    record Cycling(Cost cost) implements Activated {}

    /// 702.131 — "Outlast \<cost\>" activated keyword (Disowned
    /// Ancestor: "Outlast {1}{B}"). Pay cost as a sorcery to put a
    /// +1/+1 counter on this creature.
    record Outlast(Cost cost) implements Activated {}

    /// 702.122 — "Crew N" Vehicle activation: tap any number of other
    /// creatures you control with total power ≥ N to turn this Vehicle
    /// into an artifact creature until end of turn (Debris Beetle:
    /// "Crew 2"). `power` is the aggregate-power threshold, not a
    /// mana cost.
    record Crew(Amount power) implements Activated {}

    /// 702.77 — "Reinforce N—\<cost\>" activated keyword (Burrenton
    /// Bombardier: "Reinforce 2—{2}{W}"). Pay cost and discard this
    /// card to put N +1/+1 counters on target creature.
    record Reinforce(int count, Cost cost) implements Activated {}
}
