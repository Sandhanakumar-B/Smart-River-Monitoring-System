package controller;

import exception.StationNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PostConstruct;
import model.RiverStation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import service.HydrologicalRiskService;
import service.RiverMonitoringService;
import simulation.RiverSimulationManager;
import simulation.SensorEvent;
import simulation.SensorEventListener;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 16: Real-Time Server-Sent Events (SSE) Live Telemetry & Hydrological Risk Prediction
 * Syllabus Unit: UNIT V - Reactive Web Architecture, SseEmitter, Server-Sent Events, Real-Time Push
 *
 * Exposes full-duplex HTTP Server-Sent Events (SSE) stream pushing IoT telemetry
 * updates directly to web dashboard clients without polling.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class RiverStreamController implements SensorEventListener {

    private final RiverMonitoringService monitoringService;
    private final RiverSimulationManager simulationManager;
    private final HydrologicalRiskService riskService;

    // Active SSE client emitters (thread-safe for concurrent subscriber joins/disconnects)
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong totalBroadcastPackets = new AtomicLong(0);

    @Autowired
    public RiverStreamController(RiverMonitoringService monitoringService,
                                 RiverSimulationManager simulationManager,
                                 HydrologicalRiskService riskService) {
        this.monitoringService = monitoringService;
        this.simulationManager = simulationManager;
        this.riskService       = riskService;
    }

    @PostConstruct
    public void initListener() {
        if (simulationManager != null) {
            simulationManager.addGlobalListener(this);
        }
    }

    // ======================================================================
    // 1. Server-Sent Events (SSE) Live Telemetry Streaming
    // ======================================================================

    /**
     * GET /api/stream/telemetry
     * Subscribes an HTTP client to the live river sensor telemetry stream.
     * Content-Type: text/event-stream
     */
    @GetMapping(value = "/stream/telemetry", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTelemetry() {
        // 30-minute timeout for robust live streaming
        SseEmitter emitter = new SseEmitter(1800000L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError((e) -> emitters.remove(emitter));

        // Send an initial handshake welcome packet
        try {
            Map<String, Object> welcome = new HashMap<>();
            welcome.put("eventType", "STREAM_CONNECTED");
            welcome.put("message", "Connected to Smart River Real-Time Telemetry Stream (Day 16 SSE)");
            welcome.put("subscribersCount", emitters.size());
            welcome.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            welcome.put("simulationRunning", simulationManager != null && simulationManager.isRunning());

            emitter.send(SseEmitter.event()
                .name("connected")
                .data(welcome)
            );
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * SensorEventListener callback - invoked automatically on every IoT sensor tick
     * Broadcasts payload to all connected SSE clients.
     */
    @Override
    public void onReadingReceived(SensorEvent event) {
        if (emitters.isEmpty() || event == null) return;

        RiverStation station = null;
        try {
            station = monitoringService.getStationByIdOrThrow(event.getStationId());
        } catch (Exception ignored) {}

        HydrologicalRiskService.RiskAssessment risk = (station != null) ? riskService.assessStationRisk(station) : null;

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "TELEMETRY");
        payload.put("stationId", event.getStationId());
        payload.put("stationName", event.getStationName());
        payload.put("riverName", event.getRiverName());
        payload.put("waterLevel", event.getWaterLevelMeters());
        payload.put("normalLevel", (station != null) ? station.getNormalLevelMeters() : 6.0);
        payload.put("dangerLevel", (station != null) ? station.getDangerLevelMeters() : 14.0);
        payload.put("alertStatus", event.getAlertStatus());
        payload.put("timestamp", event.getTimestamp());
        payload.put("rateOfRiseMph", (risk != null) ? Math.round(risk.getRateOfRiseMph() * 100.0) / 100.0 : 0.0);
        payload.put("riskLevel", (risk != null) ? risk.getRiskLevel() : "SAFE");
        payload.put("vulnerabilityScore", (risk != null) ? risk.getVulnerabilityScore() : 10);
        payload.put("estimatedCrestHours", (risk != null) ? risk.getEstimatedCrestHours() : -1.0);
        payload.put("packetId", totalBroadcastPackets.incrementAndGet());

        broadcastSseEvent("telemetry", payload);
    }

    @Override
    public void onAlertTriggered(SensorEvent event) {
        if (emitters.isEmpty() || event == null) return;

        RiverStation station = null;
        try {
            station = monitoringService.getStationByIdOrThrow(event.getStationId());
        } catch (Exception ignored) {}

        Map<String, Object> alertPayload = new HashMap<>();
        alertPayload.put("eventType", "EMERGENCY_FLOOD_ALERT");
        alertPayload.put("stationId", event.getStationId());
        alertPayload.put("stationName", event.getStationName());
        alertPayload.put("waterLevel", event.getWaterLevelMeters());
        alertPayload.put("dangerLevel", (station != null) ? station.getDangerLevelMeters() : 14.0);
        alertPayload.put("alertStatus", event.getAlertStatus());
        alertPayload.put("timestamp", event.getTimestamp());
        alertPayload.put("packetId", totalBroadcastPackets.incrementAndGet());

        broadcastSseEvent("flood_alert", alertPayload);
    }

    @Override
    public void onSimulationStatusChanged(String statusMessage) {
        if (emitters.isEmpty()) return;

        Map<String, Object> msgPayload = new HashMap<>();
        msgPayload.put("eventType", "STATUS_CHANGE");
        msgPayload.put("statusMessage", statusMessage);
        msgPayload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        broadcastSseEvent("status", msgPayload);
    }

    private void broadcastSseEvent(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data)
                );
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    // ======================================================================
    // 2. Stream & Simulation Management Endpoints
    // ======================================================================

    /**
     * GET /api/stream/status
     * Returns streaming statistics and simulation thread status.
     */
    @GetMapping("/stream/status")
    public Map<String, Object> getStreamStatus() {
        int activeWorkers = (simulationManager != null) ? simulationManager.getSimulators().size() : 0;
        return Map.of(
            "activeSubscribers",     emitters.size(),
            "totalBroadcastPackets", totalBroadcastPackets.get(),
            "simulationRunning",     simulationManager != null && simulationManager.isRunning(),
            "activeSensorsCount",    activeWorkers,
            "streamEndpoint",        "/api/stream/telemetry"
        );
    }

    /**
     * POST /api/stream/simulation/start
     * Starts the IoT sensor thread simulation engine to feed live SSE telemetry.
     */
    @PostMapping("/stream/simulation/start")
    public ResponseEntity<?> startSimulation() {
        if (simulationManager == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Simulation manager not configured."));
        }
        simulationManager.startSimulation();
        return ResponseEntity.ok(Map.of(
            "message", "IoT Telemetry Simulation started. Broadcasting live SSE packets.",
            "running", true,
            "activeSensors", simulationManager.getSimulators().size()
        ));
    }

    /**
     * POST /api/stream/simulation/stop
     * Halts all sensor threads.
     */
    @PostMapping("/stream/simulation/stop")
    public ResponseEntity<?> stopSimulation() {
        if (simulationManager == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Simulation manager not configured."));
        }
        simulationManager.stopSimulation();
        return ResponseEntity.ok(Map.of(
            "message", "IoT Telemetry Simulation stopped.",
            "running", false
        ));
    }

    // ======================================================================
    // 3. Hydrological Risk Prediction Endpoints
    // ======================================================================

    /**
     * GET /api/stations/{stationId}/risk
     * Returns hydrological flood risk metrics, rate of rise, and crest estimation.
     */
    @GetMapping("/stations/{stationId}/risk")
    public ResponseEntity<?> getStationRisk(@PathVariable String stationId) {
        try {
            RiverStation station = monitoringService.getStationByIdOrThrow(stationId);
            HydrologicalRiskService.RiskAssessment assessment = riskService.assessStationRisk(station);
            return ResponseEntity.ok(assessment.toMap());
        } catch (StationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Station not found: " + stationId));
        }
    }

    /**
     * GET /api/basin/risk
     * Returns basin-wide flood vulnerability assessment across all registered stations.
     */
    @GetMapping("/basin/risk")
    public ResponseEntity<List<Map<String, Object>>> getBasinRisk() {
        List<HydrologicalRiskService.RiskAssessment> assessments = riskService.assessAllStationsRisk();
        List<Map<String, Object>> result = assessments.stream()
            .map(HydrologicalRiskService.RiskAssessment::toMap)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
