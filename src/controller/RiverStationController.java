package controller;

import exception.StationNotFoundException;
import java.util.List;
import java.util.Map;
import model.RiverStation;
import model.WaterLevelRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.RiverMonitoringService;

@RestController
@RequestMapping("/api")
public class RiverStationController {

    private final RiverMonitoringService monitoringService;

    @Autowired
    public RiverStationController(RiverMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/stations")
    public List<RiverStation> getAllStations() {
        return monitoringService.getAllStations();
    }

    @GetMapping("/readings/{stationId}")
    public ResponseEntity<?> getReadingsByStation(@PathVariable String stationId) {
        try {
            RiverStation station = monitoringService.getStationByIdOrThrow(stationId);
            // Filter all records whose stationLocation contains this station's name or ID
            List<WaterLevelRecord> stationRecords = monitoringService.getAllRecords().stream()
                    .filter(r -> r.getStationLocation().contains(station.getStationId())
                              || r.getStationLocation().contains(station.getStationName()))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(stationRecords);
        } catch (StationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getBasinStats() {
        return Map.of(
            "totalStations", monitoringService.getTotalStationsCount(),
            "totalReadings", monitoringService.getTotalReadingsCount(),
            "averageWaterLevel", monitoringService.getAverageWaterLevel(),
            "peakWaterLevel", monitoringService.getMaxRecordedWaterLevel(),
            "criticalAlertsCount", monitoringService.getCriticalAlertRecords().size()
        );
    }
}
