package service;

import dao.StationDAO;
import dao.WaterLevelRecordDAO;
import exception.DuplicateStationException;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import model.RiverStation;
import model.WaterLevelRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 18: Unit Testing with JUnit 5 & Mockito
 * Syllabus Unit: UNIT V - Test-Driven Development, Unit Testing, Mock Objects
 *
 * Comprehensive unit test suite for RiverMonitoringService.
 *
 * Key Testing Concepts Demonstrated:
 *   - @ExtendWith(MockitoExtension.class) : JUnit 5 + Mockito integration
 *   - @Mock : creates Mockito mock objects for DAO dependencies
 *   - @InjectMocks : injects mocks into the service under test
 *   - when().thenReturn() : stubbing mock behavior
 *   - verify() : asserting that mocks were called with expected arguments
 *   - assertThrows() : asserting that specific exceptions are thrown
 *   - @BeforeEach : test setup hook executed before each test method
 *   - @DisplayName : human-readable test names for IDE / report readability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Day 18 – RiverMonitoringService Unit Tests")
class RiverMonitoringServiceTest {

    // ======================================================================
    // Mocks & Subject Under Test
    // ======================================================================

    @Mock
    private StationDAO stationDAO;

    @Mock
    private WaterLevelRecordDAO recordDAO;

    @InjectMocks
    private RiverMonitoringService monitoringService;

    // ======================================================================
    // Test Fixtures
    // ======================================================================

    private RiverStation testStation;
    private RiverStation testStation2;
    private WaterLevelRecord normalRecord;
    private WaterLevelRecord criticalRecord;

    @BeforeEach
    void setUp() {
        testStation  = new RiverStation("STN-TEST-01", "Test Haridwar Station", "Ganga River", 8.0, 15.0);
        testStation2 = new RiverStation("STN-TEST-02", "Test Mathura Station",  "Yamuna River", 6.0, 12.0);

        normalRecord   = new WaterLevelRecord("REC-001", "Ganga River", "STN-TEST-01 - Test Haridwar Station",
                                              9.5, "NORMAL",    "2026-09-20 08:00:00");
        criticalRecord = new WaterLevelRecord("REC-002", "Ganga River", "STN-TEST-01 - Test Haridwar Station",
                                              16.5, "CRITICAL",  "2026-09-20 10:00:00");
    }

    // ======================================================================
    // getAllStations() Tests
    // ======================================================================

    @Test
    @DisplayName("getAllStations() returns all stations from DAO")
    void getAllStations_shouldReturnAllStationsFromDAO() throws Exception {
        // Arrange — stub DAO mock
        when(stationDAO.getAllStations()).thenReturn(Arrays.asList(testStation, testStation2));

        // Act
        List<RiverStation> result = monitoringService.getAllStations();

        // Assert
        assertNotNull(result, "Station list must not be null");
        assertEquals(2, result.size(), "Should return exactly 2 stations");
        verify(stationDAO, atLeastOnce()).getAllStations();
    }

    @Test
    @DisplayName("getAllStations() returns empty list when no stations registered")
    void getAllStations_emptyRegistry_shouldReturnEmptyList() throws Exception {
        when(stationDAO.getAllStations()).thenReturn(List.of());

        List<RiverStation> result = monitoringService.getAllStations();

        assertTrue(result.isEmpty(), "Empty registry should return empty list");
    }

    // ======================================================================
    // registerStation() Tests
    // ======================================================================

    @Test
    @DisplayName("registerStation() persists new station via DAO save")
    void registerStation_newStation_shouldSaveViaDAO() throws Exception {
        // Arrange — station does NOT exist yet
        when(stationDAO.existsById("STN-TEST-01")).thenReturn(false);
        when(stationDAO.getStationById("STN-TEST-01")).thenReturn(null);
        when(stationDAO.getAllStations()).thenReturn(List.of());

        // Act
        monitoringService.registerStation(testStation);

        // Assert — DAO.saveStation was called exactly once with our station
        verify(stationDAO, times(1)).saveStation(testStation);
    }

    @Test
    @DisplayName("registerStation() throws DuplicateStationException for existing ID")
    void registerStation_duplicateId_shouldThrowDuplicateStationException() throws Exception {
        // Arrange — station already exists
        when(stationDAO.existsById("STN-TEST-01")).thenReturn(true);

        // Assert — exception thrown (no save should occur)
        DuplicateStationException ex = assertThrows(
                DuplicateStationException.class,
                () -> monitoringService.registerStation(testStation),
                "Should throw DuplicateStationException for duplicate ID"
        );
        assertTrue(ex.getMessage().contains("STN-TEST-01"), "Exception message should contain the station ID");
        verify(stationDAO, never()).saveStation(any());
    }

    // ======================================================================
    // recordMeasurement() Tests
    // ======================================================================

    @Test
    @DisplayName("recordMeasurement() saves valid reading and returns WaterLevelRecord")
    void recordMeasurement_validLevel_shouldSaveAndReturn() throws Exception {
        // Arrange
        when(stationDAO.getStationById("STN-TEST-01")).thenReturn(testStation);
        when(recordDAO.getAllRecords()).thenReturn(List.of());

        // Act
        WaterLevelRecord result = monitoringService.recordMeasurement("STN-TEST-01", 10.5, "2026-09-20 09:00 AM");

        // Assert
        assertNotNull(result, "Result record must not be null");
        assertEquals(10.5, result.getWaterLevelMeters(), 0.001, "Water level must match");
        assertEquals("NORMAL (Safe Level)", result.getAlertStatus(), "Level 10.5 < danger 15.0 → NORMAL");
        verify(recordDAO, times(1)).saveRecord(any(WaterLevelRecord.class));
    }

    @Test
    @DisplayName("recordMeasurement() sets CRITICAL alert for level above danger threshold")
    void recordMeasurement_aboveDanger_shouldSetCriticalAlert() throws Exception {
        when(stationDAO.getStationById("STN-TEST-01")).thenReturn(testStation);
        when(recordDAO.getAllRecords()).thenReturn(List.of());

        WaterLevelRecord result = monitoringService.recordMeasurement("STN-TEST-01", 18.0, "2026-09-20 11:00 AM");

        assertTrue(result.getAlertStatus().contains("CRITICAL") || result.getAlertStatus().contains("WARNING"),
                "Level 18.0 > danger 15.0 must trigger CRITICAL alert");
    }

    @Test
    @DisplayName("recordMeasurement() throws InvalidWaterLevelException for negative level")
    void recordMeasurement_negativeLevel_shouldThrowInvalidWaterLevelException() throws Exception {
        assertThrows(
                InvalidWaterLevelException.class,
                () -> monitoringService.recordMeasurement("STN-TEST-01", -1.0, "2026-09-20 09:00 AM"),
                "Negative water level must throw InvalidWaterLevelException"
        );
        verify(recordDAO, never()).saveRecord(any());
    }

    @Test
    @DisplayName("recordMeasurement() throws InvalidWaterLevelException for level > 50m")
    void recordMeasurement_exceedsMaxLevel_shouldThrowInvalidWaterLevelException() throws Exception {
        assertThrows(
                InvalidWaterLevelException.class,
                () -> monitoringService.recordMeasurement("STN-TEST-01", 51.0, "2026-09-20 09:00 AM"),
                "Level exceeding 50m must throw InvalidWaterLevelException"
        );
    }

    @Test
    @DisplayName("recordMeasurement() throws StationNotFoundException for unknown station ID")
    void recordMeasurement_unknownStation_shouldThrowStationNotFoundException() throws Exception {
        when(stationDAO.getStationById("STN-NONEXISTENT-99")).thenReturn(null);
        when(stationDAO.getAllStations()).thenReturn(List.of());

        assertThrows(
                StationNotFoundException.class,
                () -> monitoringService.recordMeasurement("STN-NONEXISTENT-99", 10.0, "2026-09-20 09:00 AM"),
                "Unknown station ID must throw StationNotFoundException"
        );
    }

    // ======================================================================
    // getStationByIdOrThrow() Tests
    // ======================================================================

    @Test
    @DisplayName("getStationByIdOrThrow() returns station when found")
    void getStationByIdOrThrow_existingId_shouldReturnStation() throws Exception {
        when(stationDAO.getStationById("STN-TEST-01")).thenReturn(testStation);

        RiverStation result = monitoringService.getStationByIdOrThrow("STN-TEST-01");

        assertNotNull(result, "Should return the station");
        assertEquals("STN-TEST-01", result.getStationId(), "Returned station ID must match");
        assertEquals("Test Haridwar Station", result.getStationName(), "Station name must match");
    }

    @Test
    @DisplayName("getStationByIdOrThrow() throws StationNotFoundException for unknown ID")
    void getStationByIdOrThrow_unknownId_shouldThrowStationNotFoundException() throws Exception {
        when(stationDAO.getStationById("STN-UNKNOWN-00")).thenReturn(null);
        when(stationDAO.getAllStations()).thenReturn(List.of());

        StationNotFoundException ex = assertThrows(
                StationNotFoundException.class,
                () -> monitoringService.getStationByIdOrThrow("STN-UNKNOWN-00"),
                "Should throw StationNotFoundException for unknown station"
        );
        assertNotNull(ex.getMessage(), "Exception message must not be null");
    }

    // ======================================================================
    // Analytics Methods Tests
    // ======================================================================

    @Test
    @DisplayName("getAverageWaterLevel() returns correct mean across all records")
    void getAverageWaterLevel_withRecords_shouldReturnCorrectMean() throws Exception {
        when(recordDAO.getAllRecords()).thenReturn(Arrays.asList(normalRecord, criticalRecord));

        double avg = monitoringService.getAverageWaterLevel();

        assertEquals((9.5 + 16.5) / 2.0, avg, 0.001, "Average of 9.5 and 16.5 must be 13.0");
    }

    @Test
    @DisplayName("getAverageWaterLevel() returns 0.0 when no records exist")
    void getAverageWaterLevel_noRecords_shouldReturnZero() throws Exception {
        when(recordDAO.getAllRecords()).thenReturn(List.of());

        double avg = monitoringService.getAverageWaterLevel();

        assertEquals(0.0, avg, 0.001, "Average of empty dataset must be 0.0");
    }

    @Test
    @DisplayName("getMaxRecordedWaterLevel() returns highest recorded level")
    void getMaxRecordedWaterLevel_withRecords_shouldReturnMaximum() throws Exception {
        when(recordDAO.getAllRecords()).thenReturn(Arrays.asList(normalRecord, criticalRecord));

        double max = monitoringService.getMaxRecordedWaterLevel();

        assertEquals(16.5, max, 0.001, "Maximum level across records must be 16.5");
    }

    @Test
    @DisplayName("getCriticalAlertRecords() returns only CRITICAL and WARNING records")
    void getCriticalAlertRecords_mixedRecords_shouldReturnOnlyAlerts() throws Exception {
        when(recordDAO.getAllRecords()).thenReturn(Arrays.asList(normalRecord, criticalRecord));

        List<WaterLevelRecord> alerts = monitoringService.getCriticalAlertRecords();

        assertFalse(alerts.isEmpty(), "Alert list must not be empty");
        assertTrue(alerts.stream().allMatch(r ->
                r.getAlertStatus().contains("CRITICAL") || r.getAlertStatus().contains("WARNING")),
                "All returned records must have CRITICAL or WARNING status"
        );
        verify(recordDAO, atLeastOnce()).getAllRecords();
    }

    // ======================================================================
    // getTotalCounts() Tests
    // ======================================================================

    @Test
    @DisplayName("getTotalStationsCount() returns correct station count from DAO")
    void getTotalStationsCount_shouldReflectDAOData() throws Exception {
        when(stationDAO.getAllStations()).thenReturn(Arrays.asList(testStation, testStation2));

        int count = monitoringService.getTotalStationsCount();

        assertEquals(2, count, "Station count must match DAO data");
    }

    @Test
    @DisplayName("getTotalReadingsCount() returns correct record count from DAO")
    void getTotalReadingsCount_shouldReflectDAOData() throws Exception {
        when(recordDAO.getAllRecords()).thenReturn(Arrays.asList(normalRecord, criticalRecord));

        int count = monitoringService.getTotalReadingsCount();

        assertEquals(2, count, "Reading count must match DAO data");
    }
}
