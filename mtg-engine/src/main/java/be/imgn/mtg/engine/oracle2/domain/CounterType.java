package be.imgn.mtg.engine.oracle2.domain;

import java.util.Locale;

/// A counter type ({@mtg.rule 122}). Splits into:
///
/// - [PtCounter] — power/toughness counters: `+1/+1`, `-1/-1`,
///   `+0/+1`, etc.
/// - [Keyword] — the keyword counters of {@mtg.rule 122.1b}; a
///   permanent bearing one gains the named keyword ability (flying,
///   first strike, deathtouch, …).
/// - [Named] — every non-keyword named counter type: rule-defined
///   special-rule counters ({@mtg.rule 122.1c-i}: loyalty, poison,
///   defense, shield, stun, finality, rad) plus mechanic- and
///   card-specific counters that appear in vintage-legal oracle
///   text. The enum values are uppercased forms of the oracle-text
///   spelling.
/// - [Any#ANY] — "a counter" with no named type (O'aka, Traveling
///   Merchant: "Remove a counter from a nonland permanent you
///   control"). The chooser picks a counter type at resolution time.
public sealed interface CounterType {

    /// Power/toughness counter — `+N/+M`, `-N/-M`, `+N/-M`, etc.
    record PtCounter(int power, int toughness) implements CounterType {}

    /// The 15 keyword counters of {@mtg.rule 122.1b}. A permanent
    /// bearing one gains the wrapped keyword ability.
    enum Keyword implements CounterType, Parseable {
        FLYING,
        FIRST_STRIKE,
        DOUBLE_STRIKE,
        DEATHTOUCH,
        DECAYED,
        EXALTED,
        HASTE,
        HEXPROOF,
        INDESTRUCTIBLE,
        LIFELINK,
        MENACE,
        REACH,
        SHADOW,
        TRAMPLE,
        VIGILANCE;

        @Override
        public String text() {
            return titleCase(name());
        }
    }

    /// Every non-keyword counter type that appears on a vintage-legal
    /// card, plus the rule-defined special-rule counters
    /// ({@mtg.rule 122.1c-i}). The enum name uppercases the oracle-text
    /// spelling. This is a closed set — oracle text naming a counter
    /// not in this enum (and not a [Keyword]) should be treated as a
    /// parse failure, not silently accepted.
    enum Named implements CounterType, Parseable {
        // Rule 122.1c-i — counters with special rule semantics.
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

        @Override
        public String text() {
            return titleCase(name());
        }
    }

    /// "a counter" / "N counters" with no named type. Resolved at
    /// resolution time — the chooser picks a counter type among any
    /// the target carries.
    enum Any implements CounterType {
        ANY
    }

    /// Title-cased oracle spelling for an enum's `SCREAMING_SNAKE`
    /// name: `FIRST_STRIKE` → `"First strike"`. The leading uppercase
    /// lets `Parsers.phrase()` treat the first token as title-or-
    /// lower so it matches both mid-sentence and the rare sentence-
    /// start occurrence.
    private static String titleCase(String enumName) {
        var lower = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
