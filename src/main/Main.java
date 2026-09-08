package main;

import dao.StationFileDAO;
import dao.WaterLevelRecordFileDAO;
import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import imageprocessing.GaugeProcessingResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 7: Data Access Object (DAO) Pattern, CSV File Persistence & Full Layered Integration
 * Syllabus Unit: UNIT III, IV & V - File Streams, DAO Abstraction, Exception Recovery & Modular Design
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
            System.out.print("Enter your choice (1-10): ");

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
                        estimateWaterLevelFromImage(scanner);
                        break;
                    case 5:
                        registerNewStation(scanner);
                        break;
                    case 6:
                        displayReadingHistoryAndAnalytics();
                        break;
                    case 7:
                        displayCriticalFloodAlerts();
                        break;
                    case 8:
                        manageStorageAndPersistence(scanner);
                        break;
                    case 9:
                        displaySystemStatus();
                        break;
                    case 10:
                        System.out.println("Exiting system. All records safely persisted via DAO. Thank you!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 10.");
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
        System.out.println("Academic Prototype - Core Java Console Edition (Day 7: DAO & File Persistence)\n");
    }

    private static void displayMenu() {
        System.out.println("MAIN MENU:");
        System.out.println("1. View All River Monitoring Stations");
        System.out.println("2. Select / Switch Active Monitoring Station");
        System.out.println("3. Record Water Level Manually for Active Station [" + (activeStation != null ? activeStation.getStationId() : "None") + "]");
        System.out.println("4. 📷 Estimate Water Level via Gauge Image Processing (Computer Vision)");
        System.out.println("5. Register New River Monitoring Station (Custom Station)");
        System.out.println("6. View All Reading History & Basin Analytics");
        System.out.println("7. View Critical Flood Alert Records");
        System.out.println("8. 💾 DAO Storage & Data Persistence Management (CSV Inspection / Reload)");
        System.out.println("9. System Architecture & Status");
        System.out.println("10. Exit");
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
    }

    private static void switchActiveStation(Scanner scanner) {
        System.out.println("=== SWITCH ACTIVE MONITORING STATION ===");
        System.out.print("Enter Station ID to activate (e.g., STN-HAR-01): ");
        String targetId = scanner.nextLine().trim();

        // UNIT IV: Catching custom StationNotFoundException
        try {
            RiverStation selected = monitoringService.getStationByIdOrThrow(targetId);
            activeStation = selected;
            System.out.println("\n[SUCCESS] Active station updated to: " + activeStation.getStationName() + " (" + activeStation.getStationId() + ")");
            activeStation.printStationSummary();
        } catch (StationNotFoundException e) {
            System.out.println("\n[ERROR: StationNotFoundException] " + e.getMessage());
            System.out.println("Hint: Use Menu Option 1 to view all valid station IDs.");
        }
    }

    private static void recordStationMeasurement(Scanner scanner) {
        System.out.println("=== MANUAL WATER LEVEL MEASUREMENT ENTRY ===");
        if (activeStation == null) {
            System.out.println("[ERROR] No active station selected! Please choose a station first (Option 2).");
            return;
        }

        System.out.println("Target Station: " + activeStation.getStationName() + " (" + activeStation.getStationId() + ")");
        System.out.println("Thresholds    : Normal Baseline = " + activeStation.getNormalLevelMeters() + "m | Danger Alert = " + activeStation.getDangerLevelMeters() + "m");
        System.out.print("Enter measured water level in meters (0.0 to 50.0): ");

        if (!scanner.hasNextDouble()) {
            System.out.println("\n[INPUT ERROR] Invalid numeric format for water level.");
            scanner.nextLine();
            return;
        }

        double level = scanner.nextDouble();
        scanner.nextLine(); // consume newline

        System.out.print("Enter timestamp (e.g., 2026-09-08 11:30 AM): ");
        String timestamp = scanner.nextLine().trim();
        if (timestamp.isEmpty()) {
            timestamp = "2026-09-08 11:30 AM";
        }

        // UNIT IV: Catching domain exceptions (InvalidWaterLevelException & StationNotFoundException)
        try {
            WaterLevelRecord record = monitoringService.recordMeasurement(activeStation.getStationId(), level, timestamp);
            System.out.println("\n[SUCCESS] Water level measurement recorded and saved to storage via DAO!");
            System.out.println("  " + record.toString());

            // Real-time danger evaluation
            if (activeStation.isFloodRisk(level)) {
                System.out.println("\n⚠️  [HIGH FLOOD WARNING TRIGGERED] ⚠️");
                System.out.printf("  Water level (%.2fm) has breached Station Danger Threshold (%.2fm)!\n",
                    level, activeStation.getDangerLevelMeters());
                System.out.println("  Immediate automated notification dispatched to basin flood authority.");
            }
        } catch (InvalidWaterLevelException e) {
            System.out.println("\n[VALIDATION FAILED: InvalidWaterLevelException]");
            System.out.println("Error Detail: " + e.getMessage());
            System.out.println("Measurement was rejected to safeguard data integrity.");
        } catch (StationNotFoundException e) {
            System.out.println("\n[SYSTEM ERROR: StationNotFoundException] " + e.getMessage());
        }
    }

    private static void estimateWaterLevelFromImage(Scanner scanner) {
        System.out.println("=== 📷 ESTIMATE WATER LEVEL VIA IMAGE PROCESSING (COMPUTER VISION) ===");
        if (activeStation == null) {
            System.out.println("[ERROR] No active station selected! Please choose a station first (Option 2).");
            return;
        }

        System.out.println("Active Station: " + activeStation.getStationName() + " (" + activeStation.getStationId() + ")");
        System.out.println("Select Gauge Image Source:");
        System.out.println("  1. Benchmark Normal Image  ('images/gauge_normal.png'  ~8.5m target)");
        System.out.println("  2. Benchmark Warning Image ('images/gauge_warning.png' ~14.0m target)");
        System.out.println("  3. Benchmark Flood Image   ('images/gauge_flood.png'   ~17.5m target)");
        System.out.println("  4. Custom Image File Path");
        System.out.print("Choose option (1-4): ");

        String imagePath = "images/gauge_normal.png";
        if (scanner.hasNextInt()) {
            int imgChoice = scanner.nextInt();
            scanner.nextLine();
            switch (imgChoice) {
                case 1:
                    imagePath = "images/gauge_normal.png";
                    break;
                case 2:
                    imagePath = "images/gauge_warning.png";
                    break;
                case 3:
                    imagePath = "images/gauge_flood.png";
                    break;
                case 4:
                    System.out.print("Enter full or relative image file path: ");
                    imagePath = scanner.nextLine().trim();
                    break;
                default:
                    System.out.println("Invalid choice. Defaulting to 'images/gauge_normal.png'.");
            }
        } else {
            scanner.nextLine();
            System.out.println("Invalid input. Defaulting to 'images/gauge_normal.png'.");
        }

        File imgFile = new File(imagePath);
        if (!imgFile.exists()) {
            System.out.println("\n[FILE ERROR] Image file does not exist: " + imgFile.getAbsolutePath());
            System.out.println("Please run 'GenerateBenchmarkImages' or check the image path.");
            return;
        }

        System.out.println("\nExecuting Computer Vision Pipeline on: " + imagePath);
        System.out.println(" [Step 1] Loading image into memory buffer (java.awt.image.BufferedImage)...");
        System.out.println(" [Step 2] Applying vertical luminance gradient scan & multi-column flank boundary detection...");
        System.out.println(" [Step 3] Calibrating detected waterline coordinate to physical river gauge scale...");

        String timestamp = "2026-09-08 11:30 AM";
        try {
            GaugeProcessingResult result = monitoringService.processAndRecordGaugeImage(
                activeStation.getStationId(),
                imagePath,
                timestamp
            );

            if (result.isSuccess()) {
                System.out.println("\n================ IMAGE PROCESSING REPORT ================");
                System.out.println(" Source File         : " + result.getImagePath());
                System.out.println(" Waterline Detected  : Y = " + result.getDetectedWaterLineY() + " px (Image Height: " + result.getImageHeight() + " px)");
                System.out.printf(" Estimated Water Level: %.2f meters\n", result.getEstimatedWaterLevelMeters());
                System.out.printf(" Detection Confidence: %.1f%%\n", result.getConfidencePercent());
                System.out.println(" Algorithm Pipeline  : " + result.getAlgorithmSummary());
                System.out.println(" Persistent Storage  : Auto-saved to 'data/readings.csv' via DAO");
                System.out.println("==========================================================");

                // Alert Check
                if (activeStation.isFloodRisk(result.getEstimatedWaterLevelMeters())) {
                    System.out.println("\n🚨 [IMAGE ANALYSIS ALERT: FLOOD HAZARD DETECTED] 🚨");
                    System.out.printf("  Estimated level (%.2fm) has breached danger threshold (%.2fm) at %s!\n",
                        result.getEstimatedWaterLevelMeters(), activeStation.getDangerLevelMeters(), activeStation.getStationName());
                } else {
                    System.out.println("✅ Status: River level within safe operational parameters.");
                }
            } else {
                System.out.println("\n[IMAGE PROCESSING FAILED]");
                System.out.println("Diagnostics: " + result.getAlgorithmSummary());
            }

        } catch (InvalidWaterLevelException e) {
            System.out.println("\n[VALIDATION FAILED: InvalidWaterLevelException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("\n[ERROR: StationNotFoundException] " + e.getMessage());
        }
    }

    private static void registerNewStation(Scanner scanner) {
        System.out.println("=== REGISTER NEW RIVER MONITORING STATION ===");
        System.out.print("Enter Station Identifier (e.g., STN-YAM-05): ");
        String stationId = scanner.nextLine().trim();

        System.out.print("Enter Station Location Name: ");
        String stationName = scanner.nextLine().trim();

        System.out.print("Enter Monitored River Name: ");
        String riverName = scanner.nextLine().trim();

        System.out.print("Enter Normal Baseline Level (meters): ");
        if (!scanner.hasNextDouble()) {
            System.out.println("[INPUT ERROR] Invalid numeric value for normal level.");
            scanner.nextLine();
            return;
        }
        double normalLevel = scanner.nextDouble();

        System.out.print("Enter Danger Flood Threshold (meters): ");
        if (!scanner.hasNextDouble()) {
            System.out.println("[INPUT ERROR] Invalid numeric value for danger level.");
            scanner.nextLine();
            return;
        }
        double dangerLevel = scanner.nextDouble();
        scanner.nextLine(); // consume newline

        RiverStation newStation = new RiverStation(stationId, stationName, riverName, normalLevel, dangerLevel);

        // UNIT IV: Catching custom DuplicateStationException
        try {
            monitoringService.registerStation(newStation);
            System.out.println("\n[SUCCESS] New monitoring station registered successfully and persisted to disk!");
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
        System.out.println("=== WATER LEVEL READING HISTORY (COLLECTIONS & STORAGE LOG) ===");
        List<WaterLevelRecord> records = monitoringService.getAllRecords();

        if (records.isEmpty()) {
            System.out.println("No readings recorded yet.");
            return;
        }

        System.out.printf("%-14s %-14s %-36s %-10s %-25s\n", "Record ID", "River", "Station", "Level(m)", "Alert Status");
        System.out.println("----------------------------------------------------------------------------------------------------------");
        for (WaterLevelRecord record : records) {
            System.out.printf("%-14s %-14s %-36s %-10.2f %-25s\n",
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

    private static void manageStorageAndPersistence(Scanner scanner) {
        System.out.println("=== 💾 DAO STORAGE & DATA PERSISTENCE MANAGEMENT ===");
        System.out.println("1. View Physical Storage File Status & Statistics");
        System.out.println("2. Preview Raw 'data/stations.csv' File Content");
        System.out.println("3. Preview Raw 'data/readings.csv' File Content");
        System.out.println("4. Reload In-Memory Cache from Permanent Storage (StationFileDAO & WaterLevelRecordFileDAO)");
        System.out.println("5. Force Complete Flush / Resync to Storage");
        System.out.print("Select an option (1-5): ");

        if (!scanner.hasNextInt()) {
            System.out.println("[INPUT ERROR] Invalid selection.");
            scanner.nextLine();
            return;
        }

        int choice = scanner.nextInt();
        scanner.nextLine();

        File stationsFile = new File(StationFileDAO.DEFAULT_FILE_PATH);
        File readingsFile = new File(WaterLevelRecordFileDAO.DEFAULT_FILE_PATH);

        switch (choice) {
            case 1:
                System.out.println("\n--- PERMANENT STORAGE FILE HEALTH ---");
                printFileInfo("Stations CSV", stationsFile);
                printFileInfo("Readings CSV", readingsFile);
                break;

            case 2:
                System.out.println("\n--- RAW CONTENT: " + stationsFile.getPath() + " ---");
                displayRawFile(stationsFile);
                break;

            case 3:
                System.out.println("\n--- RAW CONTENT: " + readingsFile.getPath() + " ---");
                displayRawFile(readingsFile);
                break;

            case 4:
                System.out.println("\nReloading data from permanent disk storage via DAO...");
                monitoringService.loadDataFromStorage();
                List<RiverStation> reloaded = monitoringService.getAllStations();
                if (!reloaded.isEmpty()) {
                    activeStation = reloaded.get(0);
                }
                System.out.println("[SUCCESS] Reload complete!");
                System.out.println("Stations in memory: " + monitoringService.getTotalStationsCount());
                System.out.println("Readings in memory: " + monitoringService.getTotalReadingsCount());
                break;

            case 5:
                try {
                    System.out.println("\nFlushing in-memory collections to disk storage via DAO...");
                    monitoringService.syncAllToStorage();
                    System.out.println("[SUCCESS] All records and stations flushed successfully!");
                } catch (IOException e) {
                    System.out.println("[ERROR] Failed to flush to storage: " + e.getMessage());
                }
                break;

            default:
                System.out.println("Invalid selection.");
        }
    }

    private static void printFileInfo(String label, File file) {
        System.out.printf(" [%s]\n", label);
        System.out.println("   Path   : " + file.getAbsolutePath());
        System.out.println("   Exists : " + file.exists());
        if (file.exists()) {
            System.out.println("   Size   : " + file.length() + " bytes");
            System.out.println("   Lines  : " + countLines(file));
        }
    }

    private static int countLines(File file) {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) lines++;
        } catch (IOException e) {
            return -1;
        }
        return lines;
    }

    private static void displayRawFile(File file) {
        if (!file.exists()) {
            System.out.println("[NOT FOUND] File does not exist yet.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                System.out.printf(" %3d | %s\n", lineNum++, line);
            }
        } catch (IOException e) {
            System.out.println("[READ ERROR] " + e.getMessage());
        }
    }

    private static void displaySystemStatus() {
        System.out.println("================ SYSTEM ARCHITECTURE & STATUS ================");
        System.out.println("System Version : v0.7 (Day 7: DAO Pattern & CSV File Persistence Active)");
        System.out.println("Architecture   : Layered (Model -> DAO -> Service -> ImageProcessing -> Presentation)");
        System.out.println("DAO Layer      : StationDAO, StationFileDAO, WaterLevelRecordDAO, WaterLevelRecordFileDAO");
        System.out.println("Storage Media  : data/stations.csv & data/readings.csv");
        System.out.println("Image Engine   : Region of Interest (ROI) Edge Detection & Pixel Calibration");
        System.out.println("Exceptions     : Checked Domain Exceptions (InvalidWaterLevel, StationNotFound, DuplicateStation)");
        System.out.println("Active Station : " + (activeStation != null ? activeStation.getStationName() : "None"));
        System.out.println("Total Stations : " + monitoringService.getTotalStationsCount());
        System.out.println("Total Records  : " + monitoringService.getTotalReadingsCount());
        System.out.println("==============================================================");
    }
}
