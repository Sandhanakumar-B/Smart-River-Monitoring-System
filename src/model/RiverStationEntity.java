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
 * JPA Entity that maps the domain class RiverStation to the 'jpa_stations' relational table.
 * Demonstrates @Entity, @Table, @Id, and @Column ORM annotations.
 * Uses static factory methods for clean domain <-> entity conversion.
 */
@Entity
@Table(name = "jpa_stations")
public class RiverStationEntity {

    /** @Id marks the primary key; no @GeneratedValue since stationId is a domain-assigned String */
    @Id
    @Column(name = "station_id", nullable = false, unique = true, length = 50)
    private String stationId;

    @Column(name = "station_name", nullable = false, length = 120)
    private String stationName;

    @Column(name = "river_name", nullable = false, length = 80)
    private String riverName;

    @Column(name = "normal_level_meters", nullable = false)
    private double normalLevelMeters;

    @Column(name = "danger_level_meters", nullable = false)
    private double dangerLevelMeters;

    /** No-arg constructor required by JPA spec (Hibernate uses reflection to instantiate entities) */
    public RiverStationEntity() {}

    public RiverStationEntity(String stationId, String stationName, String riverName,
                               double normalLevelMeters, double dangerLevelMeters) {
        this.stationId         = stationId;
        this.stationName       = stationName;
        this.riverName         = riverName;
        this.normalLevelMeters = normalLevelMeters;
        this.dangerLevelMeters = dangerLevelMeters;
    }

    // ---- Static factory: domain object -> JPA entity ----

    /**
     * Converts a plain domain RiverStation object into a JPA-managed entity.
     * This is the "anti-corruption layer" that keeps domain models free of JPA annotations.
     */
    public static RiverStationEntity fromDomain(RiverStation station) {
        return new RiverStationEntity(
            station.getStationId(),
            station.getStationName(),
            station.getRiverName(),
            station.getNormalLevelMeters(),
            station.getDangerLevelMeters()
        );
    }

    /**
     * Converts this JPA entity back to the plain domain RiverStation object.
     */
    public RiverStation toDomain() {
        return new RiverStation(stationId, stationName, riverName, normalLevelMeters, dangerLevelMeters);
    }

    // ---- Getters & Setters (required by Hibernate's property-access strategy) ----

    public String getStationId()                  { return stationId; }
    public void   setStationId(String v)          { this.stationId = v; }

    public String getStationName()                { return stationName; }
    public void   setStationName(String v)        { this.stationName = v; }

    public String getRiverName()                  { return riverName; }
    public void   setRiverName(String v)          { this.riverName = v; }

    public double getNormalLevelMeters()          { return normalLevelMeters; }
    public void   setNormalLevelMeters(double v)  { this.normalLevelMeters = v; }

    public double getDangerLevelMeters()          { return dangerLevelMeters; }
    public void   setDangerLevelMeters(double v)  { this.dangerLevelMeters = v; }

    @Override
    public String toString() {
        return "RiverStationEntity[id=" + stationId + ", name=" + stationName + ", river=" + riverName + "]";
    }
}
