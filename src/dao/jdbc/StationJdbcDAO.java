package dao.jdbc;

import dao.StationDAO;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RiverStation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 10: JDBC Database Connectivity & Relational Data Layer
 * Syllabus Unit: UNIT V - java.sql.*, PreparedStatement, ResultSet, SQLException Handling
 * 
 * Relational database persistence implementation of {@link dao.StationDAO} using JDBC.
 */
public class StationJdbcDAO implements StationDAO {

    private final DatabaseConnectionManager dbManager;

    public StationJdbcDAO() {
        this(DatabaseConnectionManager.getInstance());
    }

    public StationJdbcDAO(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
        try {
            this.dbManager.initializeSchema();
        } catch (SQLException e) {
            System.err.println("[StationJdbcDAO] Failed to initialize table schema: " + e.getMessage());
        }
    }

    @Override
    public List<RiverStation> getAllStations() throws IOException {
        String sql = "SELECT station_id, station_name, river_name, normal_level, danger_level FROM stations";
        List<RiverStation> stations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String stationId = rs.getString("station_id");
                String stationName = rs.getString("station_name");
                String riverName = rs.getString("river_name");
                double normal = rs.getDouble("normal_level");
                double danger = rs.getDouble("danger_level");

                stations.add(new RiverStation(stationId, stationName, riverName, normal, danger));
            }
        } catch (SQLException e) {
            throw new IOException("JDBC StationDAO.getAllStations failed: " + e.getMessage(), e);
        }

        return stations;
    }

    @Override
    public RiverStation getStationById(String stationId) throws IOException {
        if (stationId == null || stationId.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT station_id, station_name, river_name, normal_level, danger_level FROM stations WHERE station_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stationId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("station_id");
                    String name = rs.getString("station_name");
                    String river = rs.getString("river_name");
                    double normal = rs.getDouble("normal_level");
                    double danger = rs.getDouble("danger_level");

                    return new RiverStation(id, name, river, normal, danger);
                }
            }
        } catch (SQLException e) {
            throw new IOException("JDBC StationDAO.getStationById failed: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void saveStation(RiverStation station) throws IOException {
        if (station == null) {
            throw new IllegalArgumentException("Cannot persist null station entity.");
        }

        if (existsById(station.getStationId())) {
            return;
        }

        String sql = "INSERT INTO stations (station_id, station_name, river_name, normal_level, danger_level) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, station.getStationId());
            ps.setString(2, station.getStationName());
            ps.setString(3, station.getRiverName());
            ps.setDouble(4, station.getNormalLevelMeters());
            ps.setDouble(5, station.getDangerLevelMeters());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("JDBC StationDAO.saveStation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAllStations(List<RiverStation> stations) throws IOException {
        if (stations == null) return;
        for (RiverStation s : stations) {
            saveStation(s);
        }
    }

    @Override
    public boolean existsById(String stationId) throws IOException {
        if (stationId == null || stationId.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM stations WHERE station_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stationId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IOException("JDBC StationDAO.existsById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageSource() {
        return "JDBC Relational Table: [stations] (" + dbManager.getJdbcUrl() + ")";
    }
}
