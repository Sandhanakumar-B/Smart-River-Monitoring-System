package dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.RiverStation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 7: CSV File-based Implementation of StationDAO
 * Syllabus Unit: UNIT III & UNIT IV - Java File I/O, Streams, Exception Handling with Resources
 */
public class StationFileDAO implements StationDAO {

    public static final String DEFAULT_FILE_PATH = "data/stations.csv";
    private static final String CSV_HEADER = "stationId,stationName,riverName,normalLevelMeters,dangerLevelMeters";

    private final File storageFile;

    public StationFileDAO() {
        this(DEFAULT_FILE_PATH);
    }

    public StationFileDAO(String filePath) {
        this.storageFile = new File(filePath);
        ensureFileExists();
    }

    /**
     * Ensures data directory and stations.csv exist with headers and initial seeds
     */
    private synchronized void ensureFileExists() {
        try {
            File parentDir = storageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!storageFile.exists() || storageFile.length() == 0) {
                seedInitialStations();
            }
        } catch (IOException e) {
            System.err.println("[StationFileDAO Error] Failed to initialize storage file: " + e.getMessage());
        }
    }

    /**
     * Seeds initial river stations if storage file is missing
     */
    private void seedInitialStations() throws IOException {
        List<RiverStation> defaultStations = new ArrayList<>();
        defaultStations.add(new RiverStation("STN-HAR-01", "Haridwar Central Gauge Station", "Ganga River", 7.50, 16.50));
        defaultStations.add(new RiverStation("STN-RSH-02", "Rishikesh Barrage Station", "Ganga River", 6.20, 14.00));
        defaultStations.add(new RiverStation("STN-KNP-03", "Kanpur Ghat Station", "Ganga River", 8.00, 17.50));
        defaultStations.add(new RiverStation("STN-VRN-04", "Varanasi Assi Ghat Station", "Ganga River", 9.10, 18.20));

        saveAllStations(defaultStations);
    }

    @Override
    public synchronized List<RiverStation> getAllStations() throws IOException {
        List<RiverStation> stations = new ArrayList<>();

        if (!storageFile.exists()) {
            return stations;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(storageFile))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Skip CSV header line
                if (isFirstLine && line.toLowerCase().startsWith("stationid")) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                RiverStation station = parseCsvLine(line);
                if (station != null) {
                    stations.add(station);
                }
            }
        }

        return stations;
    }

    @Override
    public synchronized RiverStation getStationById(String stationId) throws IOException {
        if (stationId == null) {
            return null;
        }
        for (RiverStation station : getAllStations()) {
            if (station.getStationId().equalsIgnoreCase(stationId.trim())) {
                return station;
            }
        }
        return null;
    }

    @Override
    public synchronized void saveStation(RiverStation station) throws IOException {
        if (station == null) {
            throw new IllegalArgumentException("Cannot persist null RiverStation.");
        }

        // Check if file is empty to write header
        boolean writeHeader = !storageFile.exists() || storageFile.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFile, true))) {
            if (writeHeader) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }
            writer.write(toCsvLine(station));
            writer.newLine();
        }
    }

    @Override
    public synchronized void saveAllStations(List<RiverStation> stations) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFile, false))) {
            writer.write(CSV_HEADER);
            writer.newLine();
            if (stations != null) {
                for (RiverStation station : stations) {
                    writer.write(toCsvLine(station));
                    writer.newLine();
                }
            }
        }
    }

    @Override
    public synchronized boolean existsById(String stationId) throws IOException {
        return getStationById(stationId) != null;
    }

    @Override
    public String getStorageSource() {
        return storageFile.getAbsolutePath();
    }

    /**
     * Converts a CSV line into a RiverStation object
     */
    private RiverStation parseCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) {
            return null;
        }

        try {
            String stationId = parts[0].trim();
            String stationName = parts[1].trim();
            String riverName = parts[2].trim();
            double normalLevel = Double.parseDouble(parts[3].trim());
            double dangerLevel = Double.parseDouble(parts[4].trim());

            return new RiverStation(stationId, stationName, riverName, normalLevel, dangerLevel);
        } catch (NumberFormatException e) {
            System.err.println("[StationFileDAO Warning] Skipping malformed line: " + line);
            return null;
        }
    }

    /**
     * Formats a RiverStation into a clean CSV record
     */
    private String toCsvLine(RiverStation s) {
        return String.format("%s,%s,%s,%.2f,%.2f",
            escapeCsv(s.getStationId()),
            escapeCsv(s.getStationName()),
            escapeCsv(s.getRiverName()),
            s.getNormalLevelMeters(),
            s.getDangerLevelMeters()
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // If value contains comma, wrap in quotes
        if (value.contains(",")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
