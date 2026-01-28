package be.imgn.mtg.rules.maven;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates HTML from parsed MTG Comprehensive Rules. */
public final class HtmlGenerator {

    private static final Pattern RULE_NUMBER_PATTERN = Pattern.compile("^(\\d{3}\\.\\d+[a-z]?)");
    private static final Pattern RULE_SPLIT_PATTERN = Pattern.compile("^(\\d{3}\\.\\d+[a-z]?\\.?)\\s*(.*)$");
    private static final Pattern SUBRULE_PATTERN = Pattern.compile("^\\d{3}\\.\\d+[a-z]");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b((?:https?://)?(?:www\\.)?[a-zA-Z0-9][-a-zA-Z0-9]*(?:\\.[a-zA-Z]{2,})+(?:/[^\\s<>.,;:!?)\\]]*)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RULE_REF_PATTERN =
            Pattern.compile("\\b(rules?|section)\\s+(\\d{3}\\.\\d+[a-z]?|\\d{3}|\\d)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAPTER_NUM_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.+)");
    private static final Pattern SECTION_NUM_PATTERN = Pattern.compile("^(\\d{3})\\.\\s+(.+)");
    private static final Pattern SPECIFIC_RULE_PATTERN = Pattern.compile("^\\d{3}\\.\\d");
    private static final Pattern SECTION_REF_PATTERN = Pattern.compile("^\\d{3}$");
    private static final Pattern CHAPTER_REF_PATTERN = Pattern.compile("^\\d$");

    private static final String HTML_HEAD = loadResource("html-head.html");
    private static final String JAVASCRIPT_CODE = loadResource("javascript.js");

    private static String loadResource(String name) {
        try (var is = HtmlGenerator.class.getResourceAsStream(name)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + name);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load resource: " + name, e);
        }
    }

    public String generate(RulesDocument doc) {
        var sb = new StringBuilder();

        sb.append(HTML_HEAD);

        // Table of Contents
        for (var entry : doc.toc()) {
            switch (entry.type()) {
                case CHAPTER -> {
                    var matcher = CHAPTER_NUM_PATTERN.matcher(entry.text());
                    if (matcher.find()) {
                        sb.append("                <div class=\"toc-chapter\"><a href=\"#chapter-")
                                .append(matcher.group(1))
                                .append("\">")
                                .append(escapeHtml(entry.text()))
                                .append("</a></div>\n");
                    }
                }
                case SECTION -> {
                    var matcher = SECTION_NUM_PATTERN.matcher(entry.text());
                    if (matcher.find()) {
                        sb.append("                <div class=\"toc-section\"><a href=\"#section-")
                                .append(matcher.group(1))
                                .append("\">")
                                .append(escapeHtml(entry.text()))
                                .append("</a></div>\n");
                    }
                }
                case GLOSSARY ->
                    sb.append("                <div class=\"toc-special\"><a href=\"#glossary\">Glossary</a></div>\n");
                case CREDITS ->
                    sb.append("                <div class=\"toc-special\"><a href=\"#credits\">Credits</a></div>\n");
            }
        }

        sb.append("            </nav>\n        </aside>\n\n        <main class=\"main-content\">\n");

        // Header
        sb.append("            <header id=\"top\">\n")
                .append("                <h1>")
                .append(escapeHtml(doc.title()))
                .append("</h1>\n")
                .append("                <p class=\"effective-date\">Effective: ")
                .append(escapeHtml(doc.effectiveDate()))
                .append("</p>\n")
                .append("            </header>\n");

        // Introduction
        sb.append("            <section class=\"introduction\">\n");
        for (var para : doc.introduction()) {
            sb.append("                <p>").append(linkifyText(para)).append("</p>\n");
        }
        sb.append("            </section>\n");

        // Chapters and Rules
        for (var chapter : doc.chapters()) {
            sb.append("\n            <section class=\"chapter\" id=\"chapter-")
                    .append(chapter.number())
                    .append("\">\n");
            sb.append("                <h2>")
                    .append(chapter.number())
                    .append(". ")
                    .append(escapeHtml(chapter.title()))
                    .append("</h2>\n");

            for (var section : chapter.sections()) {
                sb.append("\n                <section class=\"section\" id=\"section-")
                        .append(section.number())
                        .append("\">\n");
                sb.append("                    <h3>")
                        .append(section.number())
                        .append(". ")
                        .append(escapeHtml(section.title()))
                        .append("</h3>\n");

                for (var ruleText : section.rules()) {
                    var ruleNum = extractRuleNumber(ruleText);
                    var ruleId = makeRuleId(ruleNum);
                    var isSubrule = SUBRULE_PATTERN.matcher(ruleText).find();
                    var cssClass = isSubrule ? "rule subrule" : "rule";

                    var splitMatcher = RULE_SPLIT_PATTERN.matcher(ruleText);
                    if (splitMatcher.find()) {
                        var numPart = splitMatcher.group(1);
                        var contentPart = splitMatcher.group(2);
                        sb.append("                    <div class=\"")
                                .append(cssClass)
                                .append("\" id=\"")
                                .append(ruleId)
                                .append("\">");
                        sb.append("<span class=\"rule-number\">")
                                .append(escapeHtml(numPart))
                                .append("</span> ");
                        sb.append(linkifyText(contentPart));
                        sb.append("</div>\n");
                    } else {
                        sb.append("                    <div class=\"")
                                .append(cssClass)
                                .append("\" id=\"")
                                .append(ruleId)
                                .append("\">");
                        sb.append(linkifyText(ruleText));
                        sb.append("</div>\n");
                    }
                }
                sb.append("                </section>\n");
            }
            sb.append("            </section>\n");
        }

        // Glossary
        sb.append("\n            <section class=\"glossary-section\" id=\"glossary\">\n");
        sb.append("                <h2>Glossary</h2>\n");

        var sortedTerms = doc.glossary().keySet().stream()
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();

        for (var term : sortedTerms) {
            var termId = term.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-zA-Z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            var definition = doc.glossary().get(term);
            if (definition == null) {
                continue;
            }

            sb.append("                <div class=\"glossary-entry\" id=\"glossary-")
                    .append(termId)
                    .append("\">\n");
            sb.append("                    <span class=\"glossary-term-title\">")
                    .append(escapeHtml(term))
                    .append("</span><br>\n");
            sb.append("                    ").append(linkifyText(definition)).append("\n");
            sb.append("                </div>\n");
        }
        sb.append("            </section>\n");

        // Credits
        sb.append("\n            <section class=\"credits\" id=\"credits\">\n");
        sb.append("                <h2>Credits</h2>\n");

        var creditsText = String.join("\n", doc.credits()).strip();
        for (var para : creditsText.split("\n\n", -1)) {
            if (!para.strip().isEmpty()) {
                sb.append("                <p>")
                        .append(escapeHtml(para.strip()))
                        .append("</p>\n");
            }
        }
        sb.append("            </section>\n");
        sb.append("        </main>\n");
        sb.append("    </div>\n");

        // JavaScript
        sb.append(generateJavaScript(doc));
        sb.append("</body>\n</html>\n");

        return sb.toString();
    }

    private String extractRuleNumber(String ruleText) {
        var matcher = RULE_NUMBER_PATTERN.matcher(ruleText);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String makeRuleId(String ruleNumber) {
        return "rule-" + ruleNumber.replaceAll("\\.$", "");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String linkifyUrls(String text) {
        var matcher = URL_PATTERN.matcher(text);
        var result = new StringBuilder();
        while (matcher.find()) {
            var url = matcher.group(1);
            var href = url.startsWith("http") ? url : "https://" + url;
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            "<a href=\"" + href + "\" target=\"_blank\" rel=\"noopener\">" + url + "</a>"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String linkifyText(String text) {
        var escaped = escapeHtml(text);
        escaped = linkifyUrls(escaped);

        var matcher = RULE_REF_PATTERN.matcher(escaped);
        var result = new StringBuilder();
        while (matcher.find()) {
            var prefix = matcher.group(1);
            var number = matcher.group(2);

            String anchor;
            if (SPECIFIC_RULE_PATTERN.matcher(number).find()) {
                anchor = "rule-" + number;
            } else if (SECTION_REF_PATTERN.matcher(number).matches()) {
                anchor = "section-" + number;
            } else if (CHAPTER_REF_PATTERN.matcher(number).matches()) {
                anchor = "chapter-" + number;
            } else {
                anchor = "rule-" + number;
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            prefix + " <a href=\"#" + anchor + "\" class=\"rule-ref\">" + number + "</a>"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String generateJavaScript(RulesDocument doc) {
        var sb = new StringBuilder();
        sb.append("\n    <script>\n");
        sb.append("        // Glossary data\n");
        sb.append("        const glossary = ");
        sb.append(toJson(doc.glossary()));
        sb.append(";\n");
        sb.append(JAVASCRIPT_CODE);
        sb.append("    </script>\n");
        return sb.toString();
    }

    private String toJson(Map<String, String> map) {
        var sb = new StringBuilder();
        sb.append("{");
        var first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\n            ");
            sb.append(jsonString(entry.getKey()));
            sb.append(": ");
            sb.append(jsonString(entry.getValue()));
        }
        sb.append("\n        }");
        return sb.toString();
    }

    private String jsonString(String s) {
        var sb = new StringBuilder();
        sb.append("\"");
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
