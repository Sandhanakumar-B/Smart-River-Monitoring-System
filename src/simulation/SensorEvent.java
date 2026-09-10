package simulation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 8: Multithreading & Automated Real-Time Sensor / River Simulation
 * Syllabus Unit: UNIT III & IV - Multithreading, Thread Synchronization, Event Models
 * 
 * Represents an immutable real-time sensor measurement event dispatched by background sensor threads.
 */
public class SensorEvent {

    private final String stationId;
    private final String stationName;
    private final String riverName;
    private final double waterLevelMeters;
    private final String alertStatus;
    private final String timestamp;
    private final String sensorType;
    private final String threadName;

    public SensorEvent(String stationId, String stationName, String riverName,
                       double waterLevelMeters, String alertStatus, String timestamp,
                       String sensorType, String threadName) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.riverName = riverName;
        this.waterLevelMeters = waterLevelMeters;
        this.alertStatus = alertStatus;
        this.timestamp = timestamp;
        this.sensorType = sensorType;
        this.threadName = threadName;
    }

    public String getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public String getRiverName() {
        return riverName;
    }

    public double getWaterLevelMeters() {
        return waterLevelMeters;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSensorType() {
        return sensorType;
    }

    public String getThreadName() {
        return threadName;
    }

    public boolean isAlert() {
        return alertStatus != null && (alertStatus.contains("CRITICAL") || alertStatus.contains("WARNING"));
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s (%s) -> Level: %.2fm | Status: %s | Time: %s",
            threadName, sensorType, stationName, stationId, waterLevelMeters, alertStatus, timestamp);
    }
}
