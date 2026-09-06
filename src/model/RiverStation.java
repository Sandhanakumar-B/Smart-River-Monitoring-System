package model;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 3: River Station Entity (Aggregation/Association, Method Overriding, Object Relationships)
 * Syllabus Unit: UNIT II - OOP Concepts, Aggregation, Association, Method Overriding, Object Class
 */
public class RiverStation {

    private String stationId;
    private String stationName;
    private String riverName;
    private double normalLevelMeters;
    private double dangerLevelMeters;

    // Default constructor
    public RiverStation() {
        this("STN-DEFAULT", "Main City Station", "Ganga River", 8.0, 16.0);
    }

    // Parameterized constructor
    public RiverStation(String stationId, String stationName, String riverName, double normalLevelMeters, double dangerLevelMeters) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.riverName = riverName;
        this.normalLevelMeters = normalLevelMeters;
        this.dangerLevelMeters = dangerLevelMeters;
    }

    // Association method: RiverStation produces a WaterLevelRecord
    public WaterLevelRecord generateReading(String recordId, double measuredLevel, String timestamp) {
        return new WaterLevelRecord(
            recordId,
            this.riverName,
            this.stationName + " (" + this.stationId + ")",
            measuredLevel,
            timestamp
        );
    }

    // Business check for flood risk against station-specific danger level
    public boolean isFloodRisk(double measuredLevel) {
        return measuredLevel >= this.dangerLevelMeters;
    }

    // Getters and Setters
    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
    }

    public double getNormalLevelMeters() {
        return normalLevelMeters;
    }

    public void setNormalLevelMeters(double normalLevelMeters) {
        this.normalLevelMeters = normalLevelMeters;
    }

    public double getDangerLevelMeters() {
        return dangerLevelMeters;
    }

    public void setDangerLevelMeters(double dangerLevelMeters) {
        this.dangerLevelMeters = dangerLevelMeters;
    }

    // Method overriding from java.lang.Object (UNIT II topic)
    @Override
    public String toString() {
        return "RiverStation [ID=" + stationId + ", Name=" + stationName + ", River=" + riverName +
               ", Normal=" + normalLevelMeters + "m, Danger=" + dangerLevelMeters + "m]";
    }

    public void printStationSummary() {
        System.out.println("  =============================================");
        System.out.println("  Station ID     : " + stationId);
        System.out.println("  Station Name   : " + stationName);
        System.out.println("  Monitored River: " + riverName);
        System.out.printf("  Normal Baseline: %.2f meters\n", normalLevelMeters);
        System.out.printf("  Danger Threshold: %.2f meters\n", dangerLevelMeters);
        System.out.println("  =============================================");
    }
}
