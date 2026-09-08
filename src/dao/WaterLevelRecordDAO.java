package dao;

import java.io.IOException;
import java.util.List;
import model.WaterLevelRecord;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 7: Data Access Object (DAO) Pattern - Water Level Measurement Persistence Interface
 * Syllabus Unit: UNIT III & UNIT IV - Interfaces, Data Access Abstraction, File I/O
 */
public interface WaterLevelRecordDAO {

    /**
     * Retrieves all persisted water level measurement records.
     * @return List of WaterLevelRecord entities
     * @throws IOException if storage cannot be read
     */
    List<WaterLevelRecord> getAllRecords() throws IOException;

    /**
     * Persists a single new water level measurement record.
     * @param record The record to save
     * @throws IOException if writing to storage fails
     */
    void saveRecord(WaterLevelRecord record) throws IOException;

    /**
     * Saves a list of water level records (batch rewrite/sync).
     * @param records List of records to persist
     * @throws IOException if writing fails
     */
    void saveAllRecords(List<WaterLevelRecord> records) throws IOException;

    /**
     * Retrieves records filtered by river name.
     * @param riverName Name of the river
     * @return List of matching records
     * @throws IOException if storage cannot be accessed
     */
    List<WaterLevelRecord> getRecordsByRiver(String riverName) throws IOException;

    /**
     * Returns the physical storage source description (e.g., file path).
     */
    String getStorageSource();
}
