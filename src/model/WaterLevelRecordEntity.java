package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 14: Spring Data JPA & H2 Database Integration
 * Syllabus Unit: UNIT V - JPA Entity Mapping, Hibernate ORM, H2 Embedded Database
 *
 * JPA Entity that maps the domain class WaterLevelRecord to the 'jpa_readings' relational table.
 * Demonstrates @Entity, @Table, @Id, and @Column ORM annotations.
 */
@Entity
@Table(name = "jpa_readings")
public class WaterLevelRecordEntity {

    /** Primary key - domain-assigned record ID (e.g. "REC-1001") */
    @Id
    @Column(name = "record_id", nullable = false, unique = true, length = 50)
    private String recordId;

    @Column(name = "river_name", nullable = false, length = 80)
    private String riverName;

    /** Stores "StationName (StationId)" composite string matching domain model convention */
    @Column(name = "station_location", nullable = false, length = 150)
    private String stationLocation;

    @Column(name = "water_level_meters", nullable = false)
    private double waterLevelMeters;

    @Column(name = "timestamp", nullable = false, length = 60)
    private String timestamp;

    @Column(name = "alert_status", nullable = false, length = 60)
    private String alertStatus;

    /** No-arg constructor required by JPA spec */
    public WaterLevelRecordEntity() {}

    public WaterLevelRecordEntity(String recordId, String riverName, String stationLocation,
                                   double waterLevelMeters, String timestamp, String alertStatus) {
        this.recordId        = recordId;
        this.riverName       = riverName;
        this.stationLocation = stationLocation;
        this.waterLevelMeters = waterLevelMeters;
        this.timestamp       = timestamp;
        this.alertStatus     = alertStatus;
    }

    // ---- Static factory: domain object -> JPA entity ----

    /**
     * Converts a plain domain WaterLevelRecord into a JPA-managed entity.
     */
    public static WaterLevelRecordEntity fromDomain(WaterLevelRecord record) {
        return new WaterLevelRecordEntity(
            record.getRecordId(),
            record.getRiverName(),
            record.getStationLocation(),
            record.getWaterLevelMeters(),
            record.getTimestamp(),
            record.getAlertStatus()
        );
    }

    /**
     * Converts this JPA entity back to the plain domain WaterLevelRecord object.
     */
    public WaterLevelRecord toDomain() {
        WaterLevelRecord r = new WaterLevelRecord(
            recordId, riverName, stationLocation, waterLevelMeters, timestamp
        );
        return r;
    }

    // ---- Getters & Setters ----

    public String getRecordId()                     { return recordId; }
    public void   setRecordId(String v)             { this.recordId = v; }

    public String getRiverName()                    { return riverName; }
    public void   setRiverName(String v)            { this.riverName = v; }

    public String getStationLocation()              { return stationLocation; }
    public void   setStationLocation(String v)      { this.stationLocation = v; }

    public double getWaterLevelMeters()             { return waterLevelMeters; }
    public void   setWaterLevelMeters(double v)     { this.waterLevelMeters = v; }

    public String getTimestamp()                    { return timestamp; }
    public void   setTimestamp(String v)            { this.timestamp = v; }

    public String getAlertStatus()                  { return alertStatus; }
    public void   setAlertStatus(String v)          { this.alertStatus = v; }

    @Override
    public String toString() {
        return "WaterLevelRecordEntity[id=" + recordId + ", level=" + waterLevelMeters
               + "m, status=" + alertStatus + "]";
    }
}
