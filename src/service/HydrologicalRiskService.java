package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.RiverStation;
import model.WaterLevelRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 16: Real-Time Server-Sent Events (SSE) Live Telemetry & Hydrological Risk Prediction
 * Syllabus Unit: UNIT IV & V - Real-Time Event Modeling, Hydrological Prediction & Analytics
 *
 * Evaluates water level trend vectors (delta h / delta t), calculates rate of rise,
 * estimates crest arrival times, and generates proactive flood vulnerability scores.
 */
@Service
public class HydrologicalRiskService {

    private final RiverMonitoringService monitoringService;

    @Autowired
    public HydrologicalRiskService(RiverMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    public static class RiskAssessment {
        private final String stationId;
        private final String stationName;
        private final double currentLevel;
        private final double dangerLevel;
        private final double normalLevel;
        private final double rateOfRiseMph;      // meters per hour
        private final String riskLevel;           // SAFE, ELEVATED, WARNING, CRITICAL_SURGE
        private final int vulnerabilityScore;     // 0 to 100
        private final double estimatedCrestHours; // -1 if stable/falling
        private final String recommendation;

        public RiskAssessment(String stationId, String stationName, double currentLevel,
                              double dangerLevel, double normalLevel, double rateOfRiseMph,
                              String riskLevel, int vulnerabilityScore,
                              double estimatedCrestHours, String recommendation) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.currentLevel = currentLevel;
            this.dangerLevel = dangerLevel;
            this.normalLevel = normalLevel;
            this.rateOfRiseMph = rateOfRiseMph;
            this.riskLevel = riskLevel;
            this.vulnerabilityScore = vulnerabilityScore;
            this.estimatedCrestHours = estimatedCrestHours;
            this.recommendation = recommendation;
        }

        public String getStationId() { return stationId; }
        public String getStationName() { return stationName; }
        public double getCurrentLevel() { return currentLevel; }
        public double getDangerLevel() { return dangerLevel; }
        public double getNormalLevel() { return normalLevel; }
        public double getRateOfRiseMph() { return rateOfRiseMph; }
        public String getRiskLevel() { return riskLevel; }
        public int getVulnerabilityScore() { return vulnerabilityScore; }
        public double getEstimatedCrestHours() { return estimatedCrestHours; }
        public String getRecommendation() { return recommendation; }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("stationId", stationId);
            m.put("stationName", stationName);
            m.put("currentLevel", currentLevel);
            m.put("dangerLevel", dangerLevel);
            m.put("normalLevel", normalLevel);
            m.put("rateOfRiseMph", Math.round(rateOfRiseMph * 100.0) / 100.0);
            m.put("riskLevel", riskLevel);
            m.put("vulnerabilityScore", vulnerabilityScore);
            m.put("estimatedCrestHours", estimatedCrestHours > 0 ? Math.round(estimatedCrestHours * 10.0) / 10.0 : null);
            m.put("recommendation", recommendation);
            return m;
        }
    }

    /**
     * Assesses hydrological flood risk for a specified station.
     */
    public RiskAssessment assessStationRisk(RiverStation station) {
        if (station == null) return null;

        List<WaterLevelRecord> records = monitoringService.getAllRecords();
        List<WaterLevelRecord> stationRecords = new ArrayList<>();
        for (WaterLevelRecord r : records) {
            if (r.getStationLocation().contains(station.getStationId()) ||
                r.getStationLocation().contains(station.getStationName())) {
                stationRecords.add(r);
            }
        }

        double currentLevel = station.getNormalLevelMeters();
        double previousLevel = currentLevel;
        if (!stationRecords.isEmpty()) {
            currentLevel = stationRecords.get(stationRecords.size() - 1).getWaterLevelMeters();
            if (stationRecords.size() >= 2) {
                previousLevel = stationRecords.get(stationRecords.size() - 2).getWaterLevelMeters();
            }
        }

        // Rate of rise estimation (simulated tick delta converted to an hourly metric)
        double deltaMeters = currentLevel - previousLevel;
        double rateOfRiseMph = Math.max(-5.0, Math.min(10.0, deltaMeters * 4.0)); // scaling factor for telemetry ticks

        double danger = station.getDangerLevelMeters();
        double normal = station.getNormalLevelMeters();

        // Calculate vulnerability score (0 - 100)
        double ratio = (currentLevel - normal) / Math.max(0.1, danger - normal);
        int score = (int) Math.round(Math.max(0, Math.min(100, (ratio * 70.0) + (rateOfRiseMph > 0 ? rateOfRiseMph * 15.0 : 0))));

        String riskLevel;
        String recommendation;
        double crestHours = -1.0;

        if (currentLevel >= danger || score >= 85) {
            riskLevel = "CRITICAL_SURGE";
            recommendation = "🚨 High Flood Alert: Activate emergency sirens, notify downstream barrages, evacuate floodplains.";
            crestHours = 0.0;
        } else if (currentLevel >= (normal + (danger - normal) * 0.75) || score >= 65) {
            riskLevel = "WARNING";
            recommendation = "⚠️ Flood Warning: River approaching danger mark. Deploy sandbags and monitor crest velocity.";
            if (rateOfRiseMph > 0.1) {
                crestHours = (danger - currentLevel) / rateOfRiseMph;
            }
        } else if (currentLevel > normal || score >= 35) {
            riskLevel = "ELEVATED";
            recommendation = "ℹ️ Elevated Water Level: Surface runoff detected. Telemetry frequency increased to high-precision mode.";
            if (rateOfRiseMph > 0.2) {
                crestHours = (danger - currentLevel) / rateOfRiseMph;
            }
        } else {
            riskLevel = "SAFE";
            recommendation = "✅ River within nominal baseline capacity. Flow conditions tranquil.";
        }

        return new RiskAssessment(
            station.getStationId(),
            station.getStationName(),
            currentLevel,
            danger,
            normal,
            rateOfRiseMph,
            riskLevel,
            score,
            crestHours,
            recommendation
        );
    }

    /**
     * Computes basin-wide risk overview across all registered stations.
     */
    public List<RiskAssessment> assessAllStationsRisk() {
        List<RiverStation> stations = monitoringService.getAllStations();
        List<RiskAssessment> assessments = new ArrayList<>();
        for (RiverStation s : stations) {
            assessments.add(assessStationRisk(s));
        }
        return assessments;
    }
}
