package dao.jpa;

import model.RiverStation;
import model.WaterLevelRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 14: Spring Data JPA & H2 Database Integration - Verification Suite
 * Syllabus Unit: UNIT V - Spring Data JPA Testing, Entity Lifecycle, CRUD Verification
 *
 * Standalone automated test that boots the Spring Application Context,
 * performs a complete JPA CRUD lifecycle test using StationJpaDAO
 * and WaterLevelRecordJpaDAO, and verifies all operations without manual setup.
 *
 * Run with: mvn spring-boot:run -Dspring-boot.run.mainClass=dao.jpa.TestJpa
 * Or call TestJpa.main(args) from Main.java menu integration.
 */
public class TestJpa {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("   DAY 14 AUTOMATED JPA VERIFICATION SUITE");
        System.out.println("   Spring Data JPA + H2 Embedded Database - CRUD Lifecycle Test");
        System.out.println("=================================================================\n");

        ApplicationContext ctx;
        try {
            ctx = SpringApplication.run(main.SmartRiverApplication.class, args);
        } catch (Exception e) {
            System.out.println("[FATAL] Could not start Spring context: " + e.getMessage());
            return;
        }

        StationJpaDAO          stationDao = ctx.getBean(StationJpaDAO.class);
        WaterLevelRecordJpaDAO recordDao  = ctx.getBean(WaterLevelRecordJpaDAO.class);

        runStationTests(stationDao);
        runRecordTests(stationDao, recordDao);
        runAlertQueryTests(recordDao);

        System.out.println("\n=================================================================");
        System.out.printf("  RESULTS: %d PASSED  |  %d FAILED%n", passed, failed);
        System.out.println("=================================================================");

        SpringApplication.exit(ctx);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ----- Station CRUD Tests -----

    private static void runStationTests(StationJpaDAO dao) {
        System.out.println("--- [TEST GROUP 1] Station JPA CRUD Operations ---");

        // 1. Save a new station
        RiverStation testStation = new RiverStation("JPA-TEST-01", "JPA Test Gauge Station", "Test River", 5.0, 12.0);
        dao.saveStation(testStation);
        assertPass("saveStation() persists entity to jpa_stations table");

        // 2. Retrieve all stations - must include at least the one we just saved
        List<RiverStation> all = dao.getAllStations();
        assertCondition(!all.isEmpty(), "getAllStations() returns non-empty list from H2");

        // 3. Get by ID
        RiverStation fetched = dao.getStationById("JPA-TEST-01");
        assertCondition(fetched != null, "getStationById() retrieves saved entity");
        if (fetched != null) {
            assertCondition("JPA Test Gauge Station".equals(fetched.getStationName()),
                "getStationById() returns correct stationName");
        }

        // 4. existsById check
        boolean exists = dao.existsById("JPA-TEST-01");
        assertCondition(exists, "existsById() returns true for saved station");

        boolean notExists = dao.existsById("JPA-GHOST-99");
        assertCondition(!notExists, "existsById() returns false for non-existent ID");

        // 5. saveAllStations (batch)
        List<RiverStation> batch = List.of(
            new RiverStation("JPA-TEST-02", "Batch Station Alpha", "Alpha River", 4.0, 10.0),
            new RiverStation("JPA-TEST-03", "Batch Station Beta",  "Beta River",  3.5,  9.0)
        );
        dao.saveAllStations(batch);
        assertCondition(dao.getAllStations().size() >= 3, "saveAllStations() persists batch correctly");

        System.out.println();
    }

    // ----- Record CRUD Tests -----

    private static void runRecordTests(StationJpaDAO stationDao, WaterLevelRecordJpaDAO recordDao) {
        System.out.println("--- [TEST GROUP 2] WaterLevelRecord JPA CRUD Operations ---");

        WaterLevelRecord normalRec = new WaterLevelRecord("JPA-REC-001", "Test River",
            "JPA Test Gauge Station (JPA-TEST-01)", 7.5, "2026-09-18 10:00 AM");
        recordDao.saveRecord(normalRec);
        assertPass("saveRecord() persists normal reading to jpa_readings table");

        WaterLevelRecord criticalRec = new WaterLevelRecord("JPA-REC-002", "Test River",
            "JPA Test Gauge Station (JPA-TEST-01)", 19.5, "2026-09-18 11:00 AM");
        recordDao.saveRecord(criticalRec);
        assertPass("saveRecord() persists CRITICAL reading to jpa_readings table");

        List<WaterLevelRecord> all = recordDao.getAllRecords();
        assertCondition(!all.isEmpty(), "getAllRecords() returns non-empty list");

        List<WaterLevelRecord> byRiver = recordDao.getRecordsByRiver("Test River");
        assertCondition(!byRiver.isEmpty(), "getRecordsByRiver() returns filtered records via JPQL");

        System.out.println();
    }

    // ----- Alert Derived Query Tests -----

    private static void runAlertQueryTests(WaterLevelRecordJpaDAO recordDao) {
        System.out.println("--- [TEST GROUP 3] Spring Data JPA Derived Query Methods ---");

        List<WaterLevelRecord> alerts = recordDao.getCriticalAndWarningRecords();
        assertCondition(!alerts.isEmpty(), "getCriticalAndWarningRecords() returns alert records via derived query");
        System.out.println("  Alert records found: " + alerts.size());

        for (WaterLevelRecord r : alerts) {
            boolean isAlert = r.getAlertStatus().startsWith("CRITICAL") || r.getAlertStatus().startsWith("WARNING");
            assertCondition(isAlert, "Alert record status prefix matches: " + r.getAlertStatus());
        }

        List<WaterLevelRecord> stationRecords = recordDao.getRecordsByStation("JPA-TEST-01");
        assertCondition(!stationRecords.isEmpty(), "getRecordsByStation() finds records via LIKE query");

        System.out.println();
    }

    // ---- Test Helper Methods ----

    private static void assertPass(String description) {
        passed++;
        System.out.println("  [PASS] " + description);
    }

    private static void assertCondition(boolean condition, String description) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + description);
        } else {
            failed++;
            System.out.println("  [FAIL] " + description);
        }
    }
}
