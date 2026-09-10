package simulation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 8: Multithreading & Automated Real-Time Sensor / River Simulation
 * Syllabus Unit: UNIT III & IV - Multithreading, Listener/Observer Pattern, Thread Communication
 * 
 * Callback interface for receiving asynchronous telemetry updates and flood alerts from sensor threads.
 */
public interface SensorEventListener {

    /**
     * Invoked when an automated sensor thread samples and records a new water level reading.
     *
     * @param event the sensor measurement event
     */
    void onReadingReceived(SensorEvent event);

    /**
     * Invoked when a measured level breaches the warning or danger threshold.
     *
     * @param event the alert trigger event
     */
    void onAlertTriggered(SensorEvent event);

    /**
     * Invoked when the simulation state changes (started, stopped, paused).
     *
     * @param statusMessage informational status message
     */
    void onSimulationStatusChanged(String statusMessage);
}
