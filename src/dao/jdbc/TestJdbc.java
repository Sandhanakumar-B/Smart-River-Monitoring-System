package dao.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import model.RiverStation;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 10: JDBC Database Connectivity & Relational Data Layer
 * Syllabus Unit: UNIT V - Verification Test Suite for JDBC Database Operations
 * 
 * Standalone verification runner for testing JDBC driver, connections, PreparedStatements,
 * ResultSets, and Relational DAOs.
 */
public class TestJdbc {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   DAY 10 JDBC VERIFICATION & RELATIONAL PERSISTENCE TEST   ");
        System.out.println("============================================================");

        int passed = 0;
        int failed = 0;

        try {
            // Test 1: Driver Registration and Connection Acquisition
            System.out.println("\n[Test 1] Acquiring JDBC Connection via DriverManager...");
            DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();
            Connection conn = dbManager.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("  PASS: Successfully connected to " + dbManager.getJdbcUrl());
                passed++;
            } else {
                System.err.println("  FAIL: Connection is null or closed.");
                failed++;
            }

            // Test 2: Database Metadata Retrieval
            System.out.println("\n[Test 2] Inspecting DatabaseMetaData...");
            DatabaseMetaData meta = conn.getMetaData();
            String productName = meta.getDatabaseProductName();
            String driverName = meta.getDriverName();
            if (productName != null && driverName != null) {
                System.out.println("  PASS: Product=" + productName + ", Driver=" + driverName);
                passed++;
            } else {
                System.err.println("  FAIL: Metadata returned null values.");
                failed++;
            }

            // Test 3: Schema DDL Execution
            System.out.println("\n[Test 3] Initializing Relational Schema (DDL CREATE TABLE)...");
            dbManager.initializeSchema();
            System.out.println("  PASS: Schema tables 'stations' and 'readings' ready.");
            passed++;

            // Test 4: PreparedStatement Insert into stations
            System.out.println("\n[Test 4] Testing StationJdbcDAO saveStation & getStationById...");
            StationJdbcDAO stationDAO = new StationJdbcDAO(dbManager);
            RiverStation testStation = new RiverStation("RS-TEST-01", "Kaveri Test Station", "Kaveri", 4.5, 13.5);
            stationDAO.saveStation(testStation);

            RiverStation retrievedStation = stationDAO.getStationById("RS-TEST-01");
            if (retrievedStation != null && "Kaveri Test Station".equals(retrievedStation.getStationName())) {
                System.out.println("  PASS: Retrieved station matches inserted entity: " + retrievedStation.getStationName());
                passed++;
            } else {
                System.err.println("  FAIL: Station retrieval failed or mismatched.");
                failed++;
            }

            // Test 5: PreparedStatement Insert into readings & ResultSet traversal
            System.out.println("\n[Test 5] Testing WaterLevelRecordJdbcDAO saveRecord & getAllRecords...");
            WaterLevelRecordJdbcDAO recordDAO = new WaterLevelRecordJdbcDAO(dbManager);
            WaterLevelRecord testRecord = new WaterLevelRecord("REC-TEST-99", "Kaveri", "Kaveri Test Station (RS-TEST-01)", 10.2, "2026-09-14 04:45:00 PM");
            recordDAO.saveRecord(testRecord);

            List<WaterLevelRecord> records = recordDAO.getRecordsByRiver("Kaveri");
            if (!records.isEmpty() && records.get(0).getWaterLevelMeters() == 10.2) {
                System.out.println("  PASS: Successfully queried record by river filter. Found: " + records.size() + " record(s).");
                passed++;
            } else {
                System.err.println("  FAIL: Query by river filter returned unexpected results.");
                failed++;
            }

            // Test 6: Custom SQL Query Console Execution
            System.out.println("\n[Test 6] Testing Arbitrary SQL Query Execution...");
            String queryResult = dbManager.executeQueryAndFormat("SELECT station_id, station_name, normal_level FROM stations");
            System.out.println(queryResult);
            if (queryResult.contains("RS-TEST-01")) {
                System.out.println("  PASS: Custom SQL query returned expected tabular data.");
                passed++;
            } else {
                System.err.println("  FAIL: Custom query output missing test station.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("Unexpected exception in JDBC verification: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }

        System.out.println("\n============================================================");
        System.out.printf("JDBC VERIFICATION SUMMARY: %d PASSED, %d FAILED\n", passed, failed);
        System.out.println("============================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
