package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import be.imgn.mtg.engine.oracle2.domain.Ability;
import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.Cost;

class KeywordAbilityParserTest {

    private static Ability parse(String input) {
        return KeywordAbilityParser.KEYWORD.parseSkipping(CharPredicate.is(' '), input);
    }

    private static List<Ability> parseList(String input) {
        return KeywordAbilityParser.KEYWORD_LIST.parseSkipping(CharPredicate.is(' '), input);
    }

    /// All no-param keywords with their canonical sentence-start
    /// printed form. The table mirrors the order in
    /// [be.imgn.mtg.engine.oracle2.parser.selector.AbilitySelectorParser#ABILITY_KEYWORD].
    static Stream<Arguments> noParamKeywords() {
        return Stream.of(
                Arguments.of("Aftermath", Ability.StaticKeyword.AFTERMATH),
                Arguments.of("Ascend", Ability.StaticKeyword.ASCEND),
                Arguments.of("Assist", Ability.StaticKeyword.ASSIST),
                Arguments.of("Banding", Ability.StaticKeyword.BANDING),
                Arguments.of("Battle cry", Ability.TriggeredKeyword.BATTLE_CRY),
                Arguments.of("Cascade", Ability.TriggeredKeyword.CASCADE),
                Arguments.of("Changeling", Ability.StaticKeyword.CHANGELING),
                Arguments.of("Compleated", Ability.StaticKeyword.COMPLEATED),
                Arguments.of("Convoke", Ability.StaticKeyword.CONVOKE),
                Arguments.of("Daybound", Ability.TriggeredKeyword.DAYBOUND),
                Arguments.of("Deathtouch", Ability.StaticKeyword.DEATHTOUCH),
                Arguments.of("Decayed", Ability.StaticKeyword.DECAYED),
                Arguments.of("Defender", Ability.StaticKeyword.DEFENDER),
                Arguments.of("Delve", Ability.StaticKeyword.DELVE),
                Arguments.of("Demonstrate", Ability.TriggeredKeyword.DEMONSTRATE),
                Arguments.of("Dethrone", Ability.TriggeredKeyword.DETHRONE),
                Arguments.of("Devoid", Ability.StaticKeyword.DEVOID),
                Arguments.of("Double strike", Ability.StaticKeyword.DOUBLE_STRIKE),
                Arguments.of("Epic", Ability.StaticKeyword.EPIC),
                Arguments.of("Evolve", Ability.TriggeredKeyword.EVOLVE),
                Arguments.of("Exalted", Ability.TriggeredKeyword.EXALTED),
                Arguments.of("Extort", Ability.TriggeredKeyword.EXTORT),
                Arguments.of("Fear", Ability.StaticKeyword.FEAR),
                Arguments.of("First strike", Ability.StaticKeyword.FIRST_STRIKE),
                Arguments.of("Flanking", Ability.TriggeredKeyword.FLANKING),
                Arguments.of("Flash", Ability.StaticKeyword.FLASH),
                Arguments.of("Flying", Ability.StaticKeyword.FLYING),
                Arguments.of("For Mirrodin!", Ability.StaticKeyword.FOR_MIRRODIN),
                Arguments.of("Fuse", Ability.StaticKeyword.FUSE),
                Arguments.of("Gravestorm", Ability.TriggeredKeyword.GRAVESTORM),
                Arguments.of("Haste", Ability.StaticKeyword.HASTE),
                Arguments.of("Haunt", Ability.TriggeredKeyword.HAUNT),
                Arguments.of("Hexproof", Ability.StaticKeyword.HEXPROOF),
                Arguments.of("Hidden agenda", Ability.StaticKeyword.HIDDEN_AGENDA),
                Arguments.of("Horsemanship", Ability.StaticKeyword.HORSEMANSHIP),
                Arguments.of("Indestructible", Ability.StaticKeyword.INDESTRUCTIBLE),
                Arguments.of("Infect", Ability.StaticKeyword.INFECT),
                Arguments.of("Ingest", Ability.TriggeredKeyword.INGEST),
                Arguments.of("Intimidate", Ability.StaticKeyword.INTIMIDATE),
                Arguments.of("Lifelink", Ability.StaticKeyword.LIFELINK),
                Arguments.of("Living metal", Ability.StaticKeyword.LIVING_METAL),
                Arguments.of("Living weapon", Ability.TriggeredKeyword.LIVING_WEAPON),
                Arguments.of("Melee", Ability.TriggeredKeyword.MELEE),
                Arguments.of("Menace", Ability.StaticKeyword.MENACE),
                Arguments.of("Mentor", Ability.TriggeredKeyword.MENTOR),
                Arguments.of("Myriad", Ability.TriggeredKeyword.MYRIAD),
                Arguments.of("Nightbound", Ability.TriggeredKeyword.NIGHTBOUND),
                Arguments.of("Partner", Ability.StaticKeyword.PARTNER),
                Arguments.of("Persist", Ability.TriggeredKeyword.PERSIST),
                Arguments.of("Phasing", Ability.StaticKeyword.PHASING),
                Arguments.of("Prowess", Ability.TriggeredKeyword.PROWESS),
                Arguments.of("Reach", Ability.StaticKeyword.REACH),
                Arguments.of("Read ahead", Ability.StaticKeyword.READ_AHEAD),
                Arguments.of("Retrace", Ability.StaticKeyword.RETRACE),
                Arguments.of("Riot", Ability.StaticKeyword.RIOT),
                Arguments.of("Shadow", Ability.StaticKeyword.SHADOW),
                Arguments.of("Shroud", Ability.StaticKeyword.SHROUD),
                Arguments.of("Skulk", Ability.StaticKeyword.SKULK),
                Arguments.of("Solved", Ability.StaticKeyword.SOLVED),
                Arguments.of("Soulbond", Ability.TriggeredKeyword.SOULBOND),
                Arguments.of("Split second", Ability.StaticKeyword.SPLIT_SECOND),
                Arguments.of("Storm", Ability.TriggeredKeyword.STORM),
                Arguments.of("Sunburst", Ability.StaticKeyword.SUNBURST),
                Arguments.of("Training", Ability.TriggeredKeyword.TRAINING),
                Arguments.of("Trample", Ability.StaticKeyword.TRAMPLE),
                Arguments.of("Umbra armor", Ability.StaticKeyword.UMBRA_ARMOR),
                Arguments.of("Undaunted", Ability.StaticKeyword.UNDAUNTED),
                Arguments.of("Undying", Ability.TriggeredKeyword.UNDYING),
                Arguments.of("Vigilance", Ability.StaticKeyword.VIGILANCE),
                Arguments.of("Visit", Ability.TriggeredKeyword.VISIT),
                Arguments.of("Wither", Ability.StaticKeyword.WITHER));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("noParamKeywords")
    void noParamSentenceStart(String input, Ability expected) {
        assertThat(parse(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] lowercase {0} → {1}")
    @MethodSource("noParamKeywords")
    void noParamMidSentence(String input, Ability expected) {
        // Lowercase first letter — "flying", "double strike" — is the
        // form keyword takes in a comma-list after the first slot
        // ("Flying, vigilance"). Phrase title-or-lower must accept it.
        String lowered = Character.toLowerCase(input.charAt(0)) + input.substring(1);
        assertThat(parse(lowered)).isEqualTo(expected);
    }

    @Nested
    class Parametrised {
        @Test
        void equipMana() {
            assertThat(parse("Equip {2}")).isEqualTo(new Ability.Equip(new Cost.ManaCost("{2}")));
        }

        @Test
        void cyclingMana() {
            assertThat(parse("Cycling {1}{R}")).isEqualTo(new Ability.Cycling(new Cost.ManaCost("{1}{R}")));
        }

        @Test
        void outlastMana() {
            assertThat(parse("Outlast {1}{B}")).isEqualTo(new Ability.Outlast(new Cost.ManaCost("{1}{B}")));
        }

        @Test
        void encoreMana() {
            assertThat(parse("Encore {3}{B}{R}")).isEqualTo(new Ability.Encore(new Cost.ManaCost("{3}{B}{R}")));
        }

        @Test
        void wardMana() {
            assertThat(parse("Ward {2}")).isEqualTo(new Ability.Ward(new Cost.ManaCost("{2}")));
        }

        @Test
        void crew() {
            assertThat(parse("Crew 2")).isEqualTo(new Ability.Crew(new Amount.Exact(2)));
        }

        @Test
        void reinforce() {
            assertThat(parse("Reinforce 2—{2}{W}")).isEqualTo(new Ability.Reinforce(2, new Cost.ManaCost("{2}{W}")));
        }

        @Test
        void toxic() {
            assertThat(parse("Toxic 1")).isEqualTo(new Ability.Toxic(1));
        }

        @Test
        void support() {
            assertThat(parse("Support 2")).isEqualTo(new Ability.Support(2));
        }

        @Test
        void afflict() {
            assertThat(parse("Afflict 1")).isEqualTo(new Ability.Afflict(1));
        }

        @Test
        void bushido() {
            assertThat(parse("Bushido 1")).isEqualTo(new Ability.Bushido(1));
        }

        @Test
        void afterlife() {
            assertThat(parse("Afterlife 2")).isEqualTo(new Ability.Afterlife(2));
        }

        @Test
        void firebending() {
            assertThat(parse("Firebending 3")).isEqualTo(new Ability.Firebending(3));
        }
    }

    @Nested
    class KeywordList {
        @Test
        void singletonStaticKeyword() {
            assertThat(parseList("Flying")).containsExactly(Ability.StaticKeyword.FLYING);
        }

        @Test
        void twoKeywordsCommaSeparated() {
            assertThat(parseList("Flying, vigilance"))
                    .containsExactly(Ability.StaticKeyword.FLYING, Ability.StaticKeyword.VIGILANCE);
        }

        @Test
        void threeKeywordsCommaSeparated() {
            assertThat(parseList("Flying, trample, haste"))
                    .containsExactly(
                            Ability.StaticKeyword.FLYING, Ability.StaticKeyword.TRAMPLE, Ability.StaticKeyword.HASTE);
        }

        @Test
        void mixedStaticAndTriggered() {
            assertThat(parseList("Prowess, lifelink"))
                    .containsExactly(Ability.TriggeredKeyword.PROWESS, Ability.StaticKeyword.LIFELINK);
        }

        @Test
        void parametrizedAlongsideNoParam() {
            assertThat(parseList("Flying, ward {2}"))
                    .containsExactly(Ability.StaticKeyword.FLYING, new Ability.Ward(new Cost.ManaCost("{2}")));
        }
    }
}
