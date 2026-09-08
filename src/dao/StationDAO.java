package dao;

import java.io.IOException;
import java.util.List;
import model.RiverStation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 7: Data Access Object (DAO) Pattern - Station Persistence Interface
 * Syllabus Unit: UNIT III & UNIT IV - Interfaces, Data Access Abstraction, File I/O
 */
public interface StationDAO {

    /**
     * Retrieves all persisted river monitoring stations.
     * @return List of RiverStation entities
     * @throws IOException if storage cannot be read
     */
    List<RiverStation> getAllStations() throws IOException;

    /**
     * Finds a station by its unique identifier.
     * @param stationId Unique identifier of the station
     * @return RiverStation if found, null otherwise
     * @throws IOException if storage cannot be accessed
     */
    RiverStation getStationById(String stationId) throws IOException;

    /**
     * Persists a new station to permanent storage.
     * @param station The station entity to save
     * @throws IOException if writing to storage fails
     */
    void saveStation(RiverStation station) throws IOException;

    /**
     * Saves a list of stations to permanent storage (batch rewrite/sync).
     * @param stations List of stations to persist
     * @throws IOException if writing fails
     */
    void saveAllStations(List<RiverStation> stations) throws IOException;

    /**
     * Checks if a station with the given ID already exists in storage.
     * @param stationId Unique station ID
     * @return true if exists, false otherwise
     * @throws IOException if storage access fails
     */
    boolean existsById(String stationId) throws IOException;

    /**
     * Returns the physical storage source description (e.g., file path).
     */
    String getStorageSource();
}
