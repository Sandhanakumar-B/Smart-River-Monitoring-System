package dao.jpa;

import dao.WaterLevelRecordDAO;
import model.WaterLevelRecord;
import model.WaterLevelRecordEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 14: Spring Data JPA & H2 Database Integration
 * Syllabus Unit: UNIT V - DAO Pattern, ORM Adapter, Spring @Component Bean
 *
 * Concrete WaterLevelRecordDAO implementation backed by Spring Data JPA & H2.
 * Demonstrates how the Adapter pattern bridges the existing domain DAO interface
 * to a Spring Data JpaRepository without any JDBC/SQL boilerplate.
 */
@Component
@Primary
public class WaterLevelRecordJpaDAO implements WaterLevelRecordDAO {

    private final WaterLevelRecordJpaRepository jpaRepository;

    @Autowired
    public WaterLevelRecordJpaDAO(WaterLevelRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<WaterLevelRecord> getAllRecords() {
        // findAll() -> SELECT * FROM jpa_readings
        return jpaRepository.findAll()
                .stream()
                .map(WaterLevelRecordEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void saveRecord(WaterLevelRecord record) {
        // save() -> INSERT or UPDATE based on existing primary key
        jpaRepository.save(WaterLevelRecordEntity.fromDomain(record));
    }

    @Override
    public void saveAllRecords(List<WaterLevelRecord> records) {
        List<WaterLevelRecordEntity> entities = records.stream()
                .map(WaterLevelRecordEntity::fromDomain)
                .collect(Collectors.toList());
        // saveAll() -> batch INSERT/UPDATE
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<WaterLevelRecord> getRecordsByRiver(String riverName) {
        // Derived query: findByRiverName() -> SELECT * WHERE river_name = ?
        return jpaRepository.findByRiverName(riverName)
                .stream()
                .map(WaterLevelRecordEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Returns alert-level records using derived query on alertStatus prefix.
     * Demonstrates the power of Spring Data JPA method naming.
     */
    public List<WaterLevelRecord> getCriticalAndWarningRecords() {
        List<WaterLevelRecord> critical = jpaRepository.findByAlertStatusStartingWith("CRITICAL")
                .stream().map(WaterLevelRecordEntity::toDomain).collect(Collectors.toList());
        List<WaterLevelRecord> warning  = jpaRepository.findByAlertStatusStartingWith("WARNING")
                .stream().map(WaterLevelRecordEntity::toDomain).collect(Collectors.toList());
        critical.addAll(warning);
        return critical;
    }

    /**
     * Fetches records for a specific station using partial location matching.
     */
    public List<WaterLevelRecord> getRecordsByStation(String stationIdentifier) {
        return jpaRepository.findByStationLocationContaining(stationIdentifier)
                .stream()
                .map(WaterLevelRecordEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public String getStorageSource() {
        return "H2 Embedded Database via Spring Data JPA (jpa_readings table)";
    }
}
