package be.imgn.mtg.rules.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Parser for MTG Comprehensive Rules text files. */
public final class RulesParser {

    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile("effective as of (.+)\\.");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^([1-9])\\.\\s+(.+)");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\d{3})\\.\\s+([A-Z].*)");
    private static final Pattern RULE_PATTERN = Pattern.compile("^(\\d{3}\\.\\d+)");
    private static final Pattern TOC_CHAPTER_PATTERN = Pattern.compile("^[1-9]\\.\\s+\\w");
    private static final Pattern TOC_SECTION_PATTERN = Pattern.compile("^\\d{3}\\.\\s+\\w");
    private static final Pattern ANY_NUMBERED_PATTERN = Pattern.compile("^\\d+\\.\\s+\\w");

    private enum State {
        TITLE,
        EFFECTIVE_DATE,
        INTRODUCTION,
        TOC,
        RULES_START,
        RULES,
        GLOSSARY,
        CREDITS
    }

    public RulesDocument parse(String text) {
        var lines = text.split("\n", -1);
        var doc = new RulesDocument();

        var state = State.TITLE;
        RulesDocument.Chapter currentChapter = null;
        RulesDocument.Section currentSection = null;
        String currentGlossaryTerm = null;
        List<String> currentGlossaryDef = new ArrayList<>();

        var i = 0;
        while (i < lines.length) {
            var line = lines[i];
            var stripped = line.strip();

            switch (state) {
                case TITLE -> {
                    if (!stripped.isEmpty()) {
                        doc.setTitle(stripped);
                        state = State.EFFECTIVE_DATE;
                    }
                }
                case EFFECTIVE_DATE -> {
                    if (line.startsWith("These rules are effective as of")) {
                        var matcher = EFFECTIVE_DATE_PATTERN.matcher(line);
                        if (matcher.find()) {
                            doc.setEffectiveDate(matcher.group(1));
                        }
                        state = State.INTRODUCTION;
                    }
                }
                case INTRODUCTION -> {
                    if (stripped.equals("Contents")) {
                        state = State.TOC;
                    } else if (!stripped.isEmpty()) {
                        doc.introduction().add(stripped);
                    }
                }
                case TOC -> {
                    if (stripped.isEmpty()) {
                        // Skip empty lines
                    } else if (TOC_SECTION_PATTERN.matcher(stripped).find()) {
                        doc.toc().add(new RulesDocument.TocEntry(RulesDocument.TocType.SECTION, stripped));
                    } else if (TOC_CHAPTER_PATTERN.matcher(stripped).find()) {
                        doc.toc().add(new RulesDocument.TocEntry(RulesDocument.TocType.CHAPTER, stripped));
                    } else if (stripped.equals("Glossary")) {
                        doc.toc().add(new RulesDocument.TocEntry(RulesDocument.TocType.GLOSSARY, "Glossary"));
                    } else if (stripped.equals("Credits")) {
                        doc.toc().add(new RulesDocument.TocEntry(RulesDocument.TocType.CREDITS, "Credits"));
                        state = State.RULES_START;
                    }
                }
                case RULES_START -> {
                    if (ANY_NUMBERED_PATTERN.matcher(stripped).find()) {
                        state = State.RULES;
                        i--; // Re-process this line
                    }
                }
                case RULES -> {
                    if (stripped.equals("Glossary")) {
                        state = State.GLOSSARY;
                    } else {
                        var chapterMatcher = CHAPTER_PATTERN.matcher(stripped);
                        var sectionMatcher = SECTION_PATTERN.matcher(stripped);
                        var ruleMatcher = RULE_PATTERN.matcher(stripped);

                        if (chapterMatcher.find()) {
                            // Save current section/chapter
                            if (currentSection != null && currentChapter != null) {
                                currentChapter.sections().add(currentSection);
                            }
                            if (currentChapter != null) {
                                doc.chapters().add(currentChapter);
                            }
                            currentChapter =
                                    new RulesDocument.Chapter(chapterMatcher.group(1), chapterMatcher.group(2));
                            currentSection = null;
                        } else if (sectionMatcher.find()) {
                            if (currentSection != null && currentChapter != null) {
                                currentChapter.sections().add(currentSection);
                            }
                            currentSection =
                                    new RulesDocument.Section(sectionMatcher.group(1), sectionMatcher.group(2));
                        } else if (ruleMatcher.find()) {
                            if (currentSection != null) {
                                currentSection.rules().add(stripped);
                            }
                        } else if (!stripped.isEmpty()
                                && currentSection != null
                                && !currentSection.rules().isEmpty()) {
                            // Continuation of previous rule
                            var lastIdx = currentSection.rules().size() - 1;
                            var lastRule = currentSection.rules().get(lastIdx);
                            currentSection.rules().set(lastIdx, lastRule + " " + stripped);
                        }
                    }
                }
                case GLOSSARY -> {
                    if (stripped.equals("Credits")) {
                        if (currentGlossaryTerm != null && !currentGlossaryDef.isEmpty()) {
                            doc.glossary().put(currentGlossaryTerm, String.join(" ", currentGlossaryDef));
                        }
                        state = State.CREDITS;
                    } else if (stripped.isEmpty()) {
                        if (currentGlossaryTerm != null && !currentGlossaryDef.isEmpty()) {
                            doc.glossary().put(currentGlossaryTerm, String.join(" ", currentGlossaryDef));
                        }
                        currentGlossaryTerm = null;
                        currentGlossaryDef = new ArrayList<>();
                    } else if (currentGlossaryTerm == null) {
                        currentGlossaryTerm = stripped;
                    } else {
                        currentGlossaryDef.add(stripped);
                    }
                }
                case CREDITS -> doc.credits().add(line);
            }
            i++;
        }

        // Save remaining chapter/section
        if (currentSection != null && currentChapter != null) {
            currentChapter.sections().add(currentSection);
        }
        if (currentChapter != null) {
            doc.chapters().add(currentChapter);
        }

        return doc;
    }
}
