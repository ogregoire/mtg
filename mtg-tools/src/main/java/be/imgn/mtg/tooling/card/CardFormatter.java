package be.imgn.mtg.tooling.card;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.card.model.CardLegalityResult;
import be.imgn.mtg.tooling.card.model.CardPrintResult;
import be.imgn.mtg.tooling.card.model.CardResult;
import be.imgn.mtg.tooling.card.model.CardRulingResult;

/// Formats card information for console output.
public final class CardFormatter {

    private static final int FACE_WIDTH = 38;

    private CardFormatter() {}

    /// Formats a single card with all its details.
    ///
    /// @param card the card to format
    /// @param legalities the card's format legalities
    /// @param prints the card's prints (max 20 shown)
    /// @param totalSets total number of distinct sets
    /// @param rulings the card's rulings
    /// @param mostRecentPrint the most recent print (for Scryfall link)
    /// @return formatted card string
    public static String formatCard(
            CardResult card,
            List<CardLegalityResult> legalities,
            List<CardPrintResult> prints,
            int totalSets,
            List<CardRulingResult> rulings,
            @Nullable CardPrintResult mostRecentPrint) {
        var sb = new StringBuilder();

        if (card.isDoubleFaced()) {
            formatDoubleFacedCard(sb, card);
        } else {
            formatSingleFacedCard(sb, card);
        }

        // Color Identity
        sb.append("\n");
        formatColorIdentity(sb, card.colorIdentity());

        // Format Legalities
        sb.append("\n");
        formatLegalities(sb, legalities);

        // Prints
        if (!prints.isEmpty()) {
            sb.append("\n");
            formatPrints(sb, prints, totalSets);
        }

        // Rulings
        if (!rulings.isEmpty()) {
            sb.append("\n");
            formatRulings(sb, rulings);
        }

        // Links
        sb.append("\n");
        formatLinks(sb, card, mostRecentPrint);

        return sb.toString();
    }

    private static void formatSingleFacedCard(StringBuilder sb, CardResult card) {
        // Name and mana cost
        sb.append(card.name());
        if (card.manaCost() != null && !card.manaCost().isBlank()) {
            sb.append("  ").append(card.manaCost());
        }
        sb.append("\n");

        // Type line
        if (card.typeLine() != null) {
            sb.append(card.typeLine()).append("\n");
        }

        // Oracle text
        if (card.oracleText() != null && !card.oracleText().isBlank()) {
            sb.append("\n").append(card.oracleText()).append("\n");
        }

        // P/T, Loyalty, or Defense
        formatStats(sb, card.power(), card.toughness(), card.loyalty(), card.defense());
    }

    private static void formatDoubleFacedCard(StringBuilder sb, CardResult card) {
        var leftLines = new ArrayList<String>();
        var rightLines = new ArrayList<String>();

        // Face 1 (left side)
        var face1Name = card.face1Name() != null ? card.face1Name() : card.name();
        var face1Header = face1Name;
        if (card.face1ManaCost() != null && !card.face1ManaCost().isBlank()) {
            face1Header += "  " + card.face1ManaCost();
        }
        leftLines.add(face1Header);
        if (card.face1TypeLine() != null) {
            leftLines.add(card.face1TypeLine());
        }
        leftLines.add("");
        if (card.face1OracleText() != null && !card.face1OracleText().isBlank()) {
            leftLines.addAll(wrapText(card.face1OracleText(), FACE_WIDTH));
        }

        // Face 2 (right side)
        if (card.face2Name() != null) {
            var face2Header = card.face2Name();
            if (card.face2ManaCost() != null && !card.face2ManaCost().isBlank()) {
                face2Header += "  " + card.face2ManaCost();
            }
            rightLines.add(face2Header);
            if (card.face2TypeLine() != null) {
                rightLines.add(card.face2TypeLine());
            }
            rightLines.add("");
            if (card.face2OracleText() != null && !card.face2OracleText().isBlank()) {
                rightLines.addAll(wrapText(card.face2OracleText(), FACE_WIDTH));
            }
        }

        // Merge left and right columns
        var maxLines = Math.max(leftLines.size(), rightLines.size());
        for (int i = 0; i < maxLines; i++) {
            var left = i < leftLines.size() ? leftLines.get(i) : "";
            var right = i < rightLines.size() ? rightLines.get(i) : "";
            sb.append(padRight(left, FACE_WIDTH));
            sb.append(" | ");
            sb.append(right);
            sb.append("\n");
        }

        // Add stats on the same line for both faces
        var leftStats = formatStatsString(card.face1Power(), card.face1Toughness(), card.face1Loyalty(), null);
        var rightStats = formatStatsString(card.face2Power(), card.face2Toughness(), card.face2Loyalty(), null);
        if (leftStats != null || rightStats != null) {
            sb.append(padRight(leftStats != null ? leftStats : "", FACE_WIDTH));
            sb.append(" | ");
            sb.append(rightStats != null ? rightStats : "");
            sb.append("\n");
        }
    }

    private static void formatStats(
            StringBuilder sb,
            @Nullable String power,
            @Nullable String toughness,
            @Nullable String loyalty,
            @Nullable String defense) {
        if (power != null && toughness != null) {
            sb.append("\n").append(power).append("/").append(toughness).append("\n");
        } else if (loyalty != null) {
            sb.append("\nLoyalty: ").append(loyalty).append("\n");
        } else if (defense != null) {
            sb.append("\nDefense: ").append(defense).append("\n");
        }
    }

    private static @Nullable String formatStatsString(
            @Nullable String power, @Nullable String toughness, @Nullable String loyalty, @Nullable String defense) {
        if (power != null && toughness != null) {
            return power + "/" + toughness;
        } else if (loyalty != null) {
            return "Loyalty: " + loyalty;
        } else if (defense != null) {
            return "Defense: " + defense;
        }
        return null;
    }

    private static void formatColorIdentity(StringBuilder sb, @Nullable String colorIdentity) {
        sb.append("Color Identity: ");
        if (colorIdentity == null || colorIdentity.isBlank()) {
            sb.append("Colorless");
        } else {
            var colors = new ArrayList<String>();
            if (colorIdentity.contains("W")) colors.add("White");
            if (colorIdentity.contains("U")) colors.add("Blue");
            if (colorIdentity.contains("B")) colors.add("Black");
            if (colorIdentity.contains("R")) colors.add("Red");
            if (colorIdentity.contains("G")) colors.add("Green");
            sb.append(String.join(", ", colors));
        }
        sb.append("\n");
    }

    private static void formatLegalities(StringBuilder sb, List<CardLegalityResult> legalities) {
        sb.append("Format Legalities:\n");

        // Group by legality status
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("legal", new ArrayList<>());
        grouped.put("restricted", new ArrayList<>());
        grouped.put("banned", new ArrayList<>());
        grouped.put("not_legal", new ArrayList<>());

        for (var legality : legalities) {
            var status = legality.legality().toLowerCase(Locale.ROOT);
            var formatName = capitalize(legality.formatName());
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(formatName);
        }

        // Display grouped legalities
        if (!grouped.get("legal").isEmpty()) {
            sb.append("  Legal: ")
                    .append(String.join(", ", grouped.get("legal")))
                    .append("\n");
        }
        if (!grouped.get("restricted").isEmpty()) {
            sb.append("  Restricted: ")
                    .append(String.join(", ", grouped.get("restricted")))
                    .append("\n");
        }
        if (!grouped.get("banned").isEmpty()) {
            sb.append("  Banned: ")
                    .append(String.join(", ", grouped.get("banned")))
                    .append("\n");
        }
        if (!grouped.get("not_legal").isEmpty()) {
            sb.append("  Not Legal: ")
                    .append(String.join(", ", grouped.get("not_legal")))
                    .append("\n");
        }
    }

    private static void formatPrints(StringBuilder sb, List<CardPrintResult> prints, int totalSets) {
        sb.append("Printed in ").append(totalSets).append(totalSets == 1 ? " set" : " sets");
        if (totalSets > prints.size()) {
            sb.append(" (").append(prints.size()).append(" shown)");
        }
        sb.append(":\n");
        for (var print : prints) {
            sb.append("  - ")
                    .append(print.setName())
                    .append(" (")
                    .append(print.setCode().toUpperCase(Locale.ROOT))
                    .append(")\n");
        }
    }

    private static void formatRulings(StringBuilder sb, List<CardRulingResult> rulings) {
        sb.append("Rulings:\n");
        for (var ruling : rulings) {
            sb.append("  [")
                    .append(ruling.publishedAt())
                    .append("] ")
                    .append(ruling.comment())
                    .append("\n");
        }
    }

    private static void formatLinks(StringBuilder sb, CardResult card, @Nullable CardPrintResult mostRecentPrint) {
        sb.append("Links:\n");

        // Scryfall link
        if (mostRecentPrint != null) {
            sb.append("  Scryfall: https://scryfall.com/card/")
                    .append(mostRecentPrint.setCode().toLowerCase(Locale.ROOT))
                    .append("/")
                    .append(mostRecentPrint.collectorNumber())
                    .append("\n");
        }

        // Gatherer link
        var encodedName = URLEncoder.encode(card.name(), StandardCharsets.UTF_8);
        sb.append("  Gatherer: https://gatherer.wizards.com/Pages/Search/Default.aspx?name=+[")
                .append(encodedName)
                .append("]\n");
    }

    /// Formats search results as a list.
    ///
    /// @param cards the cards to display
    /// @param totalCount total number of matching cards
    /// @param page current page number (1-based)
    /// @param pageSize number of results per page
    /// @param originalCommand the original command (for pagination hints)
    /// @return formatted results string
    public static String formatSearchResults(
            List<CardResult> cards, int totalCount, int page, int pageSize, String originalCommand) {
        var sb = new StringBuilder();

        var totalPages = (totalCount + pageSize - 1) / pageSize;
        sb.append("Found ").append(totalCount).append(totalCount == 1 ? " card" : " cards");
        if (totalPages > 1) {
            sb.append(" (page ").append(page).append(" of ").append(totalPages).append(")");
        }
        sb.append("\n\n");

        for (var card : cards) {
            sb.append("  ").append(card.name());
            if (card.manaCost() != null && !card.manaCost().isBlank()) {
                sb.append("  ").append(card.manaCost());
            }
            sb.append("\n");
            if (card.typeLine() != null) {
                sb.append("    ").append(card.typeLine()).append("\n");
            }
            sb.append("    -> ./mtg card --name \"")
                    .append(escapeQuotes(card.name()))
                    .append("\"\n\n");
        }

        // Pagination hint
        if (page < totalPages) {
            var nextPageCmd = buildNextPageCommand(originalCommand, page + 1);
            sb.append("Next page: ").append(nextPageCmd).append("\n");
        }

        return sb.toString();
    }

    private static String buildNextPageCommand(String originalCommand, int nextPage) {
        // Remove existing --page argument if present
        var cmd = originalCommand.replaceAll("--page\\s+\\d+", "").trim();
        return cmd + " --page " + nextPage;
    }

    private static List<String> wrapText(String text, int width) {
        var lines = new ArrayList<String>();
        var paragraphs = text.split("\n", -1);

        for (var paragraph : paragraphs) {
            var words = paragraph.split("\\s+", -1);
            var currentLine = new StringBuilder();

            for (var word : words) {
                if (currentLine.isEmpty()) {
                    currentLine.append(word);
                } else if (currentLine.length() + 1 + word.length() <= width) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return s + " ".repeat(width - s.length());
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }
}
