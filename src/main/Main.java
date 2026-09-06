package main;

import java.util.Scanner;
import model.RiverStation;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 3: Integrating RiverStation, OOP Association, and Method Overriding
 * Syllabus Unit: UNIT II - Aggregation, Association, Method Overriding, Object Relationships
 */
public class Main {

    // Pre-configured default monitoring station for the session
    private static RiverStation activeStation = new RiverStation(
        "STN-HAR-01",
        "Haridwar Central Gauge Station",
        "Ganga River",
        7.5,
        16.5
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printHeader();

        while (running) {
            displayMenu();
            System.out.print("Enter your choice (1-5): ");

            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                System.out.println();
                switch (choice) {
                    case 1:
                        displayStationDetails();
                        break;
                    case 2:
                        generateStationReadingDemo(scanner);
                        break;
                    case 3:
                        displaySampleRecordDemo();
                        break;
                    case 4:
                        displaySystemStatus();
                        break;
                    case 5:
                        System.out.println("Exiting system. Thank you for using Smart River Water Level Monitoring!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 5.");
                }
            } else {
                System.out.println("\nInvalid input! Please enter a valid numerical option.");
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
        System.out.println("Academic Prototype - Core Java Console Edition\n");
    }

    private static void displayMenu() {
        System.out.println("MAIN MENU:");
        System.out.println("1. View Active Monitoring Station Details");
        System.out.println("2. Record Water Level for Active Station (Association Demo)");
        System.out.println("3. View Sample Model Records");
        System.out.println("4. System Information & Status");
        System.out.println("5. Exit");
    }

    private static void displayStationDetails() {
        System.out.println("=== ACTIVE MONITORING STATION ===");
        activeStation.printStationSummary();
        System.out.println("\nObject toString() Representation (Method Overriding):");
        System.out.println("  " + activeStation.toString());
    }

    private static void generateStationReadingDemo(Scanner scanner) {
        System.out.println("=== RECORD WATER LEVEL READING FOR STATION ===");
        System.out.print("Enter measured water level in meters (e.g. 11.2): ");

        if (scanner.hasNextDouble()) {
            double level = scanner.nextDouble();
            scanner.nextLine();

            // Association: Active station generates and associates a new reading
            WaterLevelRecord newRecord = activeStation.generateReading(
                "REC-" + System.currentTimeMillis() % 10000,
                level,
                "2026-08-29 (Manual/Sensory Input)"
            );

            System.out.println("\n[SUCCESS] New Water Level Record Generated via Station Association:");
            newRecord.printRecordDetails();

            if (activeStation.isFloodRisk(level)) {
                System.out.println(">>> ALERT: Measured level exceeds Station Danger Threshold (" + activeStation.getDangerLevelMeters() + "m)!");
            } else {
                System.out.println(">>> STATUS: Measured level is within safe operating range.");
            }
        } else {
            System.out.println("Invalid water level entered! Please enter a valid decimal number.");
            scanner.nextLine();
        }
    }

    private static void displaySampleRecordDemo() {
        System.out.println("=== DEMO: WATER LEVEL RECORD MODEL (OOP ENCAPSULATION) ===");
        
        WaterLevelRecord sampleRecord1 = new WaterLevelRecord(
            "REC-101", 
            "Ganga River", 
            "Haridwar Gauge Station #4", 
            14.75, 
            "2026-08-29 10:30 AM"
        );
        
        WaterLevelRecord sampleRecord2 = new WaterLevelRecord();
        sampleRecord2.setRecordId("REC-102");
        sampleRecord2.setRiverName("Yamuna River");
        sampleRecord2.setStationLocation("Delhi Bridge Station #2");
        sampleRecord2.setWaterLevelMeters(19.20);
        sampleRecord2.setTimestamp("2026-08-29 11:15 AM");

        System.out.println("\nSample Record 1:");
        sampleRecord1.printRecordDetails();

        System.out.println("\nSample Record 2:");
        sampleRecord2.printRecordDetails();
    }

    private static void displaySystemStatus() {
        System.out.println("================ SYSTEM STATUS ================");
        System.out.println("System Version : v0.3 (Day 3 Station Association Active)");
        System.out.println("Active Station : " + activeStation.getStationName());
        System.out.println("Storage Engine : In-Memory Object Architecture");
        System.out.println("===============================================");
    }
}
