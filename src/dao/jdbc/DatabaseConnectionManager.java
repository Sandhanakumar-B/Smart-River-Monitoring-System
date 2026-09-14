package dao.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 10: JDBC Database Connectivity & Relational Data Layer
 * Syllabus Unit: UNIT V - JDBC Database Connectivity, DriverManager, Connection, PreparedStatement, Statement
 * 
 * Manages database connection lifecycle, DDL schema creation, and SQL utility execution.
 */
public class DatabaseConnectionManager {

    public static final String DEFAULT_JDBC_URL = "jdbc:smartriver://localhost/riverdb";
    private static DatabaseConnectionManager instance;
    private final String jdbcUrl;
    private Connection cachedConnection;

    private DatabaseConnectionManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        // Ensure driver class is loaded and registered with DriverManager
        try {
            Class.forName("dao.jdbc.RiverJdbcDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnectionManager] Driver not found on classpath: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager(DEFAULT_JDBC_URL);
        }
        return instance;
    }

    public static synchronized DatabaseConnectionManager getInstance(String jdbcUrl) {
        if (instance == null || !instance.jdbcUrl.equals(jdbcUrl)) {
            instance = new DatabaseConnectionManager(jdbcUrl);
        }
        return instance;
    }

    /**
     * Establishes or returns the active JDBC database connection.
     * Demonstrates {@link java.sql.DriverManager#getConnection(String)}.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (cachedConnection == null || cachedConnection.isClosed()) {
            cachedConnection = DriverManager.getConnection(jdbcUrl);
        }
        return cachedConnection;
    }

    /**
     * Executes DDL statements creating relational tables if they do not already exist.
     * Demonstrates {@link java.sql.Statement#execute(String)}.
     */
    public synchronized void initializeSchema() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create Stations Table
            String createStationsTableSql = 
                "CREATE TABLE IF NOT EXISTS stations (" +
                "  station_id VARCHAR(20) PRIMARY KEY, " +
                "  station_name VARCHAR(100), " +
                "  river_name VARCHAR(100), " +
                "  normal_level DOUBLE, " +
                "  danger_level DOUBLE" +
                ")";
            stmt.execute(createStationsTableSql);

            // 2. Create Readings Table
            String createReadingsTableSql = 
                "CREATE TABLE IF NOT EXISTS readings (" +
                "  record_id VARCHAR(30) PRIMARY KEY, " +
                "  river_name VARCHAR(100), " +
                "  station_location VARCHAR(150), " +
                "  water_level DOUBLE, " +
                "  alert_status VARCHAR(50), " +
                "  timestamp VARCHAR(50)" +
                ")";
            stmt.execute(createReadingsTableSql);
        }
    }

    /**
     * Inspects and returns diagnostic database and driver metadata.
     * Demonstrates {@link java.sql.DatabaseMetaData}.
     */
    public String getDatabaseDiagnostics() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            sb.append("=== JDBC DATABASE METADATA DIAGNOSTICS ===\n");
            sb.append(String.format("  Database Product Name  : %s\n", meta.getDatabaseProductName()));
            sb.append(String.format("  Database Version       : %s\n", meta.getDatabaseProductVersion()));
            sb.append(String.format("  JDBC Driver Name       : %s\n", meta.getDriverName()));
            sb.append(String.format("  JDBC Driver Version    : %s\n", meta.getDriverVersion()));
            sb.append(String.format("  JDBC URL               : %s\n", meta.getURL()));
            sb.append(String.format("  Active User            : %s\n", meta.getUserName()));
            sb.append(String.format("  Auto-Commit Enabled    : %b\n", conn.getAutoCommit()));
            sb.append(String.format("  Relational Tables      : stations, readings\n"));
        } catch (SQLException e) {
            sb.append("[JDBC Error] Failed to retrieve metadata: ").append(e.getMessage());
        }
        return sb.toString();
    }

    /**
     * Executes an arbitrary SQL query and returns formatted tabular output.
     * Demonstrates {@link java.sql.PreparedStatement}, {@link java.sql.ResultSet}, and {@link java.sql.ResultSetMetaData}.
     */
    public String executeQueryAndFormat(String sql) {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                headers.add(meta.getColumnName(i));
            }

            sb.append("\nSQL: ").append(sql).append("\n");
            sb.append(String.join(" | ", headers)).append("\n");
            sb.append("-".repeat(Math.max(25, headers.size() * 18))).append("\n");

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                List<String> rowValues = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    rowValues.add(val != null ? val : "NULL");
                }
                sb.append(String.join(" | ", rowValues)).append("\n");
            }
            sb.append(String.format("\n(%d row(s) returned)\n", rowCount));

        } catch (SQLException e) {
            sb.append("\n[SQL Execution Error] ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
