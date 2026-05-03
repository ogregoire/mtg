package be.imgn.mtg.engine.oracle.domain2;

import java.util.Arrays;
import java.util.List;

/// A subtype of a card type ({@mtg.rule 205.3}). The parser recognizes
/// subtypes of every card type the oracle grammar cares about; each is a
/// distinct enum so consumers can pattern-match on the family.
///
/// Each subtype's spellings are declared with a single [template string][#parseTemplate]
/// mirroring the `phrase()` grammar:
/// - `"Ajani"` → `["Ajani"]` (invariant, no plural).
/// - `"Goblin(s)"` → `["Goblin", "Goblins"]` (regular `+s` plural).
/// - `"Witness(es)"` → `["Witness", "Witnesses"]` (custom suffix).
/// - `"[Ally|Allies]"` → `["Ally", "Allies"]` (irregular alternation).
public sealed interface Subtype
        permits ArtifactType, BattleType, CreatureType, EnchantmentType, LandType, PlaneswalkerType, SpellType {

    /// Template string declaring this subtype's spellings — fed
    /// directly to `Words.phrase` to build the parser. Examples:
    /// `"Ajani"`, `"Goblin(s)"`, `"Witness(es)"`, `"[Ally|Allies]"`.
    String text();

    /// All text forms this subtype takes in oracle text. Derived from
    /// [#text()]: the first element is always the canonical
    /// singular ("Goblin"), and the second (when present) is the plural
    /// ("Goblins"). Invariant subtypes return a single-element list.
    default List<String> texts() {
        return parseTemplate(text());
    }

    /// Parse a subtype template string into its list of spellings (see
    /// the interface-level docs). The first element is always the
    /// canonical singular form.
    static List<String> parseTemplate(String template) {
        if (template.startsWith("[") && template.endsWith("]")) {
            return List.copyOf(
                    Arrays.asList(template.substring(1, template.length() - 1).split("\\|")));
        }
        var open = template.indexOf('(');
        if (open >= 0 && template.endsWith(")")) {
            var base = template.substring(0, open);
            var suffix = template.substring(open + 1, template.length() - 1);
            return List.of(base, base + suffix);
        }
        return List.of(template);
    }
}
