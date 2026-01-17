package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.db.dao.CardDao;
import be.imgn.mtg.tooling.db.dao.FormatDao;
import be.imgn.mtg.tooling.db.dao.LegalityDao;
import be.imgn.mtg.tooling.db.dao.PrintDao;
import be.imgn.mtg.tooling.db.dao.RulingDao;
import be.imgn.mtg.tooling.db.dao.SetDao;
import be.imgn.mtg.tooling.db.dao.UtilDao;
import be.imgn.mtg.tooling.scryfall.model.ScryfallBulkData;
import be.imgn.mtg.tooling.scryfall.model.ScryfallCard;
import be.imgn.mtg.tooling.scryfall.model.ScryfallCard.ScryfallCardFace;
import be.imgn.mtg.tooling.scryfall.model.ScryfallRuling;

/// Synchronizes card data from Scryfall to the local H2 database.
///
/// This service downloads bulk data from Scryfall and imports it into
/// the database using batch operations for optimal performance.
public final class ScryfallSync {

    private static final int BATCH_SIZE = 1000;
    private static final String ORACLE_CARDS_TYPE = "oracle_cards";
    private static final String RULINGS_TYPE = "rulings";
    private static final String DEFAULT_CARDS_TYPE = "default_cards";

    /// Layouts that should be excluded from the database.
    private static final Set<String> EXCLUDED_LAYOUTS = Set.of("art_series");

    private final ScryfallClient client;
    private final Jdbi jdbi;
    private final PrintStream out;

    /// Creates a new ScryfallSync service.
    ///
    /// @param client the Scryfall API client
    /// @param jdbi the JDBI instance
    public ScryfallSync(ScryfallClient client, Jdbi jdbi) {
        this(client, jdbi, System.out);
    }

    /// Creates a new ScryfallSync service with custom output stream.
    ///
    /// @param client the Scryfall API client
    /// @param jdbi the JDBI instance
    /// @param out output stream for progress messages
    ScryfallSync(ScryfallClient client, Jdbi jdbi, PrintStream out) {
        this.client = client;
        this.jdbi = jdbi;
        this.out = out;
    }

    /// Performs a full sync of all card data from Scryfall.
    ///
    /// This downloads the oracle cards bulk data and imports all cards
    /// into the database, replacing any existing data.
    ///
    /// @throws IOException if the download or import fails
    public void syncAll() throws IOException {
        long totalStart = System.nanoTime();
        out.println("Syncing from Scryfall...");
        out.flush();

        syncSets();

        // Fetch bulk data list once for all bulk imports
        var bulkDownload = client.getBulkDataList();
        var bulkData = bulkDownload.bulkData();
        var tracker = ProgressTracker.forDownload("Downloading bulk-data", bulkDownload.bytes(), out);
        tracker.update(bulkDownload.bytes());
        tracker.complete();

        syncCardsAndLegalities(bulkData);
        syncRulings(bulkData);
        syncPrints(bulkData);

        long totalTime = System.nanoTime() - totalStart;
        out.println();
        out.printf("Sync completed successfully in %s%n", formatDuration(totalTime));
    }

    private static String formatDuration(long nanos) {
        long millis = nanos / 1_000_000;
        if (millis < 1000) {
            return millis + " ms";
        }
        double seconds = millis / 1000.0;
        if (seconds < 60) {
            return String.format("%.2f s", seconds);
        }
        long mins = (long) (seconds / 60);
        double secs = seconds % 60;
        return String.format("%d min %.2f s", mins, secs);
    }

    /// Syncs set data from Scryfall.
    ///
    /// @throws IOException if the download or import fails
    public void syncSets() throws IOException {
        long startTime = System.nanoTime();

        var download = client.getSets();
        var sets = download.sets();

        // Show download completion
        var tracker = ProgressTracker.forDownload("Downloading sets", download.bytes(), out);
        tracker.update(download.bytes());
        tracker.complete();

        jdbi.useHandle(handle -> {
            var utilDao = handle.attach(UtilDao.class);
            var setDao = handle.attach(SetDao.class);

            // Clear existing sets
            utilDao.disableReferentialIntegrity();
            setDao.deleteAll();
            utilDao.enableReferentialIntegrity();

            // Insert all sets
            for (var set : sets) {
                setDao.insert(
                        set.code(),
                        set.name(),
                        set.setType(),
                        set.block(),
                        set.blockCode(),
                        "{}" // data field - empty JSON for now
                        );
            }

            // Link parent sets
            var codeToSetId = setDao.getAllCodeToSetId();
            for (var set : sets) {
                var parentCode = set.parentSetCode();
                if (parentCode != null) {
                    var parentId = codeToSetId.get(parentCode);
                    if (parentId != null) {
                        setDao.updateParentSetId(set.code(), parentId);
                    }
                }
            }
        });
        out.printf("Imported %,d sets in %s%n", sets.size(), formatDuration(System.nanoTime() - startTime));
    }

    /// Syncs card and legality data from Scryfall.
    ///
    /// @throws IOException if the download or import fails
    public void syncCardsAndLegalities() throws IOException {
        syncCardsAndLegalities(client.getBulkDataList().bulkData());
    }

    private void syncCardsAndLegalities(List<ScryfallBulkData> bulkData) throws IOException {
        long startTime = System.nanoTime();

        var oracleData = bulkData.stream()
                .filter(bd -> ORACLE_CARDS_TYPE.equals(bd.type()))
                .findFirst()
                .orElseThrow(() -> new IOException("Oracle cards bulk data not found"));

        importCardsAndLegalities(oracleData, startTime);
    }

    /// Syncs only rulings data from Scryfall.
    ///
    /// @throws IOException if the download or import fails
    public void syncRulings() throws IOException {
        syncRulings(client.getBulkDataList().bulkData());
    }

    private void syncRulings(List<ScryfallBulkData> bulkData) throws IOException {
        long startTime = System.nanoTime();

        var rulingsData = bulkData.stream()
                .filter(bd -> RULINGS_TYPE.equals(bd.type()))
                .findFirst()
                .orElseThrow(() -> new IOException("Rulings bulk data not found"));

        importRulings(rulingsData, startTime);
    }

    /// Syncs print data from Scryfall default_cards bulk data.
    ///
    /// This downloads the full list of all card printings and inserts them
    /// into the print table.
    ///
    /// @throws IOException if the download or import fails
    public void syncPrints() throws IOException {
        syncPrints(client.getBulkDataList().bulkData());
    }

    private void syncPrints(List<ScryfallBulkData> bulkData) throws IOException {
        long startTime = System.nanoTime();

        var defaultCardsData = bulkData.stream()
                .filter(bd -> DEFAULT_CARDS_TYPE.equals(bd.type()))
                .findFirst()
                .orElseThrow(() -> new IOException("Default cards bulk data not found"));

        importPrints(defaultCardsData, startTime);
    }

    private void importPrints(ScryfallBulkData bulkData, long startTime) throws IOException {
        var downloadUri = bulkData.downloadUri();
        var contentLength = bulkData.size();

        // Get lookup maps
        Map<UUID, Long> oracleIdToCardId = jdbi.withExtension(CardDao.class, CardDao::getAllOracleIdToCardId);
        Map<String, Long> setCodeToSetId = jdbi.withExtension(SetDao.class, SetDao::getAllCodeToSetId);

        List<Long> batchCardIds = new ArrayList<>(BATCH_SIZE);
        List<Long> batchSetIds = new ArrayList<>(BATCH_SIZE);
        List<String> batchCollectorNumbers = new ArrayList<>(BATCH_SIZE);
        List<String> batchRarities = new ArrayList<>(BATCH_SIZE);
        List<String> batchData = new ArrayList<>(BATCH_SIZE);
        int[] totalCount = {0};
        int[] skippedCount = {0};

        var downloadProgress = ProgressTracker.forDownload("Downloading prints", contentLength, out);
        jdbi.useHandle(handle -> {
            var printDao = handle.attach(PrintDao.class);

            // Drop indexes and constraints for faster inserts
            printDao.dropCardIndex();
            printDao.dropSetIndex();
            printDao.dropRarityIndex();
            printDao.dropUniqueConstraint();
            printDao.deleteAll();

            try {
                client.streamCards(
                        downloadUri,
                        card -> {
                            // Skip excluded layouts (e.g., art_series)
                            if (EXCLUDED_LAYOUTS.contains(card.layout())) {
                                skippedCount[0]++;
                                return;
                            }

                            var oracleIdStr = card.oracleId();
                            var setCode = card.setCode();
                            var collectorNumber = card.collectorNumber();
                            var rarity = card.rarity();

                            // Skip cards without required fields
                            if (oracleIdStr == null || setCode == null || collectorNumber == null || rarity == null) {
                                skippedCount[0]++;
                                return;
                            }

                            var oracleId = UUID.fromString(oracleIdStr);
                            var cardId = oracleIdToCardId.get(oracleId);
                            var setId = setCodeToSetId.get(setCode);

                            // Skip unknown cards or sets
                            if (cardId == null || setId == null) {
                                skippedCount[0]++;
                                return;
                            }

                            batchCardIds.add(cardId);
                            batchSetIds.add(setId);
                            batchCollectorNumbers.add(collectorNumber);
                            batchRarities.add(rarity);
                            batchData.add("{}"); // Empty JSON for now

                            if (batchCardIds.size() >= BATCH_SIZE) {
                                printDao.insertBatch(
                                        batchCardIds, batchSetIds, batchCollectorNumbers, batchRarities, batchData);
                                totalCount[0] += batchCardIds.size();
                                batchCardIds.clear();
                                batchSetIds.clear();
                                batchCollectorNumbers.clear();
                                batchRarities.clear();
                                batchData.clear();
                            }
                        },
                        downloadProgress::update);

                // Insert remaining prints
                if (!batchCardIds.isEmpty()) {
                    printDao.insertBatch(batchCardIds, batchSetIds, batchCollectorNumbers, batchRarities, batchData);
                    totalCount[0] += batchCardIds.size();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to stream cards for prints", e);
            }

            // Recreate indexes and constraints
            printDao.createCardIndex();
            printDao.createSetIndex();
            printDao.createRarityIndex();
            printDao.createUniqueConstraint();
        });
        downloadProgress.complete();
        out.printf(
                "Imported %,d prints (%,d skipped) in %s%n",
                totalCount[0], skippedCount[0], formatDuration(System.nanoTime() - startTime));
    }

    private void importCardsAndLegalities(ScryfallBulkData bulkData, long startTime) throws IOException {
        var downloadUri = bulkData.downloadUri();
        var contentLength = bulkData.size();

        List<ScryfallCard> batch = new ArrayList<>(BATCH_SIZE);
        Set<String> formatNames = new HashSet<>();
        int[] cardCount = {0};

        var downloadProgress = ProgressTracker.forDownload("Downloading cards", contentLength, out);
        jdbi.useHandle(handle -> {
            var utilDao = handle.attach(UtilDao.class);
            var cardDao = handle.attach(CardDao.class);
            var formatDao = handle.attach(FormatDao.class);
            var legalityDao = handle.attach(LegalityDao.class);

            // Clear existing data (legalities first due to FK constraint)
            // Drop indexes for faster bulk insert
            legalityDao.dropLegalityIndex();
            legalityDao.dropCardFormatIndex();
            legalityDao.dropFormatLegalityCardIndex();
            utilDao.disableReferentialIntegrity();
            legalityDao.deleteAll();
            cardDao.deleteAll();
            formatDao.deleteAll();
            utilDao.enableReferentialIntegrity();

            // We need format IDs, but we don't know all formats upfront
            // So we'll collect them in first pass, insert formats, then do cards+legalities
            // Actually, let's do a simpler approach: insert formats on-demand as we encounter them
            Map<String, Long> formatNameToId = new HashMap<>();

            try {
                client.streamCards(
                        downloadUri,
                        card -> {
                            // Skip excluded layouts (e.g., art_series)
                            if (EXCLUDED_LAYOUTS.contains(card.layout())) {
                                return;
                            }

                            // Collect format names from this card
                            var legalities = card.legalities();
                            for (String formatName : legalities.keySet()) {
                                if (!formatNameToId.containsKey(formatName)) {
                                    formatNames.add(formatName);
                                }
                            }

                            batch.add(card);
                            if (batch.size() >= BATCH_SIZE) {
                                // Insert any new formats we've encountered
                                insertNewFormats(formatDao, formatNames, formatNameToId);
                                formatNames.clear();

                                // Insert cards and get their IDs
                                var insertedIds = insertCardBatchAndGetIds(cardDao, batch);
                                cardCount[0] += batch.size();

                                // Insert legalities for these cards
                                insertLegalitiesForBatch(legalityDao, batch, insertedIds, formatNameToId);

                                batch.clear();
                            }
                        },
                        downloadProgress::update);

                // Insert remaining
                if (!batch.isEmpty()) {
                    insertNewFormats(formatDao, formatNames, formatNameToId);
                    var insertedIds = insertCardBatchAndGetIds(cardDao, batch);
                    cardCount[0] += batch.size();
                    insertLegalitiesForBatch(legalityDao, batch, insertedIds, formatNameToId);
                }

                // Recreate legality indexes
                legalityDao.createLegalityIndex();
                legalityDao.createCardFormatIndex();
                legalityDao.createFormatLegalityCardIndex();

            } catch (IOException e) {
                throw new RuntimeException("Failed to stream cards", e);
            }
        });
        downloadProgress.complete();
        out.printf("Imported %,d cards in %s%n", cardCount[0], formatDuration(System.nanoTime() - startTime));
    }

    private void insertNewFormats(FormatDao formatDao, Set<String> newFormats, Map<String, Long> formatNameToId) {
        for (String formatName : newFormats) {
            if (!formatNameToId.containsKey(formatName)) {
                long id = formatDao.insert(formatName);
                formatNameToId.put(formatName, id);
            }
        }
    }

    private Map<UUID, Long> insertCardBatchAndGetIds(CardDao cardDao, List<ScryfallCard> cards) {
        Map<UUID, Long> result = new HashMap<>();
        for (var card : cards) {
            var oracleIdStr = card.oracleId();
            if (oracleIdStr == null) {
                continue;
            }
            var oracleId = UUID.fromString(oracleIdStr);

            long cardId = cardDao.insertAndGetId(
                    oracleId,
                    card.name(),
                    card.layout(),
                    card.cmc(),
                    joinColors(card.colorIdentity()),
                    joinColors(card.colorIndicator()),
                    joinColors(card.colors()),
                    card.defense(),
                    card.handModifier(),
                    joinKeywords(card.keywords()),
                    card.lifeModifier(),
                    card.loyalty(),
                    card.manaCost(),
                    card.oracleText(),
                    card.power(),
                    card.toughness(),
                    card.typeLine(),
                    getFaceName(card.cardFaces(), 0),
                    getFaceManaValue(card.cardFaces(), 0),
                    getFaceColorIndicator(card.cardFaces(), 0),
                    getFaceColors(card.cardFaces(), 0),
                    getFaceDefense(card.cardFaces(), 0),
                    getFaceLoyalty(card.cardFaces(), 0),
                    getFaceManaCost(card.cardFaces(), 0),
                    getFaceOracleText(card.cardFaces(), 0),
                    getFacePower(card.cardFaces(), 0),
                    getFaceToughness(card.cardFaces(), 0),
                    getFaceTypeLine(card.cardFaces(), 0),
                    getFaceName(card.cardFaces(), 1),
                    getFaceManaValue(card.cardFaces(), 1),
                    getFaceColorIndicator(card.cardFaces(), 1),
                    getFaceColors(card.cardFaces(), 1),
                    getFaceDefense(card.cardFaces(), 1),
                    getFaceLoyalty(card.cardFaces(), 1),
                    getFaceManaCost(card.cardFaces(), 1),
                    getFaceOracleText(card.cardFaces(), 1),
                    getFacePower(card.cardFaces(), 1),
                    getFaceToughness(card.cardFaces(), 1),
                    getFaceTypeLine(card.cardFaces(), 1),
                    "{}");

            result.put(oracleId, cardId);
        }
        return result;
    }

    private void insertLegalitiesForBatch(
            LegalityDao legalityDao,
            List<ScryfallCard> cards,
            Map<UUID, Long> oracleIdToCardId,
            Map<String, Long> formatNameToId) {
        List<Long> batchCardIds = new ArrayList<>();
        List<Long> batchFormatIds = new ArrayList<>();
        List<String> batchLegalities = new ArrayList<>();

        for (var card : cards) {
            var oracleIdStr = card.oracleId();
            if (oracleIdStr == null) continue;

            var cardId = oracleIdToCardId.get(UUID.fromString(oracleIdStr));
            if (cardId == null) continue;

            for (var entry : card.legalities().entrySet()) {
                var formatId = formatNameToId.get(entry.getKey());
                if (formatId == null) continue;

                batchCardIds.add(cardId);
                batchFormatIds.add(formatId);
                batchLegalities.add(entry.getValue());
            }
        }

        if (!batchCardIds.isEmpty()) {
            legalityDao.insertBatch(batchCardIds, batchFormatIds, batchLegalities);
        }
    }

    private void importRulings(ScryfallBulkData bulkData, long startTime) throws IOException {
        var downloadUri = bulkData.downloadUri();
        var contentLength = bulkData.size();

        var batch = new ArrayList<ScryfallRuling>(BATCH_SIZE);
        int[] totalCount = {0};

        var downloadProgress = ProgressTracker.forDownload("Downloading rulings", contentLength, out);
        jdbi.useHandle(handle -> {
            var rulingDao = handle.attach(RulingDao.class);

            // Clear existing rulings
            rulingDao.deleteAll();

            try {
                client.streamRulings(
                        downloadUri,
                        ruling -> {
                            batch.add(ruling);
                            if (batch.size() >= BATCH_SIZE) {
                                insertRulingBatch(rulingDao, batch);
                                totalCount[0] += batch.size();
                                batch.clear();
                            }
                        },
                        downloadProgress::update);

                // Insert remaining rulings
                if (!batch.isEmpty()) {
                    insertRulingBatch(rulingDao, batch);
                    totalCount[0] += batch.size();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to stream rulings", e);
            }
        });
        downloadProgress.complete();
        out.printf("Imported %,d rulings in %s%n", totalCount[0], formatDuration(System.nanoTime() - startTime));
    }

    private void insertRulingBatch(RulingDao rulingDao, List<ScryfallRuling> rulings) {
        for (var ruling : rulings) {
            var oracleId = UUID.fromString(ruling.oracleId());
            var publishedAt = LocalDate.parse(ruling.publishedAt());
            rulingDao.insert(oracleId, ruling.source(), publishedAt, ruling.comment());
        }
    }

    private @Nullable String joinColors(@Nullable List<String> colors) {
        if (colors == null || colors.isEmpty()) {
            return null;
        }
        return String.join("", colors);
    }

    private @Nullable String joinKeywords(@Nullable List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        return String.join(",", keywords);
    }

    private @Nullable String getFaceName(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).name();
    }

    private @Nullable Double getFaceManaValue(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).cmc();
    }

    private @Nullable String getFaceColorIndicator(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return joinColors(faces.get(index).colorIndicator());
    }

    private @Nullable String getFaceColors(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return joinColors(faces.get(index).colors());
    }

    private @Nullable String getFaceDefense(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).defense();
    }

    private @Nullable String getFaceLoyalty(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).loyalty();
    }

    private @Nullable String getFaceManaCost(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).manaCost();
    }

    private @Nullable String getFaceOracleText(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).oracleText();
    }

    private @Nullable String getFacePower(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).power();
    }

    private @Nullable String getFaceToughness(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).toughness();
    }

    private @Nullable String getFaceTypeLine(@Nullable List<ScryfallCardFace> faces, int index) {
        if (faces == null || index >= faces.size()) {
            return null;
        }
        return faces.get(index).typeLine();
    }
}
