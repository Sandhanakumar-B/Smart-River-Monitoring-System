package service;

import controller.RiverStreamController;
import dao.StationDAO;
import dao.WaterLevelRecordDAO;
import dao.StationFileDAO;
import dao.WaterLevelRecordFileDAO;
import model.RiverStation;
import model.WaterLevelRecord;
import simulation.RiverSimulationManager;
import simulation.SensorEvent;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 16: Real-Time Server-Sent Events (SSE) Live Telemetry & Hydrological Risk Prediction
 * Syllabus Unit: UNIT V - Verification Test Suite for Reactive SSE & Predictive Hydrology
 */
public class TestDay16StreamAndRisk {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  DAY 16 VERIFICATION SUITE: SSE STREAMING & RISK PREDICTION     ");
        System.out.println("=================================================================\n");

        int passed = 0;
        int failed = 0;

        try {
            // 1. Setup Services
            StationDAO stationDAO = new StationFileDAO();
            WaterLevelRecordDAO recordDAO = new WaterLevelRecordFileDAO();
            RiverMonitoringService monitoringService = new RiverMonitoringService(stationDAO, recordDAO);
            RiverSimulationManager simManager = new RiverSimulationManager(monitoringService);
            HydrologicalRiskService riskService = new HydrologicalRiskService(monitoringService);
            RiverStreamController streamController = new RiverStreamController(monitoringService, simManager, riskService);

            // Test 1: Hydrological Risk Service on nominal station
            System.out.print("[TEST 1] Hydrological risk assessment on nominal baseline... ");
            RiverStation station = monitoringService.getAllStations().get(0);
            HydrologicalRiskService.RiskAssessment assessment = riskService.assessStationRisk(station);
            assertCondition(assessment != null, "Assessment should not be null.");
            assertCondition(assessment.getStationId().equals(station.getStationId()), "Station ID must match.");
            assertCondition(assessment.getVulnerabilityScore() >= 0 && assessment.getVulnerabilityScore() <= 100,
                            "Score must be between 0 and 100.");
            System.out.println("PASSED! Risk Level: " + assessment.getRiskLevel() + ", Score: " + assessment.getVulnerabilityScore());
            passed++;

            // Test 2: Flood surge risk escalation
            System.out.print("[TEST 2] High flood surge risk assessment escalation... ");
            monitoringService.recordMeasurement(station.getStationId(), station.getDangerLevelMeters() + 1.5, "2026-09-22 10:00:00 [TEST]");
            HydrologicalRiskService.RiskAssessment surgeAssessment = riskService.assessStationRisk(station);
            assertCondition("CRITICAL_SURGE".equals(surgeAssessment.getRiskLevel()), "Surge reading must yield CRITICAL_SURGE.");
            assertCondition(surgeAssessment.getVulnerabilityScore() >= 85, "Vulnerability score must be >= 85.");
            assertCondition(surgeAssessment.getRecommendation().contains("High Flood Alert"), "Must include emergency recommendation.");
            System.out.println("PASSED! Correctly escalated to: " + surgeAssessment.getRiskLevel());
            passed++;

            // Test 3: Basin-wide risk overview
            System.out.print("[TEST 3] Basin-wide hydrological risk overview... ");
            var allRisks = riskService.assessAllStationsRisk();
            assertCondition(allRisks.size() == monitoringService.getAllStations().size(), "Must evaluate all basin stations.");
            System.out.println("PASSED! Evaluated " + allRisks.size() + " stations.");
            passed++;

            // Test 4: SSE Stream Controller Emitter Creation
            System.out.print("[TEST 4] SSE Stream Emitter subscription handshake... ");
            var emitter = streamController.streamTelemetry();
            assertCondition(emitter != null, "SseEmitter must not be null.");
            var status = streamController.getStreamStatus();
            assertCondition((int) status.get("activeSubscribers") >= 1, "Active subscribers must be at least 1.");
            System.out.println("PASSED! Active Subscribers: " + status.get("activeSubscribers"));
            passed++;

            // Test 5: Sensor reading SSE broadcast dispatch
            System.out.print("[TEST 5] Sensor event SSE broadcasting... ");
            SensorEvent event = new SensorEvent(
                station.getStationId(),
                station.getStationName(),
                station.getRiverName(),
                8.75,
                "NORMAL",
                "2026-09-22 10:15:00",
                "IoT-Radar",
                "WorkerThread-1"
            );
            streamController.onReadingReceived(event);
            var updatedStatus = streamController.getStreamStatus();
            assertCondition((long) updatedStatus.get("totalBroadcastPackets") >= 1, "Packet count must increment.");
            System.out.println("PASSED! Broadcast packets dispatched: " + updatedStatus.get("totalBroadcastPackets"));
            passed++;

            // Test 6: Simulation Thread Lifecycle Control
            System.out.print("[TEST 6] IoT Simulation Start/Stop via Stream Controller... ");
            var startRes = streamController.startSimulation();
            assertCondition(startRes.getStatusCode().is2xxSuccessful(), "Start response must be 200 OK.");
            assertCondition(simManager.isRunning(), "SimulationManager must be running.");
            var stopRes = streamController.stopSimulation();
            assertCondition(stopRes.getStatusCode().is2xxSuccessful(), "Stop response must be 200 OK.");
            assertCondition(!simManager.isRunning(), "SimulationManager must be stopped.");
            System.out.println("PASSED! Simulation started and halted cleanly.");
            passed++;

            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("DAY 16 ALL " + passed + " TESTS PASSED SUCCESSFULLY! (0 failures)");
            System.out.println("-----------------------------------------------------------------");

        } catch (Throwable t) {
            System.err.println("\nFAILED! Exception: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }
}
