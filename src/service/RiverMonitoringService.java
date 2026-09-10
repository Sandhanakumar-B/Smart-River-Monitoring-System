package service;

import dao.StationDAO;
import dao.StationFileDAO;
import dao.WaterLevelRecordDAO;
import dao.WaterLevelRecordFileDAO;
import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import imageprocessing.GaugeProcessingResult;
import imageprocessing.WaterLevelImageProcessor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import model.RiverStation;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 8: Service Layer with Concurrent Multithreading & Thread-Safe Collections
 * Syllabus Unit: UNIT III, IV & V - Thread Safety, CopyOnWriteArrayList, DAO Pattern, File I/O
 */
public class RiverMonitoringService {

    public static final double MIN_PERMISSIBLE_LEVEL = 0.0;
    public static final double MAX_PERMISSIBLE_LEVEL = 50.0;

    private final StationDAO stationDAO;
    private final WaterLevelRecordDAO recordDAO;
    private final List<RiverStation> stations;
    private final List<WaterLevelRecord> records;
    private final WaterLevelImageProcessor imageProcessor;

    /**
     * Default constructor initializing standard File DAOs (data/stations.csv & data/readings.csv)
     */
    public RiverMonitoringService() {
        this(new StationFileDAO(), new WaterLevelRecordFileDAO());
    }

    /**
     * Dependency injection constructor allowing custom or mock DAOs
     */
    public RiverMonitoringService(StationDAO stationDAO, WaterLevelRecordDAO recordDAO) {
        this.stationDAO = stationDAO;
        this.recordDAO = recordDAO;
        this.stations = new CopyOnWriteArrayList<>();
        this.records = new CopyOnWriteArrayList<>();
        this.imageProcessor = new WaterLevelImageProcessor();
        loadDataFromStorage();
    }

    /**
     * Synchronizes in-memory collections with permanent DAO storage
     */
    public synchronized void loadDataFromStorage() {
        this.stations.clear();
        this.records.clear();

        try {
            List<RiverStation> storedStations = this.stationDAO.getAllStations();
            this.stations.addAll(storedStations);

            List<WaterLevelRecord> storedRecords = this.recordDAO.getAllRecords();
            this.records.addAll(storedRecords);
        } catch (IOException e) {
            System.err.println("[RiverMonitoringService Error] Failed to load data from storage: " + e.getMessage());
            bootstrapDefaultsIfEmpty();
        }

        if (this.stations.isEmpty()) {
            bootstrapDefaultsIfEmpty();
        }
    }

    /**
     * Fallback bootstrap if storage was empty
     */
    private void bootstrapDefaultsIfEmpty() {
        try {
            registerStation(new RiverStation("STN-HAR-01", "Haridwar Central Gauge Station", "Ganga River", 7.5, 16.5));
            registerStation(new RiverStation("STN-RSH-02", "Rishikesh Barrage Station", "Ganga River", 6.2, 14.0));
            registerStation(new RiverStation("STN-KNP-03", "Kanpur Ghat Station", "Ganga River", 8.0, 17.5));
            registerStation(new RiverStation("STN-VRN-04", "Varanasi Assi Ghat Station", "Ganga River", 9.1, 18.2));

            recordMeasurement("STN-HAR-01", 8.2, "2026-09-01 08:00 AM");
            recordMeasurement("STN-RSH-02", 7.0, "2026-09-01 09:30 AM");
            recordMeasurement("STN-KNP-03", 17.8, "2026-09-01 11:15 AM");
        } catch (DuplicateStationException | StationNotFoundException | InvalidWaterLevelException e) {
            System.err.println("Warning during system bootstrapping: " + e.getMessage());
        }
    }

    /**
     * Registers a new monitoring station into the system and commits to storage via DAO.
     * Throws DuplicateStationException if station ID is already in use.
     */
    public synchronized void registerStation(RiverStation station) throws DuplicateStationException {
        if (station == null || station.getStationId() == null || station.getStationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Station and Station ID cannot be null or empty.");
        }

        if (getStationById(station.getStationId()) != null) {
            throw new DuplicateStationException(station.getStationId());
        }

        this.stations.add(station);

        // Persist via DAO
        try {
            this.stationDAO.saveStation(station);
        } catch (IOException e) {
            System.err.println("[Persistence Warning] Failed to save station to permanent storage: " + e.getMessage());
        }
    }

    /**
     * Returns an unmodifiable view of all registered stations
     */
    public List<RiverStation> getAllStations() {
        return Collections.unmodifiableList(this.stations);
    }

    /**
     * Finds a station by its unique identifier (returns null if not found)
     */
    public RiverStation getStationById(String stationId) {
        if (stationId == null) {
            return null;
        }
        for (RiverStation station : this.stations) {
            if (station.getStationId().equalsIgnoreCase(stationId.trim())) {
                return station;
            }
        }
        return null;
    }

    /**
     * Finds a station by its ID or throws StationNotFoundException
     */
    public RiverStation getStationByIdOrThrow(String stationId) throws StationNotFoundException {
        RiverStation station = getStationById(stationId);
        if (station == null) {
            throw new StationNotFoundException(stationId);
        }
        return station;
    }

    /**
     * Validates, creates, records, and permanently persists a new water level measurement.
     */
    public synchronized WaterLevelRecord recordMeasurement(String stationId, double levelMeters, String timestamp)
            throws StationNotFoundException, InvalidWaterLevelException {
        
        // Validate level constraints (UNIT IV - Throwing Custom Exception)
        if (levelMeters < MIN_PERMISSIBLE_LEVEL || levelMeters > MAX_PERMISSIBLE_LEVEL) {
            throw new InvalidWaterLevelException(levelMeters);
        }

        RiverStation station = getStationByIdOrThrow(stationId);

        String recordId = "REC-" + (1000 + this.records.size() + 1);
        WaterLevelRecord record = station.generateReading(recordId, levelMeters, timestamp);
        this.records.add(record);

        // Persist via DAO
        try {
            this.recordDAO.saveRecord(record);
        } catch (IOException e) {
            System.err.println("[Persistence Warning] Failed to save record to permanent storage: " + e.getMessage());
        }

        return record;
    }

    /**
     * Returns all historical water level readings
     */
    public List<WaterLevelRecord> getAllRecords() {
        return Collections.unmodifiableList(this.records);
    }

    /**
     * Filters and returns only readings that triggered critical flood warnings
     */
    public List<WaterLevelRecord> getCriticalAlertRecords() {
        List<WaterLevelRecord> alerts = new ArrayList<>();
        for (WaterLevelRecord record : this.records) {
            if (record.getAlertStatus().startsWith("CRITICAL") || record.getAlertStatus().startsWith("WARNING")) {
                alerts.add(record);
            }
        }
        return alerts;
    }

    /**
     * Computes the mathematical average of all recorded water levels
     */
    public double getAverageWaterLevel() {
        if (this.records.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (WaterLevelRecord record : this.records) {
            sum += record.getWaterLevelMeters();
        }
        return sum / this.records.size();
    }

    /**
     * Finds the maximum water level recorded so far
     */
    public double getMaxRecordedWaterLevel() {
        if (this.records.isEmpty()) {
            return 0.0;
        }
        double max = this.records.get(0).getWaterLevelMeters();
        for (WaterLevelRecord record : this.records) {
            if (record.getWaterLevelMeters() > max) {
                max = record.getWaterLevelMeters();
            }
        }
        return max;
    }

    public int getTotalStationsCount() {
        return this.stations.size();
    }

    public int getTotalReadingsCount() {
        return this.records.size();
    }

    /**
     * Estimates water level from a staff gauge image, logs the record, and persists to permanent storage.
     */
    public synchronized GaugeProcessingResult processAndRecordGaugeImage(String stationId, String imagePath, String timestamp)
            throws StationNotFoundException, InvalidWaterLevelException {
        RiverStation station = getStationByIdOrThrow(stationId);

        // Calibrate based on maximum gauge scale (default 20.0m or 1.25x danger level)
        double maxGaugeScale = Math.max(20.0, station.getDangerLevelMeters() * 1.25);
        GaugeProcessingResult result = this.imageProcessor.processGaugeImage(imagePath, maxGaugeScale);

        if (!result.isSuccess()) {
            return result;
        }

        double estimatedLevel = result.getEstimatedWaterLevelMeters();
        if (estimatedLevel < MIN_PERMISSIBLE_LEVEL || estimatedLevel > MAX_PERMISSIBLE_LEVEL) {
            throw new InvalidWaterLevelException(estimatedLevel);
        }

        String recordId = "REC-IMG-" + (1000 + this.records.size() + 1);
        WaterLevelRecord record = station.generateReading(recordId, estimatedLevel, timestamp + " [Image Analysis]");
        this.records.add(record);

        // Persist via DAO
        try {
            this.recordDAO.saveRecord(record);
        } catch (IOException e) {
            System.err.println("[Persistence Warning] Failed to persist image analysis reading: " + e.getMessage());
        }

        return result;
    }

    /**
     * Forces writing all in-memory stations and records back to permanent disk files
     */
    public synchronized void syncAllToStorage() throws IOException {
        this.stationDAO.saveAllStations(this.stations);
        this.recordDAO.saveAllRecords(this.records);
    }

    public StationDAO getStationDAO() {
        return stationDAO;
    }

    public WaterLevelRecordDAO getRecordDAO() {
        return recordDAO;
    }

    public WaterLevelImageProcessor getImageProcessor() {
        return imageProcessor;
    }
}
