package dao.jdbc;

import dao.WaterLevelRecordDAO;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 10: JDBC Database Connectivity & Relational Data Layer
 * Syllabus Unit: UNIT V - java.sql.*, PreparedStatement, ResultSet, Parameterized SQL Queries
 * 
 * Relational database persistence implementation of {@link dao.WaterLevelRecordDAO} using JDBC.
 */
public class WaterLevelRecordJdbcDAO implements WaterLevelRecordDAO {

    private final DatabaseConnectionManager dbManager;

    public WaterLevelRecordJdbcDAO() {
        this(DatabaseConnectionManager.getInstance());
    }

    public WaterLevelRecordJdbcDAO(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
        try {
            this.dbManager.initializeSchema();
        } catch (SQLException e) {
            System.err.println("[WaterLevelRecordJdbcDAO] Failed to initialize table schema: " + e.getMessage());
        }
    }

    @Override
    public List<WaterLevelRecord> getAllRecords() throws IOException {
        String sql = "SELECT record_id, river_name, station_location, water_level, alert_status, timestamp FROM readings";
        List<WaterLevelRecord> records = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String recordId = rs.getString("record_id");
                String riverName = rs.getString("river_name");
                String stationLocation = rs.getString("station_location");
                double level = rs.getDouble("water_level");
                String timestamp = rs.getString("timestamp");

                records.add(new WaterLevelRecord(recordId, riverName, stationLocation, level, timestamp));
            }
        } catch (SQLException e) {
            throw new IOException("JDBC WaterLevelRecordDAO.getAllRecords failed: " + e.getMessage(), e);
        }

        return records;
    }

    @Override
    public void saveRecord(WaterLevelRecord record) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("Cannot persist null water level record.");
        }

        String sql = "INSERT INTO readings (record_id, river_name, station_location, water_level, alert_status, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.getRecordId());
            ps.setString(2, record.getRiverName());
            ps.setString(3, record.getStationLocation());
            ps.setDouble(4, record.getWaterLevelMeters());
            ps.setString(5, record.getAlertStatus());
            ps.setString(6, record.getTimestamp());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("JDBC WaterLevelRecordDAO.saveRecord failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAllRecords(List<WaterLevelRecord> records) throws IOException {
        if (records == null) return;
        for (WaterLevelRecord r : records) {
            saveRecord(r);
        }
    }

    @Override
    public List<WaterLevelRecord> getRecordsByRiver(String riverName) throws IOException {
        if (riverName == null || riverName.trim().isEmpty()) {
            return getAllRecords();
        }

        String sql = "SELECT record_id, river_name, station_location, water_level, alert_status, timestamp FROM readings WHERE river_name = ?";
        List<WaterLevelRecord> records = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, riverName.trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String recordId = rs.getString("record_id");
                    String rName = rs.getString("river_name");
                    String stationLocation = rs.getString("station_location");
                    double level = rs.getDouble("water_level");
                    String timestamp = rs.getString("timestamp");

                    records.add(new WaterLevelRecord(recordId, rName, stationLocation, level, timestamp));
                }
            }
        } catch (SQLException e) {
            throw new IOException("JDBC WaterLevelRecordDAO.getRecordsByRiver failed: " + e.getMessage(), e);
        }

        return records;
    }

    @Override
    public String getStorageSource() {
        return "JDBC Relational Table: [readings] (" + dbManager.getJdbcUrl() + ")";
    }
}
