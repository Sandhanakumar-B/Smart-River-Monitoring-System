package service;

import model.RiverStation;
import model.WaterLevelRecord;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 12: Advanced Data Analytics & Reporting
 * Focus: Java 8 Streams API, Lambdas, Optional, and Grouping Collectors
 */
public class RiverAnalyticsService {

    private final RiverMonitoringService monitoringService;

    public RiverAnalyticsService(RiverMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Uses max() with a Comparator to find the highest single water level reading safely.
     * Returns an Optional to handle cases where no records exist.
     */
    public Optional<WaterLevelRecord> findHighestRecordedLevel() {
        return monitoringService.getAllRecords().stream()
                .max(Comparator.comparingDouble(WaterLevelRecord::getWaterLevelMeters));
    }

    /**
     * Returns the top N highest readings by sorting in reverse order and applying a limit.
     */
    public List<WaterLevelRecord> getTopNHighestReadings(int n) {
        return monitoringService.getAllRecords().stream()
                .sorted(Comparator.comparingDouble(WaterLevelRecord::getWaterLevelMeters).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Groups all records by Station ID and computes the average water level for each station.
     */
    public Map<String, Double> getAverageWaterLevelByStation() {
        return monitoringService.getAllRecords().stream()
                .collect(Collectors.groupingBy(
                        WaterLevelRecord::getStationLocation,
                        Collectors.averagingDouble(WaterLevelRecord::getWaterLevelMeters)
                ));
    }

    /**
     * Uses map and distinct to find unique station names that have ever triggered a CRITICAL alert.
     */
    public List<String> getStationsWithCriticalAlerts() {
        return monitoringService.getAllRecords().stream()
                .filter(r -> r.getAlertStatus().startsWith("CRITICAL"))
                .map(WaterLevelRecord::getStationLocation)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Generates a comprehensive string report combining multiple streams analytics.
     */
    public String generateAnalyticsReport() {
        StringBuilder report = new StringBuilder();
        report.append("=========================================================\n");
        report.append("          DAY 12 ADVANCED ANALYTICS REPORT               \n");
        report.append("=========================================================\n\n");

        report.append(String.format("Total Stations: %d\n", monitoringService.getTotalStationsCount()));
        report.append(String.format("Total Readings: %d\n", monitoringService.getTotalReadingsCount()));
        report.append(String.format("Basin-wide Average Level: %.2f m\n\n", monitoringService.getAverageWaterLevel()));

        Optional<WaterLevelRecord> maxRec = findHighestRecordedLevel();
        report.append("--- Highest Single Reading ---\n");
        maxRec.ifPresentOrElse(
                r -> report.append(String.format("Level: %.2f m at %s (%s)\n\n", r.getWaterLevelMeters(), r.getStationLocation(), r.getTimestamp())),
                () -> report.append("No readings available.\n\n")
        );

        report.append("--- Average Water Level by Station ---\n");
        Map<String, Double> avgByStation = getAverageWaterLevelByStation();
        if (avgByStation.isEmpty()) {
            report.append("No data available.\n");
        } else {
            avgByStation.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> {
                        report.append(String.format("%-30s : %.2f m\n", entry.getKey(), entry.getValue()));
                    });
        }
        report.append("\n");

        report.append("--- Top 3 Highest Readings ---\n");
        List<WaterLevelRecord> top3 = getTopNHighestReadings(3);
        if (top3.isEmpty()) {
            report.append("No readings available.\n");
        } else {
            top3.forEach(r -> report.append(String.format("%.2f m at %s (%s)\n", r.getWaterLevelMeters(), r.getStationLocation(), r.getTimestamp())));
        }
        report.append("\n");

        report.append("--- Stations With Historical CRITICAL Alerts ---\n");
        List<String> criticalStations = getStationsWithCriticalAlerts();
        if (criticalStations.isEmpty()) {
            report.append("No critical alerts recorded.\n");
        } else {
            criticalStations.forEach(s -> report.append("- ").append(s).append("\n"));
        }

        report.append("\n=========================================================");
        return report.toString();
    }
}
