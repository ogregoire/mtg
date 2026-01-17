package be.imgn.mtg.javadoc;

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
 * Custom Javadoc taglet that renders {@code {@mtg.rule 202.3a}} as a hyperlink to the project's rules page.
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
 *   <li>Single rule: {@code {@mtg.rule 202.3a}} → links to {@code /mtg/rules/#rule-202.3a}
 *   <li>Rule range: {@code {@mtg.rule 613.1-613.7}} → links to {@code /mtg/rules/#rule-613.1}
 *   <li>Section: {@code {@mtg.rule 704}} → links to {@code /mtg/rules/#section-704}
 *   <li>Chapter: {@code {@mtg.rule 1}} → links to {@code /mtg/rules/#chapter-1}
 * </ul>
 */
public class MtgRuleTaglet implements Taglet {

    private static final String TAG_NAME = "mtg.rule";
    private static final String RULES_BASE_URL = "/mtg/rules/#";

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
                    "<a href=\"%s%s\">rule %s</a>", RULES_BASE_URL, formatRuleForAnchor(startRule), escapeHtml(rule));
        } else {
            // Single rule
            return String.format(
                    "<a href=\"%s%s\">rule %s</a>", RULES_BASE_URL, formatRuleForAnchor(startRule), escapeHtml(rule));
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

    private String formatRuleForAnchor(String rule) {
        // Anchors use different prefixes based on the rule type:
        // - Single digit (1-9): chapter-{number}
        // - Three digits (100-999): section-{number}
        // - With decimal (100.1, 100.1a): rule-{number}
        if (rule.contains(".")) {
            return "rule-" + rule;
        } else if (rule.length() == 1) {
            return "chapter-" + rule;
        } else {
            return "section-" + rule;
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
