package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import model.RiverStation;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 8: Multithreading & Automated Real-Time Sensor / River Simulation
 * Syllabus Unit: UNIT III & IV - Thread Groups/Coordination, Synchronization, Concurrency Management
 * 
 * Orchestrates multiple concurrent sensor worker threads across the river basin.
 */
public class RiverSimulationManager {

    private final RiverMonitoringService monitoringService;
    private final List<StationSensorSimulator> simulators;
    private final List<SensorEventListener> globalListeners;
    private volatile boolean isRunning;
    private long defaultIntervalMillis;

    public RiverSimulationManager(RiverMonitoringService monitoringService) {
        this(monitoringService, 2500); // default 2.5 seconds per tick
    }

    public RiverSimulationManager(RiverMonitoringService monitoringService, long defaultIntervalMillis) {
        if (monitoringService == null) {
            throw new IllegalArgumentException("RiverMonitoringService cannot be null.");
        }
        this.monitoringService = monitoringService;
        this.defaultIntervalMillis = Math.max(500, defaultIntervalMillis);
        this.simulators = new CopyOnWriteArrayList<>();
        this.globalListeners = new CopyOnWriteArrayList<>();
        this.isRunning = false;
        refreshSimulators();
    }

    /**
     * Discovers all registered stations and initializes corresponding sensor workers.
     */
    public synchronized void refreshSimulators() {
        boolean wasRunning = isRunning;
        if (wasRunning) {
            stopSimulation();
        }

        simulators.clear();
        for (RiverStation station : monitoringService.getAllStations()) {
            StationSensorSimulator sim = new StationSensorSimulator(station, monitoringService, defaultIntervalMillis);
            for (SensorEventListener listener : globalListeners) {
                sim.addListener(listener);
            }
            simulators.add(sim);
        }

        if (wasRunning) {
            startSimulation();
        }
    }

    public void addGlobalListener(SensorEventListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            globalListeners.add(listener);
            for (StationSensorSimulator sim : simulators) {
                sim.addListener(listener);
            }
        }
    }

    public void removeGlobalListener(SensorEventListener listener) {
        globalListeners.remove(listener);
        for (StationSensorSimulator sim : simulators) {
            sim.removeListener(listener);
        }
    }

    /**
     * Starts all sensor worker threads concurrently.
     */
    public synchronized void startSimulation() {
        if (isRunning) {
            return;
        }

        // Re-check stations in case new ones were added
        if (simulators.isEmpty()) {
            refreshSimulators();
        }

        notifyStatusChange("Starting " + simulators.size() + " concurrent IoT sensor threads...");
        for (StationSensorSimulator sim : simulators) {
            sim.start();
        }
        this.isRunning = true;
        notifyStatusChange("All " + simulators.size() + " sensor threads are now ACTIVE and transmitting telemetry.");
    }

    /**
     * Safely stops and interrupts all sensor worker threads.
     */
    public synchronized void stopSimulation() {
        if (!isRunning && simulators.stream().noneMatch(StationSensorSimulator::isRunning)) {
            return;
        }

        notifyStatusChange("Halting all sensor threads...");
        for (StationSensorSimulator sim : simulators) {
            sim.stop();
        }

        // Wait up to 1 second per thread for clean termination
        for (StationSensorSimulator sim : simulators) {
            try {
                sim.join(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.isRunning = false;
        notifyStatusChange("All sensor threads stopped successfully. Simulation offline.");
    }

    /**
     * Pauses or unpauses all active sensor simulators without killing their threads.
     */
    public void setPaused(boolean paused) {
        for (StationSensorSimulator sim : simulators) {
            sim.setPaused(paused);
        }
        notifyStatusChange(paused ? "Simulation PAUSED (Threads in wait state)." : "Simulation RESUMED.");
    }

    public void pauseSimulation() {
        setPaused(true);
    }

    public void resumeSimulation() {
        setPaused(false);
    }

    public boolean isPaused() {
        return !simulators.isEmpty() && simulators.stream().allMatch(StationSensorSimulator::isPaused);
    }

    /**
     * Injects a rapid water level surge across all stations (e.g., severe cloudburst upstream).
     */
    public void triggerBasinSurge(double surgeMeters) {
        for (StationSensorSimulator sim : simulators) {
            sim.applySurge(surgeMeters);
        }
        notifyStatusChange(String.format("BASIN-WIDE SURGE TRIGGERED: +%.2fm water rise applied to all active stations!", surgeMeters));
    }

    /**
     * Injects a water level surge into a specific river station.
     */
    public boolean triggerStationSurge(String stationId, double surgeMeters) {
        for (StationSensorSimulator sim : simulators) {
            if (sim.getStation().getStationId().equalsIgnoreCase(stationId.trim())) {
                sim.applySurge(surgeMeters);
                notifyStatusChange(String.format("SURGE TRIGGERED at %s: +%.2fm rise injected!", stationId, surgeMeters));
                return true;
            }
        }
        return false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public List<StationSensorSimulator> getSimulators() {
        return Collections.unmodifiableList(simulators);
    }

    public long getDefaultIntervalMillis() {
        return defaultIntervalMillis;
    }

    public void setDefaultIntervalMillis(long defaultIntervalMillis) {
        this.defaultIntervalMillis = Math.max(500, defaultIntervalMillis);
        for (StationSensorSimulator sim : simulators) {
            sim.setSamplingIntervalMillis(this.defaultIntervalMillis);
        }
    }

    /**
     * Diagnostic report of active worker threads and their runtime state.
     */
    public List<String> getThreadDiagnosticReport() {
        List<String> report = new ArrayList<>();
        for (StationSensorSimulator sim : simulators) {
            Thread t = sim.getWorkerThread();
            String threadName = (t != null) ? t.getName() : "SensorThread-" + sim.getStation().getStationId() + " (not started)";
            String state = (t != null) ? t.getState().toString() : "NEW";
            boolean isAlive = (t != null) && t.isAlive();
            int priority = (t != null) ? t.getPriority() : Thread.NORM_PRIORITY;

            report.add(String.format("Thread: %-26s | State: %-14s | Alive: %-5b | Priority: %d | Station: %s",
                threadName, state, isAlive, priority, sim.getStation().getStationId()));
        }
        return report;
    }

    private void notifyStatusChange(String message) {
        for (SensorEventListener listener : globalListeners) {
            listener.onSimulationStatusChanged(message);
        }
    }
}
