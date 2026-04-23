package be.imgn.mtg.engine.oracle.domain;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A counter type (e.g., +1/+1, loyalty, charge).
public sealed interface CounterType {
    record PtCounter(int power, int toughness) implements CounterType {}

    /// The fifteen keyword counters of {@mtg.rule 122.1b} — a
    /// permanent bearing one gains the wrapped keyword ability. The
    /// attached [#ability] is the canonical [Ability] instance
    /// (typically an [Ability.StaticKeyword]; `EXALTED` resolves to
    /// [Ability.TriggeredKeyword#EXALTED]).
    enum Keyword implements CounterType {
        FLYING(Ability.StaticKeyword.FLYING),
        FIRST_STRIKE(Ability.StaticKeyword.FIRST_STRIKE),
        DOUBLE_STRIKE(Ability.StaticKeyword.DOUBLE_STRIKE),
        DEATHTOUCH(Ability.StaticKeyword.DEATHTOUCH),
        DECAYED(Ability.StaticKeyword.DECAYED),
        EXALTED(Ability.TriggeredKeyword.EXALTED),
        HASTE(Ability.StaticKeyword.HASTE),
        HEXPROOF(Ability.StaticKeyword.HEXPROOF),
        INDESTRUCTIBLE(Ability.StaticKeyword.INDESTRUCTIBLE),
        LIFELINK(Ability.StaticKeyword.LIFELINK),
        MENACE(Ability.StaticKeyword.MENACE),
        REACH(Ability.StaticKeyword.REACH),
        SHADOW(Ability.StaticKeyword.SHADOW),
        TRAMPLE(Ability.StaticKeyword.TRAMPLE),
        VIGILANCE(Ability.StaticKeyword.VIGILANCE);

        private final Ability ability;

        Keyword(Ability ability) {
            this.ability = ability;
        }

        public Ability ability() {
            return ability;
        }

        /// Oracle-text spelling as a `Words.phrase` template — Title
        /// case on the leading word so `phrase()` matches both
        /// mid-sentence (the normal counter-name position) and the
        /// rare sentence-start occurrence.
        public String text() {
            return titleCase(name());
        }
    }

    /// Every non-keyword counter type that appears on a vintage-legal
    /// card, plus the rule-defined special-rule counters
    /// ({@mtg.rule 122.1c-i}) even if no current card uses them by
    /// that name. Keyword counters ({@mtg.rule 122.1b}) live in
    /// [Keyword]. The enum name uppercases the oracle-text spelling
    /// (e.g., `FINALITY` → "finality"). This is a closed set — oracle
    /// text naming a counter not in this enum (and not a keyword)
    /// should be treated as a parse failure, not silently accepted.
    private static String titleCase(String enumName) {
        var lower = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    enum Named implements CounterType {
        // Rule 122.1c-i — Counters with special rule semantics
        SHIELD,
        STUN,
        LOYALTY,
        POISON,
        DEFENSE,
        FINALITY,
        RAD,
        // Mechanic-specific and card-specific counter types observed
        // in vintage-legal oracle text.
        ACORN,
        AEGIS,
        AGE,
        AIM,
        ARROW,
        ARROWHEAD,
        AWAKENING,
        BAIT,
        BLAZE,
        BLESSING,
        BLIGHT,
        BLOOD,
        BLOODLINE,
        BLOODSTAIN,
        BOOK,
        BORE,
        BOUNTY,
        BRIBERY,
        BRICK,
        BURDEN,
        CAGE,
        CARRION,
        CELL,
        CHARGE,
        CHORUS,
        COIN,
        COLLECTION,
        COMPONENT,
        CONQUEROR,
        CONTESTED,
        CORPSE,
        CORRUPTION,
        CREDIT,
        CROAK,
        CRYSTAL,
        CUBE,
        CURRENCY,
        DEATH,
        DELAY,
        DEPLETION,
        DESCENT,
        DESPAIR,
        DISCOVERY,
        DIVINITY,
        DOOM,
        DREAM,
        DREAD,
        DUTY,
        ECHO,
        EGG,
        ELIXIR,
        EMBER,
        ENERGY,
        ENLIGHTENED,
        EON,
        EVERYTHING,
        EXPERIENCE,
        EYEBALL,
        FADE,
        FATE,
        FEATHER,
        FEEDING,
        FELLOWSHIP,
        FETCH,
        FILIBUSTER,
        FILM,
        FIRE,
        FLAME,
        FLOOD,
        FORESHADOW,
        FUNGUS,
        FURY,
        FUSE,
        GEM,
        GHOSTFORM,
        GLYPH,
        GOLD,
        GROWTH,
        HARMONY,
        HATCHLING,
        HIT,
        HONE,
        HOOFPRINT,
        HOPE,
        HOUR,
        HOURGLASS,
        HUNGER,
        ICE,
        IMPOSTOR,
        INCARNATION,
        INCUBATION,
        INFECTION,
        INFLUENCE,
        INGENUITY,
        INGREDIENT,
        INTEL,
        INTERVENTION,
        INVITATION,
        ISOLATION,
        JAVELIN,
        JUDGMENT,
        KI,
        KICK,
        KNOWLEDGE,
        LANDMARK,
        LEVEL,
        LOOT,
        LORE,
        LUCK,
        MAGNET,
        MANIFESTATION,
        MANNEQUIN,
        MATRIX,
        MEMORY,
        MINE,
        MINING,
        MIRE,
        MUSTER,
        NEST,
        NET,
        NIGHT,
        OIL,
        OMEN,
        ORE,
        PAGE,
        PAIN,
        PALLIATION,
        PARALYZATION,
        PETAL,
        PETRIFICATION,
        PHYLACTERY,
        PHYRESIS,
        PIN,
        PLAGUE,
        PLOT,
        POLYP,
        POSSESSION,
        PRESSURE,
        PREY,
        PUPA,
        QUEST,
        RALLY,
        REJECTION,
        REPRIEVE,
        REV,
        REVIVAL,
        RIBBON,
        RITUAL,
        ROPE,
        SCREAM,
        SHELL,
        SHRED,
        SILVER,
        SKEWER,
        SLEEP,
        SLEIGHT,
        SLIME,
        SLUMBER,
        SOOT,
        SOUL,
        SPITE,
        SPORE,
        STASH,
        STORAGE,
        STORY,
        STRIFE,
        STUDY,
        SUPPLY,
        SUSPECT,
        TAKEOVER,
        TASK,
        THEFT,
        TICKET,
        TIDE,
        TIME,
        TOWER,
        TRAINING,
        TRAP,
        TREASURE,
        UNITY,
        UNLOCK,
        VALOR,
        VELOCITY,
        VERSE,
        VITALITY,
        VOID,
        VORTEX,
        VOW,
        VOYAGE,
        WAGE,
        WINCH,
        WIND,
        WISH;

        /// Oracle-text spelling as a `Words.phrase` template — Title
        /// case on the leading word so `phrase()` matches both
        /// mid-sentence ("put a lore counter on") and sentence-start
        /// ("Lore counters can't be added to permanents").
        public String text() {
            return titleCase(name());
        }
    }

    /// "a counter" / "N counters" with no named type — e.g., O'aka,
    /// Traveling Merchant ("Remove a counter from a nonland permanent
    /// you control"). Resolved at resolution: the chooser picks a
    /// counter among any the target carries.
    enum Any implements CounterType {
        ANY
    }

    /// Creates a [PtCounter] counter type.
    static CounterType ptCounter(int power, int toughness) {
        return new PtCounter(power, toughness);
    }

    /// Every named counter type — keyword counters ({@mtg.rule 122.1b})
    /// and [Named] — keyed by oracle-text spelling ([Keyword#text] /
    /// [Named#text], Title case on the leading word). Consumed by the
    /// parser to build its `phrase()` alternatives.
    Map<String, CounterType> BY_TEXT = Stream.concat(
                    Stream.of(Keyword.values()).map(k -> Map.<String, CounterType>entry(k.text(), k)),
                    Stream.of(Named.values()).map(n -> Map.<String, CounterType>entry(n.text(), n)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
}
