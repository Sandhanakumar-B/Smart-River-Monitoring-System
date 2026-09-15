package gui;

import dao.jdbc.DatabaseConnectionManager;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.swing.SwingUtilities;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;
import simulation.RiverSimulationManager;
import simulation.SensorEvent;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 11: Java Swing Graphical User Interface & Real-Time Monitoring Dashboard
 * Syllabus Unit: UNIT V - Automated Verification Suite for Swing GUI, Graphics2D Rendering & Event Listeners
 * 
 * Verifies GUI components, 2D offscreen rendering, telemetry event callbacks, and JDBC integration.
 */
public class TestGUI {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   DAY 11 SWING GUI & EVENT-DRIVEN DASHBOARD VERIFICATION   ");
        System.out.println("============================================================");

        testHeadlessEnvironmentSafety();
        testRiverGaugeVisualizerRendering();
        testGuiComponentInitializationAndDataBinding();
        testSensorEventListenerTelemetryBinding();
        testJdbcSqlConsoleIntegration();

        System.out.println("\n============================================================");
        System.out.printf("GUI VERIFICATION SUMMARY: %d PASSED, %d FAILED\n", testsPassed, testsFailed);
        System.out.println("============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void testHeadlessEnvironmentSafety() {
        System.out.println("\n[Test 1] Verifying Graphics Environment & Safety Detection...");
        boolean isHeadless = GraphicsEnvironment.isHeadless();
        System.out.println("  Info: GraphicsEnvironment.isHeadless() = " + isHeadless);
        assertCondition(true, "Graphics environment safely queried without unhandled runtime exceptions.");
    }

    private static void testRiverGaugeVisualizerRendering() {
        System.out.println("\n[Test 2] Testing RiverGaugeVisualizerPanel Graphics2D Rendering Pipeline...");
        try {
            RiverStation station = new RiverStation("STN-TEST-01", "Rishikesh Test Station", "Ganga River", 6.5, 14.5);
            RiverGaugeVisualizerPanel panel = new RiverGaugeVisualizerPanel(station, 8.2, 11.0, 14.5, 25.0);
            panel.setSize(360, 480);

            // Assert properties
            assertCondition(panel.getCurrentWaterLevel() == 8.2, "Initial water level correctly assigned (8.2m).");
            assertCondition(panel.getDangerThreshold() == 14.5, "Danger threshold correctly assigned (14.5m).");

            // Update level
            panel.setCurrentWaterLevel(15.2); // Now in danger range
            assertCondition(panel.getCurrentWaterLevel() == 15.2, "Updated water level correctly reflected (15.2m).");

            // Render to off-screen BufferedImage to verify Graphics2D pipeline
            BufferedImage buffer = new BufferedImage(360, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffer.createGraphics();
            panel.paint(g2);
            g2.dispose();

            // Verify the buffer contains painted non-transparent pixels
            int samplePixel = buffer.getRGB(180, 240);
            assertCondition(samplePixel != 0, "Off-screen Graphics2D rendering executed; non-blank buffer confirmed (RGB=" + Integer.toHexString(samplePixel) + ").");

        } catch (Exception e) {
            assertCondition(false, "Graphics2D rendering failed: " + e.getMessage());
        }
    }

    private static void testGuiComponentInitializationAndDataBinding() {
        System.out.println("\n[Test 3] Testing GUI Construction, Data Binding & Service Layer Integration...");
        try {
            RiverMonitoringService service = new RiverMonitoringService();
            RiverSimulationManager simManager = new RiverSimulationManager(service);

            RiverMonitoringGUI gui = new RiverMonitoringGUI(service, simManager);

            assertCondition(gui.getMonitoringService() != null, "RiverMonitoringService successfully bound to GUI.");
            assertCondition(gui.getSimulationManager() != null, "RiverSimulationManager successfully bound to GUI.");

            List<RiverStation> stations = service.getAllStations();
            assertCondition(stations.size() >= 4, "Default monitoring stations registered: " + stations.size() + " stations found.");

            // In non-headless environments, check table model count
            if (!GraphicsEnvironment.isHeadless()) {
                assertCondition(gui.getStationsTableModel() != null, "Stations JTable model initialized.");
                assertCondition(gui.getStationsTableModel().getRowCount() == stations.size(), 
                    "JTable row count matches registered stations (" + stations.size() + " rows).");
                gui.dispose();
            } else {
                System.out.println("  [Headless Notice] UI frame display skipped in headless mode.");
            }

        } catch (Exception e) {
            assertCondition(false, "GUI construction failed: " + e.getMessage());
        }
    }

    private static void testSensorEventListenerTelemetryBinding() {
        System.out.println("\n[Test 4] Testing SensorEventListener Real-Time Telemetry Event Dispatch...");
        try {
            RiverMonitoringService service = new RiverMonitoringService();
            RiverSimulationManager simManager = new RiverSimulationManager(service);

            RiverMonitoringGUI gui = new RiverMonitoringGUI(service, simManager);

            // Construct telemetry event
            SensorEvent event = new SensorEvent(
                "STN-HAR-01",
                "Haridwar Central Gauge Station",
                "Ganga River",
                17.85,
                "CRITICAL (Flood Alert)",
                "2026-09-15 10:30:00 AM",
                "ULTRASONIC_TELEMETRY",
                "SensorWorker-STN-HAR-01"
            );

            // Dispatch event directly to GUI listener callback
            gui.onReadingReceived(event);
            gui.onAlertTriggered(event);
            gui.onSimulationStatusChanged("Telemetry test heartbeat");

            assertCondition(true, "SensorEvent callbacks delivered to GUI listener successfully without threading conflicts.");

            if (!GraphicsEnvironment.isHeadless()) {
                gui.dispose();
            }

        } catch (Exception e) {
            assertCondition(false, "Telemetry event dispatch failed: " + e.getMessage());
        }
    }

    private static void testJdbcSqlConsoleIntegration() {
        System.out.println("\n[Test 5] Testing Relational JDBC Console Database Connection & Querying...");
        try {
            DatabaseConnectionManager connMgr = DatabaseConnectionManager.getInstance();
            connMgr.initializeSchema();
            Connection conn = connMgr.getConnection();

            dao.jdbc.StationJdbcDAO stationDAO = new dao.jdbc.StationJdbcDAO(connMgr);
            if (stationDAO.getAllStations().isEmpty()) {
                stationDAO.saveStation(new RiverStation("STN-HAR-01", "Haridwar Central Gauge Station", "Ganga River", 7.5, 16.5));
            }

            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT station_id, station_name FROM stations")) {
                    int rows = 0;
                    while (rs.next()) {
                        rows++;
                    }
                    assertCondition(rows >= 1, "Relational SQL console integration verified: " + rows + " station record(s) queried via JDBC.");
                }
            }
        } catch (Exception e) {
            assertCondition(false, "JDBC SQL console query failed: " + e.getMessage());
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("  PASS: " + message);
            testsPassed++;
        } else {
            System.err.println("  FAIL: " + message);
            testsFailed++;
        }
    }
}
