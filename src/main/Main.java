package main;

import java.util.Scanner;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 2: Integrating Domain Model (WaterLevelRecord), Encapsulation, and Object Creation
 * Syllabus Unit: UNIT I & UNIT II - OOP Concepts, Classes, Objects, Constructors, Methods
 */
public class Main {

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
                        loadRiverImagePlaceholder();
                        break;
                    case 2:
                        estimateWaterLevelPlaceholder();
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
        System.out.println("1. Load / Select River Image");
        System.out.println("2. Process Image & Estimate Water Level");
        System.out.println("3. View Sample Water Level Record (Model Demo)");
        System.out.println("4. System Information & Status");
        System.out.println("5. Exit");
    }

    private static void loadRiverImagePlaceholder() {
        System.out.println("[Feature Pending]: Image file selection will be available soon.");
        System.out.println("Status: Ready to integrate file input handling.");
    }

    private static void estimateWaterLevelPlaceholder() {
        System.out.println("[Feature Pending]: Image processing and edge/level estimation module.");
        System.out.println("Status: Waiting for image selection module integration.");
    }

    private static void displaySampleRecordDemo() {
        System.out.println("=== DEMO: WATER LEVEL RECORD MODEL (OOP ENCAPSULATION) ===");
        
        // Creating object using parameterized constructor
        WaterLevelRecord sampleRecord1 = new WaterLevelRecord(
            "REC-101", 
            "Ganga River", 
            "Haridwar Gauge Station #4", 
            14.75, 
            "2026-08-29 10:30 AM"
        );
        
        // Creating object using default constructor and setter methods
        WaterLevelRecord sampleRecord2 = new WaterLevelRecord();
        sampleRecord2.setRecordId("REC-102");
        sampleRecord2.setRiverName("Yamuna River");
        sampleRecord2.setStationLocation("Delhi Bridge Station #2");
        sampleRecord2.setWaterLevelMeters(19.20);
        sampleRecord2.setTimestamp("2026-08-29 11:15 AM");

        System.out.println("\nSample Record 1 (Created via Parameterized Constructor):");
        sampleRecord1.printRecordDetails();

        System.out.println("\nSample Record 2 (Created via Default Constructor & Setters):");
        sampleRecord2.printRecordDetails();
    }

    private static void displaySystemStatus() {
        System.out.println("================ SYSTEM STATUS ================");
        System.out.println("System Version : v0.2 (Day 2 Data Model Integrated)");
        System.out.println("Active Model   : WaterLevelRecord (Encapsulation Enabled)");
        System.out.println("Storage Engine : In-Memory Object Model");
        System.out.println("===============================================");
    }
}
