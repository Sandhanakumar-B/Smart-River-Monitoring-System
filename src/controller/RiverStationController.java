package controller;

import dao.jpa.WaterLevelRecordJpaDAO;
import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import java.util.List;
import java.util.Map;
import model.RiverStation;
import model.WaterLevelRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 14: Spring Data JPA & H2 Database Integration - Expanded REST API
 * Syllabus Unit: UNIT V - Spring @RestController, @RequestBody, @DeleteMapping, HTTP Status Codes
 *
 * REST Controller exposing all river monitoring endpoints.
 * Day 13 provided 3 GET endpoints; Day 14 adds POST, DELETE, and JPA-powered alert queries.
 */
@RestController
@RequestMapping("/api")
public class RiverStationController {

    private final RiverMonitoringService monitoringService;
    private final WaterLevelRecordJpaDAO jpaRecordDAO;

    @Autowired
    public RiverStationController(RiverMonitoringService monitoringService,
                                   WaterLevelRecordJpaDAO jpaRecordDAO) {
        this.monitoringService = monitoringService;
        this.jpaRecordDAO      = jpaRecordDAO;
    }

    // ======================================================================
    // Day 13 Endpoints (GET - preserved & backward compatible)
    // ======================================================================

    /**
     * GET /api/stations
     * Returns JSON array of all registered monitoring stations.
     */
    @GetMapping("/stations")
    public List<RiverStation> getAllStations() {
        return monitoringService.getAllStations();
    }

    /**
     * GET /api/readings/{stationId}
     * Returns historical water level records filtered by station ID.
     */
    @GetMapping("/readings/{stationId}")
    public ResponseEntity<?> getReadingsByStation(@PathVariable String stationId) {
        try {
            RiverStation station = monitoringService.getStationByIdOrThrow(stationId);
            List<WaterLevelRecord> stationRecords = monitoringService.getAllRecords().stream()
                    .filter(r -> r.getStationLocation().contains(station.getStationId())
                              || r.getStationLocation().contains(station.getStationName()))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(stationRecords);
        } catch (StationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/stats
     * Returns basin-wide analytics summary.
     */
    @GetMapping("/stats")
    public Map<String, Object> getBasinStats() {
        return Map.of(
            "totalStations",      monitoringService.getTotalStationsCount(),
            "totalReadings",      monitoringService.getTotalReadingsCount(),
            "averageWaterLevel",  monitoringService.getAverageWaterLevel(),
            "peakWaterLevel",     monitoringService.getMaxRecordedWaterLevel(),
            "criticalAlertsCount", monitoringService.getCriticalAlertRecords().size()
        );
    }

    // ======================================================================
    // Day 14 Endpoints (POST, DELETE, JPA-powered queries)
    // ======================================================================

    /**
     * POST /api/stations
     * Register a new river monitoring station via JSON request body.
     * Body example:
     * {
     *   "stationId": "STN-NEW-05",
     *   "stationName": "New Gorge Station",
     *   "riverName": "Yamuna River",
     *   "normalLevelMeters": 6.0,
     *   "dangerLevelMeters": 14.0
     * }
     */
    @PostMapping("/stations")
    public ResponseEntity<?> registerStation(@RequestBody RiverStation station) {
        try {
            monitoringService.registerStation(station);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Station registered successfully and persisted to JPA/H2 database.",
                "stationId", station.getStationId()
            ));
        } catch (DuplicateStationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Duplicate station ID: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/readings
     * Record a new water level measurement for a station via JSON request body.
     * Query params: stationId, level, timestamp
     * Example: POST /api/readings?stationId=STN-HAR-01&level=10.5&timestamp=2026-09-18+10:00+AM
     */
    @PostMapping("/readings")
    public ResponseEntity<?> recordMeasurement(@RequestParam String stationId,
                                                @RequestParam double level,
                                                @RequestParam(defaultValue = "Now") String timestamp) {
        try {
            WaterLevelRecord record = monitoringService.recordMeasurement(stationId, level, timestamp);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message",     "Reading recorded and persisted to JPA/H2 database.",
                "recordId",    record.getRecordId(),
                "alertStatus", record.getAlertStatus(),
                "levelMeters", record.getWaterLevelMeters()
            ));
        } catch (StationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Station not found: " + e.getMessage()));
        } catch (InvalidWaterLevelException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid water level: " + e.getMessage()));
        }
    }

    /**
     * GET /api/readings/alerts
     * Returns all CRITICAL and WARNING water level records using JPA derived query.
     * Demonstrates Spring Data JPA findByAlertStatusStartingWith() in action.
     */
    @GetMapping("/readings/alerts")
    public ResponseEntity<List<WaterLevelRecord>> getAlertReadings() {
        List<WaterLevelRecord> alerts = jpaRecordDAO.getCriticalAndWarningRecords();
        return ResponseEntity.ok(alerts);
    }

    /**
     * DELETE /api/stations/{stationId}
     * Removes a station from the system.
     * Demonstrates @DeleteMapping and 204 No Content HTTP response.
     */
    @DeleteMapping("/stations/{stationId}")
    public ResponseEntity<?> deleteStation(@PathVariable String stationId) {
        try {
            monitoringService.getStationByIdOrThrow(stationId); // verify exists first
            // Note: removal from in-memory service is beyond today's scope;
            // this shows the HTTP DELETE pattern and status codes.
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (StationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Station not found: " + stationId));
        }
    }
}
