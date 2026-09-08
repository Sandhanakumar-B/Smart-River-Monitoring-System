package dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 7: CSV File-based Implementation of WaterLevelRecordDAO
 * Syllabus Unit: UNIT III & UNIT IV - Java File I/O, Streams, CSV Serialization
 */
public class WaterLevelRecordFileDAO implements WaterLevelRecordDAO {

    public static final String DEFAULT_FILE_PATH = "data/readings.csv";
    private static final String CSV_HEADER = "recordId,riverName,stationLocation,waterLevelMeters,alertStatus,timestamp";

    private final File storageFile;

    public WaterLevelRecordFileDAO() {
        this(DEFAULT_FILE_PATH);
    }

    public WaterLevelRecordFileDAO(String filePath) {
        this.storageFile = new File(filePath);
        ensureFileExists();
    }

    /**
     * Ensures data directory and readings.csv exist with headers and baseline seeds
     */
    private synchronized void ensureFileExists() {
        try {
            File parentDir = storageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!storageFile.exists() || storageFile.length() == 0) {
                seedInitialReadings();
            }
        } catch (IOException e) {
            System.err.println("[WaterLevelRecordFileDAO Error] Failed to initialize storage: " + e.getMessage());
        }
    }

    private void seedInitialReadings() throws IOException {
        List<WaterLevelRecord> defaultReadings = new ArrayList<>();
        defaultReadings.add(new WaterLevelRecord(
            "REC-1001",
            "Ganga River",
            "Haridwar Central Gauge Station (STN-HAR-01)",
            8.20,
            "2026-09-01 08:00 AM"
        ));
        defaultReadings.add(new WaterLevelRecord(
            "REC-1002",
            "Ganga River",
            "Rishikesh Barrage Station (STN-RSH-02)",
            7.00,
            "2026-09-01 09:30 AM"
        ));
        defaultReadings.add(new WaterLevelRecord(
            "REC-1003",
            "Ganga River",
            "Kanpur Ghat Station (STN-KNP-03)",
            17.80,
            "2026-09-01 11:15 AM"
        ));

        saveAllRecords(defaultReadings);
    }

    @Override
    public synchronized List<WaterLevelRecord> getAllRecords() throws IOException {
        List<WaterLevelRecord> records = new ArrayList<>();

        if (!storageFile.exists()) {
            return records;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(storageFile))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (isFirstLine && line.toLowerCase().startsWith("recordid")) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                WaterLevelRecord record = parseCsvLine(line);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        return records;
    }

    @Override
    public synchronized void saveRecord(WaterLevelRecord record) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("Cannot persist null WaterLevelRecord.");
        }

        boolean writeHeader = !storageFile.exists() || storageFile.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFile, true))) {
            if (writeHeader) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }
            writer.write(toCsvLine(record));
            writer.newLine();
        }
    }

    @Override
    public synchronized void saveAllRecords(List<WaterLevelRecord> records) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFile, false))) {
            writer.write(CSV_HEADER);
            writer.newLine();
            if (records != null) {
                for (WaterLevelRecord record : records) {
                    writer.write(toCsvLine(record));
                    writer.newLine();
                }
            }
        }
    }

    @Override
    public synchronized List<WaterLevelRecord> getRecordsByRiver(String riverName) throws IOException {
        List<WaterLevelRecord> filtered = new ArrayList<>();
        if (riverName == null) return filtered;

        for (WaterLevelRecord record : getAllRecords()) {
            if (record.getRiverName().equalsIgnoreCase(riverName.trim())) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    @Override
    public String getStorageSource() {
        return storageFile.getAbsolutePath();
    }

    private WaterLevelRecord parseCsvLine(String line) {
        // Robust CSV splitter that respects quoted fields containing commas
        List<String> tokens = parseCsvTokens(line);
        if (tokens.size() < 6) {
            return null;
        }

        try {
            String recordId = tokens.get(0).trim();
            String riverName = tokens.get(1).trim();
            String stationLocation = tokens.get(2).trim();
            double waterLevel = Double.parseDouble(tokens.get(3).trim());
            // tokens.get(4) is alertStatus (can be recomputed or verified)
            String timestamp = tokens.get(5).trim();

            return new WaterLevelRecord(recordId, riverName, stationLocation, waterLevel, timestamp);
        } catch (NumberFormatException e) {
            System.err.println("[WaterLevelRecordFileDAO Warning] Skipping malformed line: " + line);
            return null;
        }
    }

    private List<String> parseCsvTokens(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private String toCsvLine(WaterLevelRecord r) {
        return String.format("%s,%s,%s,%.2f,%s,%s",
            escapeCsv(r.getRecordId()),
            escapeCsv(r.getRiverName()),
            escapeCsv(r.getStationLocation()),
            r.getWaterLevelMeters(),
            escapeCsv(r.getAlertStatus()),
            escapeCsv(r.getTimestamp())
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
