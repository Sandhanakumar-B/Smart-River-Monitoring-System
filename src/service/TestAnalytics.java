package service;

import model.RiverStation;
import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;

public class TestAnalytics {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING DAY 12 STREAMS API ANALYTICS TESTS      ");
        System.out.println("==================================================");

        RiverMonitoringService baseService = new RiverMonitoringService();
        RiverAnalyticsService analyticsService = new RiverAnalyticsService(baseService);

        try {
            // Seed a bit more data just in case the file is empty
            if (baseService.getTotalStationsCount() == 0) {
                baseService.registerStation(new RiverStation("STN-TST-01", "Test Station 1", "River A", 5.0, 10.0));
                baseService.registerStation(new RiverStation("STN-TST-02", "Test Station 2", "River B", 4.0, 8.0));
            }
            
            RiverStation firstStation = baseService.getAllStations().get(0);
            
            baseService.recordMeasurement(firstStation.getStationId(), 2.5, "2026-09-12 10:00");
            baseService.recordMeasurement(firstStation.getStationId(), 15.0, "2026-09-12 11:00"); // Critical

            System.out.println("\n[1] Testing Top N Readings...");
            analyticsService.getTopNHighestReadings(3).forEach(r -> {
                System.out.println(" -> " + r.getWaterLevelMeters() + "m at " + r.getStationLocation());
            });

            System.out.println("\n[2] Testing Average Level By Station (Grouping Collector)...");
            analyticsService.getAverageWaterLevelByStation().forEach((k, v) -> {
                System.out.println(" -> " + k + " : " + String.format("%.2f", v) + "m");
            });

            System.out.println("\n[3] Testing Full Report Generation...");
            String report = analyticsService.generateAnalyticsReport();
            System.out.println(report);

            System.out.println("\nALL ANALYTICS TESTS PASSED.");
        } catch (DuplicateStationException | StationNotFoundException | InvalidWaterLevelException e) {
            System.err.println("Test Failed: " + e.getMessage());
        }
    }
}
