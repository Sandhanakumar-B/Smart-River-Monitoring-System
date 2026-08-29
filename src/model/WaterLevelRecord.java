package model;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 2: Water Level Data Model (Encapsulation, Constructors, Getters/Setters, Access Specifiers)
 * Syllabus Unit: UNIT I & UNIT II - OOP Concepts, Classes, Constructors, Encapsulation, Methods
 */
public class WaterLevelRecord {

    // Private fields (Encapsulation)
    private String recordId;
    private String riverName;
    private String stationLocation;
    private double waterLevelMeters;
    private String alertStatus;
    private String timestamp;

    // Threshold constants (in meters)
    public static final double WARNING_THRESHOLD = 12.0;
    public static final double CRITICAL_THRESHOLD = 18.0;

    // Default constructor
    public WaterLevelRecord() {
        this.recordId = "REC-000";
        this.riverName = "Unknown River";
        this.stationLocation = "Default Station";
        this.waterLevelMeters = 0.0;
        this.timestamp = "N/A";
        this.alertStatus = evaluateAlertStatus(0.0);
    }

    // Parameterized constructor
    public WaterLevelRecord(String recordId, String riverName, String stationLocation, double waterLevelMeters, String timestamp) {
        this.recordId = recordId;
        this.riverName = riverName;
        this.stationLocation = stationLocation;
        this.waterLevelMeters = waterLevelMeters;
        this.timestamp = timestamp;
        this.alertStatus = evaluateAlertStatus(waterLevelMeters);
    }

    // Helper method to determine status based on water level
    public static String evaluateAlertStatus(double level) {
        if (level >= CRITICAL_THRESHOLD) {
            return "CRITICAL (Flood Alert)";
        } else if (level >= WARNING_THRESHOLD) {
            return "WARNING (High Flow)";
        } else {
            return "NORMAL (Safe Level)";
        }
    }

    // Getters and Setters
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
    }

    public String getStationLocation() {
        return stationLocation;
    }

    public void setStationLocation(String stationLocation) {
        this.stationLocation = stationLocation;
    }

    public double getWaterLevelMeters() {
        return waterLevelMeters;
    }

    public void setWaterLevelMeters(double waterLevelMeters) {
        this.waterLevelMeters = waterLevelMeters;
        this.alertStatus = evaluateAlertStatus(waterLevelMeters);
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Formatted details view
    public void printRecordDetails() {
        System.out.println("  ---------------------------------------------");
        System.out.println("  Record ID       : " + recordId);
        System.out.println("  River Name      : " + riverName);
        System.out.println("  Station Location: " + stationLocation);
        System.out.printf("  Water Level     : %.2f meters\n", waterLevelMeters);
        System.out.println("  Alert Status    : " + alertStatus);
        System.out.println("  Timestamp       : " + timestamp);
        System.out.println("  ---------------------------------------------");
    }
}
