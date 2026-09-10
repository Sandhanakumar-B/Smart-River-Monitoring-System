package simulation;

import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 8: Multithreading & Automated Real-Time Sensor / River Simulation
 * Syllabus Unit: UNIT III & IV - Multithreading, Runnable Interface, Thread Lifecycle & Interruption
 * 
 * Simulates an automated IoT river gauge telemetry sensor executing on an independent worker thread.
 */
public class StationSensorSimulator implements Runnable {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

    private final RiverStation station;
    private final RiverMonitoringService monitoringService;
    private final List<SensorEventListener> listeners;
    private final Random random;

    private volatile boolean running;
    private volatile boolean paused;
    private volatile double surgeOffsetMeters;
    private long samplingIntervalMillis;
    private double currentSimulatedLevel;
    private Thread workerThread;

    public StationSensorSimulator(RiverStation station, RiverMonitoringService monitoringService, long samplingIntervalMillis) {
        if (station == null || monitoringService == null) {
            throw new IllegalArgumentException("Station and MonitoringService cannot be null.");
        }
        this.station = station;
        this.monitoringService = monitoringService;
        this.samplingIntervalMillis = Math.max(500, samplingIntervalMillis);
        this.listeners = new CopyOnWriteArrayList<>();
        this.random = new Random();
        this.running = false;
        this.paused = false;
        this.surgeOffsetMeters = 0.0;
        this.currentSimulatedLevel = station.getNormalLevelMeters();
    }

    public void addListener(SensorEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SensorEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts the sensor simulator in a new managed background thread.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        this.running = true;
        this.paused = false;
        this.workerThread = new Thread(this, "SensorThread-" + station.getStationId());
        this.workerThread.setDaemon(true); // Allow JVM to exit cleanly if main terminates
        this.workerThread.start();
    }

    /**
     * Gracefully signals the worker thread to stop and interrupts sleep.
     */
    public synchronized void stop() {
        this.running = false;
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
    }

    /**
     * Waits for worker thread to complete execution.
     */
    public void join(long millis) throws InterruptedException {
        if (workerThread != null) {
            workerThread.join(millis);
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }

    public void applySurge(double surgeMeters) {
        this.surgeOffsetMeters += surgeMeters;
        this.currentSimulatedLevel = Math.min(RiverMonitoringService.MAX_PERMISSIBLE_LEVEL - 1.0, this.currentSimulatedLevel + surgeMeters);
    }

    public void resetSurge() {
        this.surgeOffsetMeters = 0.0;
        this.currentSimulatedLevel = station.getNormalLevelMeters();
    }

    public RiverStation getStation() {
        return station;
    }

    public Thread getWorkerThread() {
        return workerThread;
    }

    public long getSamplingIntervalMillis() {
        return samplingIntervalMillis;
    }

    public void setSamplingIntervalMillis(long samplingIntervalMillis) {
        this.samplingIntervalMillis = Math.max(500, samplingIntervalMillis);
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();

        while (running) {
            try {
                if (!paused) {
                    performMeasurementTick(threadName);
                }

                Thread.sleep(samplingIntervalMillis);
            } catch (InterruptedException e) {
                // Thread interrupted for shutdown or interval adjustment
                Thread.currentThread().interrupt(); // Restore interrupted status
                break;
            } catch (Exception e) {
                System.err.println("[" + threadName + " Error] " + e.getMessage());
            }
        }
    }

    /**
     * Computes next hydrological reading, records into service layer, and dispatches event.
     */
    private void performMeasurementTick(String threadName) {
        // Hydrological Markov Random Walk: slight drift towards normal baseline with natural ripple
        double drift = (station.getNormalLevelMeters() - currentSimulatedLevel) * 0.08;
        double jitter = (random.nextDouble() - 0.48) * 0.25; // small ripple [-0.12m, +0.13m]

        currentSimulatedLevel += drift + jitter + (surgeOffsetMeters * 0.2);
        
        // Decay surge offset gradually to simulate river drainage
        if (surgeOffsetMeters > 0.0) {
            surgeOffsetMeters = Math.max(0.0, surgeOffsetMeters - 0.15);
        }

        // Clamp within realistic physical boundaries
        currentSimulatedLevel = Math.max(1.0, Math.min(RiverMonitoringService.MAX_PERMISSIBLE_LEVEL - 1.0, currentSimulatedLevel));
        double roundedLevel = Math.round(currentSimulatedLevel * 100.0) / 100.0;

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        try {
            // Persist through thread-safe service & DAO
            WaterLevelRecord record = monitoringService.recordMeasurement(station.getStationId(), roundedLevel, timestamp);

            // Construct telemetry event
            SensorEvent event = new SensorEvent(
                station.getStationId(),
                station.getStationName(),
                station.getRiverName(),
                roundedLevel,
                record.getAlertStatus(),
                timestamp,
                "ULTRASONIC_TELEMETRY",
                threadName
            );

            // Dispatch to listeners
            for (SensorEventListener listener : listeners) {
                listener.onReadingReceived(event);
                if (event.isAlert()) {
                    listener.onAlertTriggered(event);
                }
            }

        } catch (StationNotFoundException | InvalidWaterLevelException e) {
            System.err.println("[" + threadName + " Validation Issue] " + e.getMessage());
        }
    }
}
