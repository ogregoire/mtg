package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SplitterTest {

    @Nested
    class OnString {

        @Test
        void splitsByLiteralString() {
            assertThat(Splitter.on(",").split("a,b,c")).containsExactly("a", "b", "c");
        }

        @Test
        void preservesWhitespace() {
            assertThat(Splitter.on(",").split("a, b , c")).containsExactly("a", " b ", " c");
        }

        @Test
        void splitsByMultiCharacterSeparator() {
            assertThat(Splitter.on("::").split("a::b::c")).containsExactly("a", "b", "c");
        }

        @Test
        void singleElementNoSeparator() {
            assertThat(Splitter.on(",").split("hello")).containsExactly("hello");
        }

        @Test
        void emptyPartsPreserved() {
            assertThat(Splitter.on(",").split(",a,,b,")).containsExactly("", "a", "", "b", "");
        }

        @Test
        void emptyInput() {
            assertThat(Splitter.on(",").split("")).containsExactly("");
        }

        @Test
        void regexMetaCharactersAreLiteral() {
            assertThat(Splitter.on(".").split("a.b.c")).containsExactly("a", "b", "c");
        }

        @Test
        void emptySeparatorThrows() {
            assertThatThrownBy(() -> Splitter.on(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("separator cannot be empty");
        }
    }

    @Nested
    class OnChar {

        @Test
        void splitsByCharacter() {
            assertThat(Splitter.on(',').split("x,y,z")).containsExactly("x", "y", "z");
        }

        @Test
        void regexMetaCharIsLiteral() {
            assertThat(Splitter.on('|').split("a|b|c")).containsExactly("a", "b", "c");
        }
    }

    @Nested
    class OnPattern {

        @Test
        void splitsByRegexString() {
            assertThat(Splitter.onPattern("\\s+").split("one two  three")).containsExactly("one", "two", "three");
        }

        @Test
        void splitsByCompiledPattern() {
            var pattern = Pattern.compile("[,;]");
            assertThat(Splitter.onPattern(pattern).split("a,b;c")).containsExactly("a", "b", "c");
        }

        @Test
        void splitsByDigits() {
            assertThat(Splitter.onPattern("\\d+").split("a1b22c")).containsExactly("a", "b", "c");
        }
    }

    @Nested
    class TrimResults {

        @Test
        void trimsWhitespace() {
            assertThat(Splitter.on(",").trimResults().split(" a , b , c ")).containsExactly("a", "b", "c");
        }

        @Test
        void trimsTabsAndNewlines() {
            assertThat(Splitter.on(",").trimResults().split("\ta\t,\nb\n")).containsExactly("a", "b");
        }

        @Test
        void doesNotAffectOriginalSplitter() {
            var splitter = Splitter.on(",");
            splitter.trimResults();
            assertThat(splitter.split(" a , b ")).containsExactly(" a ", " b ");
        }

        @Test
        void emptyAfterTrim() {
            assertThat(Splitter.on(",").trimResults().split(" , , ")).containsExactly("", "", "");
        }

        @Test
        void combinedWithPattern() {
            assertThat(Splitter.onPattern("[;,]").trimResults().split("a ; b , c"))
                    .containsExactly("a", "b", "c");
        }
    }

    @Nested
    class OmitEmptyStrings {

        @Test
        void omitsEmptyParts() {
            assertThat(Splitter.on(",").omitEmptyStrings().split("a,,b,,c")).containsExactly("a", "b", "c");
        }

        @Test
        void omitsLeadingAndTrailingEmpty() {
            assertThat(Splitter.on(",").omitEmptyStrings().split(",a,b,")).containsExactly("a", "b");
        }

        @Test
        void allEmptyReturnsNothing() {
            assertThat(Splitter.on(",").omitEmptyStrings().split(",,")).isEmpty();
        }

        @Test
        void emptyInputReturnsNothing() {
            assertThat(Splitter.on(",").omitEmptyStrings().split("")).isEmpty();
        }

        @Test
        void noEmptyPartsUnchanged() {
            assertThat(Splitter.on(",").omitEmptyStrings().split("a,b,c")).containsExactly("a", "b", "c");
        }

        @Test
        void combinedWithTrim() {
            assertThat(Splitter.on(",").trimResults().omitEmptyStrings().split(" a , , b , "))
                    .containsExactly("a", "b");
        }

        @Test
        void doesNotAffectOriginalSplitter() {
            var splitter = Splitter.on(",");
            splitter.omitEmptyStrings();
            assertThat(splitter.split("a,,b")).containsExactly("a", "", "b");
        }
    }

    @Nested
    class Limit {

        @Test
        void limitsToTwoParts() {
            assertThat(Splitter.on(",").limit(2).split("a,b,c")).containsExactly("a", "b,c");
        }

        @Test
        void limitsToThreeParts() {
            assertThat(Splitter.on(",").limit(3).split("a,b,c,d")).containsExactly("a", "b", "c,d");
        }

        @Test
        void limitOneMeansNoSplit() {
            assertThat(Splitter.on(",").limit(1).split("a,b,c")).containsExactly("a,b,c");
        }

        @Test
        void limitExceedsPartsReturnsAll() {
            assertThat(Splitter.on(",").limit(10).split("a,b")).containsExactly("a", "b");
        }

        @Test
        void limitWithPattern() {
            assertThat(Splitter.onPattern("\\s+").limit(2).split("a b c")).containsExactly("a", "b c");
        }

        @Test
        void limitWithTrim() {
            assertThat(Splitter.on(",").trimResults().limit(2).split(" a , b , c "))
                    .containsExactly("a", "b , c");
        }

        @Test
        void limitWithOmitEmpty() {
            assertThat(Splitter.on(",").omitEmptyStrings().limit(2).split(",a,,b,c"))
                    .containsExactly("a", ",b,c");
        }

        @Test
        void zeroLimitThrows() {
            assertThatThrownBy(() -> Splitter.on(",").limit(0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void negativeLimitThrows() {
            assertThatThrownBy(() -> Splitter.on(",").limit(-1)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class StreamBehavior {

        @Test
        void returnsStream() {
            var count = Splitter.on(",").split("a,b,c").count();
            assertThat(count).isEqualTo(3);
        }

        @Test
        void supportsFiltering() {
            var result =
                    Splitter.on(",").split("a,,b,,c").filter(s -> !s.isEmpty()).toList();
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void supportsMapping() {
            var result =
                    Splitter.on(",").split("a,b,c").map(String::toUpperCase).toList();
            assertThat(result).containsExactly("A", "B", "C");
        }
    }
}
