package be.imgn.mtg.rules.maven;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data model for parsed MTG Comprehensive Rules. */
public final class RulesDocument {

    private String title = "";
    private String effectiveDate = "";
    private final List<String> introduction = new ArrayList<>();
    private final List<TocEntry> toc = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private final Map<String, String> glossary = new LinkedHashMap<>();
    private final List<String> credits = new ArrayList<>();

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String effectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public List<String> introduction() {
        return introduction;
    }

    public List<TocEntry> toc() {
        return toc;
    }

    public List<Chapter> chapters() {
        return chapters;
    }

    public Map<String, String> glossary() {
        return glossary;
    }

    public List<String> credits() {
        return credits;
    }

    public enum TocType {
        CHAPTER,
        SECTION,
        GLOSSARY,
        CREDITS
    }

    public record TocEntry(TocType type, String text) {}

    public record Chapter(String number, String title, List<Section> sections) {
        public Chapter(String number, String title) {
            this(number, title, new ArrayList<>());
        }
    }

    public record Section(String number, String title, List<String> rules) {
        public Section(String number, String title) {
            this(number, title, new ArrayList<>());
        }
    }
}
