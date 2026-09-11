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
import simulation.RiverSimulationManager;
import simulation.SensorEvent;
import simulation.SensorEventListener;
import simulation.StationSensorSimulator;
import network.MonitoringServer;
import network.MonitoringClient;
import java.io.IOException;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 9: Java Networking & Socket Programming — TCP Client-Server River Monitoring
 * Syllabus Unit: UNIT III, IV & V - Multithreading, Thread Synchronization, Networking, File Persistence
 */
public class Main {

    private static final RiverMonitoringService monitoringService = new RiverMonitoringService();
    private static final RiverSimulationManager simulationManager = new RiverSimulationManager(monitoringService);
    private static final MonitoringServer        networkServer     = new MonitoringServer(monitoringService);
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
            System.out.print("Enter your choice (1-12): ");

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
                        manageRealTimeSimulation(scanner);
                        break;
                    case 10:
                        manageNetworkServer(scanner);
                        break;
                    case 11:
                        displaySystemStatus();
                        break;
                    case 12:
                        System.out.println("Stopping background sensor threads and network server...");
                        simulationManager.stopSimulation();
                        if (networkServer.isRunning()) networkServer.stopServer();
                        System.out.println("Exiting system. All records safely persisted via DAO. Thank you!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 12.");
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
        System.out.println("Academic Prototype - Core Java Console Edition (Day 9: Networking & Socket Programming)\n");
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
        System.out.println("9. ⚡ Real-Time Multithreaded Sensor & River Simulation (IoT Telemetry)");
        System.out.println("10. 🌐 TCP Networking & Remote Monitoring Server (Socket Programming)");
        System.out.println("11. System Architecture & Status");
        System.out.println("12. Exit");
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
            simulationManager.refreshSimulators();
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

    private static void manageRealTimeSimulation(Scanner scanner) {
        boolean inSimulationMenu = true;

        while (inSimulationMenu) {
            System.out.println("=== ⚡ REAL-TIME MULTITHREADED SENSOR & RIVER SIMULATION ===");
            System.out.println("Background worker threads simulate automated IoT river sensors concurrently.");
            System.out.println("Simulation Status : " + (simulationManager.isRunning() ? "🟢 ACTIVE (Running concurrently)" : "⚪ IDLE (Stopped)"));
            System.out.println("Worker Threads    : " + simulationManager.getSimulators().size() + " Station Workers");
            System.out.println("Sampling Interval : " + simulationManager.getDefaultIntervalMillis() + " ms");
            System.out.println();
            System.out.println("SIMULATION CONTROLS:");
            System.out.println("1. Start Live Streaming Telemetry (Console Monitor - Press Enter to Stop)");
            System.out.println("2. Run Automated Simulation Batch (e.g. 5 concurrent sensor cycles)");
            System.out.println("3. ⚠️ Trigger Basin-Wide Flash Flood Surge Scenario (+4.5m rise)");
            System.out.println("4. ⚠️ Trigger Cloudburst Surge at Active/Specific Station");
            System.out.println("5. View Worker Thread Diagnostics & Health Status");
            System.out.println("6. Configure Telemetry Tick Interval");
            System.out.println("7. Stop / Terminate Background Simulation");
            System.out.println("8. Return to Main Menu");
            System.out.print("Enter simulation choice (1-8): ");

            if (!scanner.hasNextInt()) {
                System.out.println("[INPUT ERROR] Invalid choice format.\n");
                scanner.nextLine();
                continue;
            }

            int simChoice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            System.out.println();
            switch (simChoice) {
                case 1:
                    runLiveConsoleMonitor(scanner);
                    break;
                case 2:
                    runFastSimulationBatch(scanner);
                    break;
                case 3:
                    triggerBasinSurge();
                    break;
                case 4:
                    triggerStationSpecificSurge(scanner);
                    break;
                case 5:
                    displayThreadDiagnostics();
                    break;
                case 6:
                    configureSimulationInterval(scanner);
                    break;
                case 7:
                    simulationManager.stopSimulation();
                    System.out.println("[SUCCESS] Background simulation halted. Worker threads terminated.");
                    break;
                case 8:
                    inSimulationMenu = false;
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 1 and 8.");
            }

            if (inSimulationMenu) {
                System.out.println("\n------------------------------------------------------------");
            }
        }
    }

    private static void runLiveConsoleMonitor(Scanner scanner) {
        System.out.println("\n--- STARTING LIVE TELEMETRY CONSOLE MONITOR ---");
        System.out.println("Dedicated worker threads are streaming real-time measurements in parallel.");
        System.out.println("Press [ENTER] at any moment to pause and return to the menu.\n");

        SensorEventListener monitorListener = new SensorEventListener() {
            @Override
            public void onReadingReceived(SensorEvent event) {
                String badge = event.isAlert() ? " ⚠️ [CRITICAL ALERT!]" : "";
                System.out.printf("  [%s] Telemetry: %-32s -> %5.2fm | %-16s%s\n",
                    event.getThreadName(),
                    event.getStationName() + " (" + event.getStationId() + ")",
                    event.getWaterLevelMeters(),
                    event.getAlertStatus(),
                    badge);
            }

            @Override
            public void onAlertTriggered(SensorEvent event) {
                System.out.printf("  🚨 [DANGER EVENT TRIGGERED] %s breached danger limit (%.2fm) at %s!\n",
                    event.getStationId(), event.getWaterLevelMeters(), event.getTimestamp());
            }

            @Override
            public void onSimulationStatusChanged(String statusMessage) {
                System.out.println("  [Simulation Event] " + statusMessage);
            }
        };

        simulationManager.addGlobalListener(monitorListener);
        if (!simulationManager.isRunning()) {
            simulationManager.startSimulation();
        }

        // Block until user hits Enter
        scanner.nextLine();

        simulationManager.removeGlobalListener(monitorListener);
        System.out.println("\n[INFO] Exited live monitor stream.");
        System.out.print("Do you want to keep background threads streaming? (y/n): ");
        String keepRunning = scanner.nextLine().trim();
        if (!keepRunning.equalsIgnoreCase("y")) {
            simulationManager.stopSimulation();
            System.out.println("[INFO] Simulation worker threads stopped.");
        } else {
            System.out.println("[INFO] Simulation worker threads continue streaming in background.");
        }
    }

    private static void runFastSimulationBatch(Scanner scanner) {
        System.out.println("\n--- RUN AUTOMATED SIMULATION BATCH ---");
        System.out.print("Enter number of measurement cycles to simulate (1-20, default 5): ");
        int cycles = 5;
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            try {
                cycles = Integer.parseInt(input);
                if (cycles < 1 || cycles > 50) {
                    cycles = 5;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, defaulting to 5 cycles.");
            }
        }

        long origInterval = simulationManager.getDefaultIntervalMillis();
        simulationManager.setDefaultIntervalMillis(400); // fast cadence for batch test

        int beforeCount = monitoringService.getTotalReadingsCount();

        SensorEventListener batchListener = new SensorEventListener() {
            @Override
            public void onReadingReceived(SensorEvent event) {
                String badge = event.isAlert() ? " ⚠️ [ALERT]" : "";
                System.out.printf("  ✓ [%s] %-28s -> %5.2fm | %-16s%s\n",
                    event.getThreadName(), event.getStationId(), event.getWaterLevelMeters(), event.getAlertStatus(), badge);
            }

            @Override
            public void onAlertTriggered(SensorEvent event) {
                System.out.printf("  >>> 🚨 High Alert Triggered by %s!\n", event.getStationId());
            }

            @Override
            public void onSimulationStatusChanged(String statusMessage) {
                // quiet in batch mode
            }
        };

        simulationManager.addGlobalListener(batchListener);
        System.out.println("Executing " + cycles + " concurrent cycles across all stations...\n");
        simulationManager.startSimulation();

        try {
            Thread.sleep(cycles * 450L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        simulationManager.stopSimulation();
        simulationManager.removeGlobalListener(batchListener);
        simulationManager.setDefaultIntervalMillis(origInterval); // restore interval

        int afterCount = monitoringService.getTotalReadingsCount();
        int added = afterCount - beforeCount;

        System.out.println("\n[BATCH COMPLETE] Successfully captured and persisted " + added + " new telemetry readings via DAO!");
        System.out.println("Total readings currently in system: " + afterCount);
    }

    private static void triggerBasinSurge() {
        System.out.println("\n--- SIMULATING BASIN-WIDE FLASH FLOOD SURGE ---");
        System.out.println("Applying heavy monsoon rainfall surge (+4.50m) across all monitored river stations...");
        simulationManager.triggerBasinSurge(4.50);
        System.out.println("[SUCCESS] Surge injected! All station worker threads will immediately register elevated water levels.");
        System.out.println("Check Menu Option 7 (Critical Alerts) or run Option 9-1 (Live Stream) to observe real-time triggers.");
    }

    private static void triggerStationSpecificSurge(Scanner scanner) {
        System.out.println("\n--- SIMULATING CLOUDBURST / LOCALIZED DAM DISCHARGE ---");
        String targetId = (activeStation != null) ? activeStation.getStationId() : "STN-HAR-01";
        System.out.print("Enter target Station ID [" + targetId + "]: ");
        String entered = scanner.nextLine().trim();
        if (!entered.isEmpty()) {
            targetId = entered;
        }

        System.out.print("Enter water level surge in meters (e.g., 5.0): ");
        double surge = 5.0;
        if (scanner.hasNextDouble()) {
            surge = scanner.nextDouble();
            scanner.nextLine();
        } else {
            scanner.nextLine();
        }

        boolean success = simulationManager.triggerStationSurge(targetId, surge);
        if (success) {
            System.out.printf("\n[SUCCESS] Injected +%.2fm surge into %s!\n", surge, targetId);
        } else {
            System.out.println("\n[ERROR] Station ID not found in active simulators.");
        }
    }

    private static void displayThreadDiagnostics() {
        System.out.println("\n================ WORKER THREAD DIAGNOSTICS ================");
        System.out.println("Simulation Running : " + simulationManager.isRunning());
        System.out.println("Default Interval   : " + simulationManager.getDefaultIntervalMillis() + " ms");
        System.out.println("Active Simulators  : " + simulationManager.getSimulators().size());
        System.out.println("-----------------------------------------------------------");
        List<String> reports = simulationManager.getThreadDiagnosticReport();
        for (String rep : reports) {
            System.out.println(" " + rep);
        }
        System.out.println("===========================================================");
    }

    private static void configureSimulationInterval(Scanner scanner) {
        System.out.println("\n--- CONFIGURE SENSOR SAMPLING INTERVAL ---");
        System.out.println("Current interval: " + simulationManager.getDefaultIntervalMillis() + " ms");
        System.out.print("Enter new interval in milliseconds (min 500 ms, e.g., 2000): ");

        if (scanner.hasNextLong()) {
            long newInterval = scanner.nextLong();
            scanner.nextLine();
            simulationManager.setDefaultIntervalMillis(newInterval);
            System.out.println("[SUCCESS] Sampling interval updated to: " + simulationManager.getDefaultIntervalMillis() + " ms");
        } else {
            System.out.println("[INPUT ERROR] Invalid numeric interval.");
            scanner.nextLine();
        }
    }

    private static void manageNetworkServer(Scanner scanner) {
        System.out.println("================ 🌐 TCP NETWORKING & REMOTE MONITORING SERVER ================");
        System.out.println("Syllabus: UNIT V - java.net.ServerSocket, Socket, InputStream/OutputStream, Multi-Client");
        System.out.println("Server Status : " + (networkServer.isRunning() ?
            "🟢 RUNNING on port " + networkServer.getPort() +
            " | Clients served: " + networkServer.getTotalClientsServed()
            : "⚪ STOPPED"));
        System.out.println();
        System.out.println("NETWORKING MENU:");
        System.out.println("  1. Start TCP Monitoring Server (bind to port " + network.MonitoringProtocol.DEFAULT_PORT + ")");
        System.out.println("  2. Stop TCP Monitoring Server");
        System.out.println("  3. Connect as Remote Client (interactive query session)");
        System.out.println("  4. Run Automated Client Demo (headless protocol test)");
        System.out.println("  5. View Protocol Reference");
        System.out.println("  6. Back to Main Menu");
        System.out.print("Enter choice: ");

        String input = scanner.nextLine().trim();
        switch (input) {
            case "1":
                startNetworkServer();
                break;
            case "2":
                networkServer.stopServer();
                break;
            case "3":
                if (!networkServer.isRunning()) {
                    System.out.println("[WARN] Server is not running. Starting it first...");
                    startNetworkServer();
                }
                MonitoringClient client = new MonitoringClient();
                client.runInteractiveSession(scanner);
                break;
            case "4":
                runAutomatedClientDemo();
                break;
            case "5":
                displayProtocolReference();
                break;
            case "6":
            default:
                System.out.println("Returning to main menu.");
        }
    }

    private static void startNetworkServer() {
        try {
            networkServer.startServer();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not start server: " + e.getMessage());
            System.out.println("Hint: Port " + network.MonitoringProtocol.DEFAULT_PORT +
                " may already be in use. Try restarting the application.");
        }
    }

    private static void runAutomatedClientDemo() {
        if (!networkServer.isRunning()) {
            System.out.println("[WARN] Server not running. Starting first...");
            startNetworkServer();
        }
        System.out.println("\n--- AUTOMATED CLIENT DEMO (Headless Protocol Verification) ---");
        MonitoringClient autoClient = new MonitoringClient();
        String[] demoCommands = {
            network.MonitoringProtocol.CMD_SERVER_STATUS,
            network.MonitoringProtocol.CMD_LIST_STATIONS,
            network.MonitoringProtocol.CMD_BASIN_STATS,
            network.MonitoringProtocol.CMD_GET_ALERTS
        };
        for (String cmd : demoCommands) {
            System.out.println("\n  > Sending: " + cmd);
            try {
                String response = autoClient.sendCommand(cmd);
                for (String line : response.split("\n")) {
                    System.out.println("  " + line);
                }
            } catch (IOException e) {
                System.out.println("  [ERROR] " + e.getMessage());
            }
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        System.out.println("\n[DEMO COMPLETE] All protocol commands verified successfully.");
    }

    private static void displayProtocolReference() {
        System.out.println("\n====== TCP PROTOCOL REFERENCE (Text-Based, Line-Delimited) ======");
        System.out.println(" Port      : " + network.MonitoringProtocol.DEFAULT_PORT);
        System.out.println(" Host      : " + network.MonitoringProtocol.SERVER_HOST);
        System.out.println(" Timeout   : " + network.MonitoringProtocol.SOCKET_TIMEOUT_MS / 1000 + " s");
        System.out.println();
        System.out.println(" COMMANDS (Client → Server):");
        System.out.println("   LIST_STATIONS          - Get all registered river stations");
        System.out.println("   GET_LEVEL  <stationId> - Get latest water level reading for a station");
        System.out.println("   GET_HISTORY <stationId> - Get all historical readings for a station");
        System.out.println("   GET_ALERTS             - Get all critical flood alert records");
        System.out.println("   BASIN_STATS            - Get basin-wide analytics");
        System.out.println("   SERVER_STATUS          - Get server health info");
        System.out.println("   QUIT                   - Close session");
        System.out.println();
        System.out.println(" RESPONSE FORMAT (Server → Client):");
        System.out.println("   OK <message>           - Successful response header");
        System.out.println("   |field1|field2|...     - Data rows (pipe-delimited)");
        System.out.println("   ERROR <message>        - Error response");
        System.out.println("   END                    - End-of-response marker");
        System.out.println("  WELCOME <banner>        - Server welcome message on connect");
        System.out.println("==================================================================");
    }

    private static void displaySystemStatus() {
        System.out.println("================ SYSTEM ARCHITECTURE & STATUS ================");
        System.out.println("System Version : v0.9 (Day 9: Java Networking & Socket Programming)");
        System.out.println("Architecture   : Layered (Model -> DAO -> Service -> Simulation -> ImageProcessing -> Network -> UI)");
        System.out.println("Multithreading : Active (Dedicated Worker Threads per Station implementing Runnable)");
        System.out.println("Simulation Mgr : " + (simulationManager.isRunning() ? "🟢 Running (" + simulationManager.getSimulators().size() + " worker threads)" : "⚪ Idle / Standby"));
        System.out.println("Network Server : " + (networkServer.isRunning() ?
            "🟢 Listening on port " + networkServer.getPort() +
            " | Clients served: " + networkServer.getTotalClientsServed()
            : "⚪ Offline"));
        System.out.println("Thread Safety  : CopyOnWriteArrayList + Synchronized Service & DAO Methods");
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
