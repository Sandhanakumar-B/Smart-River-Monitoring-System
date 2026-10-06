package service;

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
import static org.mockito.Mockito.*;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 18: Unit Testing with JUnit 5 & Mockito
 * Syllabus Unit: UNIT V - Isolation Testing, Mockito Service Mocking, Risk Score Validation
 *
 * Unit tests for HydrologicalRiskService demonstrating:
 *   - Mocking the RiverMonitoringService dependency
 *   - Testing risk level classification logic
 *   - Verifying vulnerability score calculation bounds
 *   - Testing edge cases: empty records, surge scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Day 18 – HydrologicalRiskService Unit Tests")
class HydrologicalRiskServiceTest {

    @Mock
    private RiverMonitoringService monitoringService;

    @InjectMocks
    private HydrologicalRiskService riskService;

    private RiverStation normalStation;
    private RiverStation criticalStation;

    @BeforeEach
    void setUp() {
        normalStation   = new RiverStation("STN-RISK-01", "Risk Test Normal", "Ganga River", 8.0, 15.0);
        criticalStation = new RiverStation("STN-RISK-02", "Risk Test Critical", "Yamuna River", 6.0, 12.0);
    }

    @Test
    @DisplayName("assessStationRisk() returns SAFE when no records and normal baseline")
    void assessStationRisk_noRecords_shouldReturnSafe() {
        when(monitoringService.getAllRecords()).thenReturn(List.of());

        HydrologicalRiskService.RiskAssessment result = riskService.assessStationRisk(normalStation);

        assertNotNull(result, "Risk assessment must not be null");
        assertEquals("SAFE", result.getRiskLevel(), "No readings → should be SAFE at baseline");
        assertEquals("STN-RISK-01", result.getStationId(), "Station ID must match");
        assertTrue(result.getVulnerabilityScore() >= 0 && result.getVulnerabilityScore() <= 100,
                "Vulnerability score must be 0-100");
    }

    @Test
    @DisplayName("assessStationRisk() escalates to CRITICAL_SURGE above danger level")
    void assessStationRisk_aboveDanger_shouldEscalateToCriticalSurge() {
        WaterLevelRecord surgeRecord = new WaterLevelRecord(
                "REC-SURGE-01", "Ganga River",
                "STN-RISK-01 - Risk Test Normal",
                16.5, "CRITICAL", "2026-09-25 10:00:00");

        when(monitoringService.getAllRecords()).thenReturn(List.of(surgeRecord));

        HydrologicalRiskService.RiskAssessment result = riskService.assessStationRisk(normalStation);

        assertEquals("CRITICAL_SURGE", result.getRiskLevel(), "Level > danger should be CRITICAL_SURGE");
        assertTrue(result.getVulnerabilityScore() >= 85, "Critical surge score must be >= 85");
        assertTrue(result.getRecommendation().contains("Flood Alert") || result.getRecommendation().contains("flood"),
                "Recommendation must contain flood alert message");
    }

    @Test
    @DisplayName("assessStationRisk() returns null for null station input")
    void assessStationRisk_nullStation_shouldReturnNull() {
        HydrologicalRiskService.RiskAssessment result = riskService.assessStationRisk(null);
        assertNull(result, "Null station input should return null assessment");
    }

    @Test
    @DisplayName("assessAllStationsRisk() evaluates exactly as many stations as service returns")
    void assessAllStationsRisk_twoStations_shouldReturnTwoAssessments() {
        when(monitoringService.getAllStations()).thenReturn(Arrays.asList(normalStation, criticalStation));
        when(monitoringService.getAllRecords()).thenReturn(List.of());

        List<HydrologicalRiskService.RiskAssessment> results = riskService.assessAllStationsRisk();

        assertEquals(2, results.size(), "Should produce one assessment per station");
        assertTrue(results.stream().allMatch(r -> r != null), "All assessments must be non-null");
        verify(monitoringService, times(1)).getAllStations();
    }

    @Test
    @DisplayName("RiskAssessment.toMap() contains all required keys")
    void riskAssessment_toMap_shouldContainAllRequiredKeys() {
        when(monitoringService.getAllRecords()).thenReturn(List.of());

        HydrologicalRiskService.RiskAssessment result = riskService.assessStationRisk(normalStation);
        var map = result.toMap();

        assertAll("toMap() must contain all expected keys",
                () -> assertTrue(map.containsKey("stationId")),
                () -> assertTrue(map.containsKey("stationName")),
                () -> assertTrue(map.containsKey("currentLevel")),
                () -> assertTrue(map.containsKey("dangerLevel")),
                () -> assertTrue(map.containsKey("riskLevel")),
                () -> assertTrue(map.containsKey("vulnerabilityScore")),
                () -> assertTrue(map.containsKey("recommendation"))
        );
    }

    @Test
    @DisplayName("assessStationRisk() sets ELEVATED risk when level is between normal and 75% of danger range")
    void assessStationRisk_elevatedRange_shouldReturnElevated() {
        // Level at 9.5 is above normal (8.0) but well below danger (15.0)
        WaterLevelRecord elevatedRecord = new WaterLevelRecord(
                "REC-ELEV-01", "Ganga River",
                "STN-RISK-01 - Risk Test Normal",
                9.5, "WARNING", "2026-09-25 10:00:00");

        when(monitoringService.getAllRecords()).thenReturn(List.of(elevatedRecord));

        HydrologicalRiskService.RiskAssessment result = riskService.assessStationRisk(normalStation);

        // Level is above normal — should not be SAFE
        assertNotEquals("SAFE", result.getRiskLevel(), "Level above normal must not be SAFE");
        // Score should be positive
        assertTrue(result.getVulnerabilityScore() > 0, "Elevated level score must be > 0");
    }
}
