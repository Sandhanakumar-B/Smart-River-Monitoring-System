package main;

import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import java.util.List;
import java.util.Scanner;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 5: Custom Exception Handling, Robust Input Validation, and Structured Error Recovery
 * Syllabus Unit: UNIT IV - Exception Handling, try-catch-finally, throws, custom exceptions
 */
public class Main {

    private static final RiverMonitoringService monitoringService = new RiverMonitoringService();
    private static RiverStation activeStation;

    public static void main(String[] args) {
        List<RiverStation> initialStations = monitoringService.getAllStations();
        if (!initialStations.isEmpty()) {
            activeStation = initialStations.get(0);
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printHeader();

        while (running) {
            displayMenu();
            System.out.print("Enter your choice (1-8): ");

            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                System.out.println();
                switch (choice) {
                    case 1:
                        displayAllStations();
                        break;
                    case 2:
                        switchActiveStation(scanner);
                        break;
                    case 3:
                        recordStationMeasurement(scanner);
                        break;
                    case 4:
                        registerNewStation(scanner);
                        break;
                    case 5:
                        displayReadingHistoryAndAnalytics();
                        break;
                    case 6:
                        displayCriticalFloodAlerts();
                        break;
                    case 7:
                        displaySystemStatus();
                        break;
                    case 8:
                        System.out.println("Exiting system. Thank you for using Smart River Water Level Monitoring!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 8.");
                }
            } else {
                System.out.println("\n[INPUT ERROR] Invalid format! Please enter a numerical menu option.");
                scanner.nextLine(); // clear invalid token
            }

            if (running) {
                System.out.println("\n------------------------------------------------------------");
            }
        }

        scanner.close();
    }

    private static void printHeader() {
        System.out.println("============================================================");
        System.out.println("   SMART RIVER WATER LEVEL MONITORING & DATA COLLECTION     ");
        System.out.println("               (Using Image Processing)                     ");
        System.out.println("============================================================");
        System.out.println("Academic Prototype - Core Java Console Edition (Day 5: Exception Handling)\n");
    }

    private static void displayMenu() {
        System.out.println("MAIN MENU:");
        System.out.println("1. View All River Monitoring Stations");
        System.out.println("2. Select / Switch Active Monitoring Station");
        System.out.println("3. Record Water Level for Active Station [" + (activeStation != null ? activeStation.getStationId() : "None") + "]");
        System.out.println("4. Register New River Monitoring Station (Custom Station)");
        System.out.println("5. View All Reading History & Basin Analytics");
        System.out.println("6. View Critical Flood Alert Records");
        System.out.println("7. System Architecture & Status");
        System.out.println("8. Exit");
    }

    private static void displayAllStations() {
        System.out.println("=== REGISTERED RIVER MONITORING STATIONS ===");
        List<RiverStation> stations = monitoringService.getAllStations();
        
        System.out.printf("%-12s %-32s %-14s %-12s %-12s\n", "Station ID", "Station Name", "River", "Normal(m)", "Danger(m)");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (RiverStation station : stations) {
            String activeFlag = (activeStation != null && activeStation.getStationId().equals(station.getStationId())) ? " [*ACTIVE*]" : "";
            System.out.printf("%-12s %-32s %-14s %-12.2f %-12.2f%s\n",
                station.getStationId(),
                station.getStationName(),
                station.getRiverName(),
                station.getNormalLevelMeters(),
                station.getDangerLevelMeters(),
                activeFlag
            );
        }
        System.out.println("\nTotal Stations Managed: " + monitoringService.getTotalStationsCount());
    }

    private static void switchActiveStation(Scanner scanner) {
        System.out.println("=== SWITCH ACTIVE MONITORING STATION ===");
        System.out.print("Enter Station ID to activate (e.g., STN-HAR-01, STN-RSH-02): ");
        String stationId = scanner.nextLine().trim();

        // UNIT IV: Structured Exception Handling with try-catch
        try {
            RiverStation found = monitoringService.getStationByIdOrThrow(stationId);
            activeStation = found;
            System.out.println("\n[SUCCESS] Active station switched to: " + found.getStationName() + " (" + found.getStationId() + ")");
        } catch (StationNotFoundException e) {
            System.out.println("\n[ERROR: StationNotFoundException] " + e.getMessage());
            System.out.println("Tip: Use Menu Option 1 to check the list of valid station IDs.");
        }
    }

    private static void recordStationMeasurement(Scanner scanner) {
        if (activeStation == null) {
            System.out.println("[ERROR] No active station selected! Please select a station first.");
            return;
        }

        System.out.println("=== RECORD WATER LEVEL FOR STATION: " + activeStation.getStationName() + " ===");
        System.out.printf("Permissible Range: %.1fm to %.1fm | Danger Threshold: %.2fm\n",
            RiverMonitoringService.MIN_PERMISSIBLE_LEVEL,
            RiverMonitoringService.MAX_PERMISSIBLE_LEVEL,
            activeStation.getDangerLevelMeters()
        );
        System.out.print("Enter measured water level in meters (e.g. 15.4): ");

        if (scanner.hasNextDouble()) {
            double level = scanner.nextDouble();
            scanner.nextLine(); // consume newline

            String timestamp = "2026-09-06 (Live Reading)";

            // UNIT IV: Catching domain-specific checked exceptions
            try {
                WaterLevelRecord record = monitoringService.recordMeasurement(activeStation.getStationId(), level, timestamp);
                System.out.println("\n[SUCCESS] Water level measurement verified & saved:");
                record.printRecordDetails();

                if (activeStation.isFloodRisk(level)) {
                    System.out.println(">>> 🚨 CRITICAL ALERT: Measured level exceeds Danger Threshold of " + activeStation.getDangerLevelMeters() + "m!");
                } else {
                    System.out.println(">>> ✅ STATUS: Water level is within safe operational limits.");
                }
            } catch (InvalidWaterLevelException e) {
                System.out.println("\n[VALIDATION FAILED: InvalidWaterLevelException]");
                System.out.println("Error Detail: " + e.getMessage());
                System.out.println("Cause: Readings below 0.0m or exceeding 50.0m are rejected as physically invalid.");
            } catch (StationNotFoundException e) {
                System.out.println("\n[ERROR: StationNotFoundException] " + e.getMessage());
            }
        } else {
            System.out.println("\n[INPUT ERROR] Invalid format! Please enter a numerical decimal value (e.g. 12.5).");
            scanner.nextLine(); // clear invalid token
        }
    }

    private static void registerNewStation(Scanner scanner) {
        System.out.println("=== REGISTER NEW RIVER MONITORING STATION ===");
        
        System.out.print("Enter Unique Station ID (e.g., STN-DEL-05): ");
        String stationId = scanner.nextLine().trim();

        System.out.print("Enter Station Name (e.g., Delhi Wazirabad Station): ");
        String stationName = scanner.nextLine().trim();

        System.out.print("Enter River Name (e.g., Yamuna River): ");
        String riverName = scanner.nextLine().trim();

        System.out.print("Enter Normal Baseline Level in meters (e.g., 8.5): ");
        if (!scanner.hasNextDouble()) {
            System.out.println("[INPUT ERROR] Invalid normal level! Registration aborted.");
            scanner.nextLine();
            return;
        }
        double normalLevel = scanner.nextDouble();

        System.out.print("Enter Danger Threshold in meters (e.g., 18.0): ");
        if (!scanner.hasNextDouble()) {
            System.out.println("[INPUT ERROR] Invalid danger level! Registration aborted.");
            scanner.nextLine();
            return;
        }
        double dangerLevel = scanner.nextDouble();
        scanner.nextLine(); // consume newline

        RiverStation newStation = new RiverStation(stationId, stationName, riverName, normalLevel, dangerLevel);

        // UNIT IV: Catching custom DuplicateStationException
        try {
            monitoringService.registerStation(newStation);
            System.out.println("\n[SUCCESS] New monitoring station registered successfully!");
            System.out.println("  " + newStation.toString());
        } catch (DuplicateStationException e) {
            System.out.println("\n[REGISTRATION FAILED: DuplicateStationException]");
            System.out.println("Error Detail: " + e.getMessage());
            System.out.println("Action: Please use a distinct station identifier.");
        } catch (IllegalArgumentException e) {
            System.out.println("\n[VALIDATION FAILED] " + e.getMessage());
        }
    }

    private static void displayReadingHistoryAndAnalytics() {
        System.out.println("=== WATER LEVEL READING HISTORY (COLLECTIONS LOG) ===");
        List<WaterLevelRecord> records = monitoringService.getAllRecords();

        if (records.isEmpty()) {
            System.out.println("No readings recorded yet.");
            return;
        }

        System.out.printf("%-10s %-14s %-32s %-12s %-25s\n", "Record ID", "River", "Station", "Level(m)", "Alert Status");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (WaterLevelRecord record : records) {
            System.out.printf("%-10s %-14s %-32s %-12.2f %-25s\n",
                record.getRecordId(),
                record.getRiverName(),
                record.getStationLocation(),
                record.getWaterLevelMeters(),
                record.getAlertStatus()
            );
        }

        System.out.println("\n--- BASIN ANALYTICS ---");
        System.out.println("Total Readings Logged   : " + monitoringService.getTotalReadingsCount());
        System.out.printf("Average Water Level     : %.2f meters\n", monitoringService.getAverageWaterLevel());
        System.out.printf("Peak Recorded Level     : %.2f meters\n", monitoringService.getMaxRecordedWaterLevel());
    }

    private static void displayCriticalFloodAlerts() {
        System.out.println("=== CRITICAL FLOOD & WARNING ALERTS ===");
        List<WaterLevelRecord> alerts = monitoringService.getCriticalAlertRecords();

        if (alerts.isEmpty()) {
            System.out.println("✅ All monitored stations report normal water levels. No active alerts.");
            return;
        }

        System.out.println("WARNING / CRITICAL RECORDS DETECTED (" + alerts.size() + " Found):");
        for (WaterLevelRecord alert : alerts) {
            System.out.println("  * " + alert.getRecordId() + " | " + alert.getStationLocation() +
                               " | Level: " + alert.getWaterLevelMeters() + "m | Status: " + alert.getAlertStatus());
        }
    }

    private static void displaySystemStatus() {
        System.out.println("================ SYSTEM ARCHITECTURE & STATUS ================");
        System.out.println("System Version : v0.5 (Day 5: Custom Exception Handling)");
        System.out.println("Architecture   : Layered (Model -> Service -> Main Presentation)");
        System.out.println("Exceptions     : Checked Domain Exceptions (InvalidWaterLevel, StationNotFound, DuplicateStation)");
        System.out.println("Active Station : " + (activeStation != null ? activeStation.getStationName() : "None"));
        System.out.println("Total Stations : " + monitoringService.getTotalStationsCount());
        System.out.println("Total Records  : " + monitoringService.getTotalReadingsCount());
        System.out.println("==============================================================");
    }
}
