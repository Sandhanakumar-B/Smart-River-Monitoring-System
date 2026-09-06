package service;

import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.RiverStation;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 5: Service Layer with Custom Exceptions & Robust Validation
 * Syllabus Unit: UNIT IV - Exception Handling (Checked Exceptions, throws, throw, Validation)
 */
public class RiverMonitoringService {

    public static final double MIN_PERMISSIBLE_LEVEL = 0.0;
    public static final double MAX_PERMISSIBLE_LEVEL = 50.0;

    private final List<RiverStation> stations;
    private final List<WaterLevelRecord> records;

    public RiverMonitoringService() {
        this.stations = new ArrayList<>();
        this.records = new ArrayList<>();
        initializeDefaultStations();
    }

    /**
     * Seeds initial river monitoring stations across key basin locations
     */
    private void initializeDefaultStations() {
        try {
            registerStation(new RiverStation(
                "STN-HAR-01",
                "Haridwar Central Gauge Station",
                "Ganga River",
                7.5,
                16.5
            ));

            registerStation(new RiverStation(
                "STN-RSH-02",
                "Rishikesh Barrage Station",
                "Ganga River",
                6.2,
                14.0
            ));

            registerStation(new RiverStation(
                "STN-KNP-03",
                "Kanpur Ghat Station",
                "Ganga River",
                8.0,
                17.5
            ));

            registerStation(new RiverStation(
                "STN-VRN-04",
                "Varanasi Assi Ghat Station",
                "Ganga River",
                9.1,
                18.2
            ));

            // Initial baseline readings
            recordMeasurement("STN-HAR-01", 8.2, "2026-09-01 08:00 AM");
            recordMeasurement("STN-RSH-02", 7.0, "2026-09-01 09:30 AM");
            recordMeasurement("STN-KNP-03", 17.8, "2026-09-01 11:15 AM");
        } catch (DuplicateStationException | StationNotFoundException | InvalidWaterLevelException e) {
            System.err.println("Warning during system bootstrapping: " + e.getMessage());
        }
    }

    /**
     * Registers a new monitoring station into the system.
     * Throws DuplicateStationException if station ID is already in use.
     */
    public void registerStation(RiverStation station) throws DuplicateStationException {
        if (station == null || station.getStationId() == null || station.getStationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Station and Station ID cannot be null or empty.");
        }

        if (getStationById(station.getStationId()) != null) {
            throw new DuplicateStationException(station.getStationId());
        }

        this.stations.add(station);
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
     * Validates and records a new water level measurement.
     * Throws StationNotFoundException if station does not exist.
     * Throws InvalidWaterLevelException if water level is below 0m or exceeds 50m.
     */
    public WaterLevelRecord recordMeasurement(String stationId, double levelMeters, String timestamp)
            throws StationNotFoundException, InvalidWaterLevelException {
        
        // Validate level constraints (UNIT IV - Throwing Custom Exception)
        if (levelMeters < MIN_PERMISSIBLE_LEVEL || levelMeters > MAX_PERMISSIBLE_LEVEL) {
            throw new InvalidWaterLevelException(levelMeters);
        }

        RiverStation station = getStationByIdOrThrow(stationId);

        String recordId = "REC-" + (1000 + this.records.size() + 1);
        WaterLevelRecord record = station.generateReading(recordId, levelMeters, timestamp);
        this.records.add(record);
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
}
