package be.imgn.mtg.tools.javadoc;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;

import jdk.javadoc.doclet.Taglet;

/**
 * Custom Javadoc taglet that renders {@code {@mtg.rule 202.3a}} as a hyperlink to Yawgatog's MTG rules.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * /// Returns the mana value ({@mtg.rule 202.3a}).
 * Value manaValue();
 * }</pre>
 *
 * <h2>Supported formats</h2>
 *
 * <ul>
 *   <li>Single rule: {@code {@mtg.rule 202.3a}}
 *   <li>Rule range: {@code {@mtg.rule 613.1-613.7}}
 *   <li>Section: {@code {@mtg.rule 704}}
 * </ul>
 */
public class MtgRuleTaglet implements Taglet {

    private static final String TAG_NAME = "mtg.rule";
    private static final String YAWGATOG_BASE_URL = "https://yawgatog.com/resources/magic-rules/#R";

    // Pattern to match rule references: section (e.g., 704), rule (e.g., 704.5), or subrule (e.g., 704.5a)
    private static final Pattern RULE_PATTERN =
            Pattern.compile("^(\\d+(?:\\.\\d+[a-km-np-z]?)?)(?:-(\\d+(?:\\.\\d+[a-km-np-z]?)?))?$");

    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(Location.class);
    }

    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags.isEmpty()) {
            return "";
        }

        var tag = tags.getFirst();
        if (!(tag instanceof UnknownInlineTagTree inlineTag)) {
            return "";
        }

        var content = extractContent(inlineTag);
        if (content.isEmpty()) {
            return "<code>mtg.rule: missing rule reference</code>";
        }

        var rule = content.trim();
        Matcher matcher = RULE_PATTERN.matcher(rule);

        if (!matcher.matches()) {
            return "<code>" + escapeHtml(rule) + "</code>";
        }

        var startRule = matcher.group(1);
        var endRule = matcher.group(2);

        if (endRule != null) {
            // Range format: link to start rule, display full range
            return String.format(
                    "<a href=\"%s%s\">rule %s</a>", YAWGATOG_BASE_URL, formatRuleForUrl(startRule), escapeHtml(rule));
        } else {
            // Single rule
            return String.format(
                    "<a href=\"%s%s\">rule %s</a>", YAWGATOG_BASE_URL, formatRuleForUrl(startRule), escapeHtml(rule));
        }
    }

    private String extractContent(UnknownInlineTagTree inlineTag) {
        var sb = new StringBuilder();
        for (DocTree content : inlineTag.getContent()) {
            if (content instanceof TextTree textTree) {
                sb.append(textTree.getBody());
            }
        }
        return sb.toString();
    }

    private String formatRuleForUrl(String rule) {
        // Yawgatog URLs use the rule number directly (e.g., R202.3a)
        return rule;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
