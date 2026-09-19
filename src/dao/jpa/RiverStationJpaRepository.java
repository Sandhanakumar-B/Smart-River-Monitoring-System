package dao.jpa;

import model.RiverStationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 14: Spring Data JPA & H2 Database Integration
 * Syllabus Unit: UNIT V - Spring Data JPA, Repository Pattern, Derived Query Methods
 *
 * Spring Data JPA repository for RiverStationEntity.
 * Extending JpaRepository<T, ID> provides all CRUD operations automatically:
 *   - save(entity)        -> INSERT or UPDATE
 *   - findById(id)        -> SELECT WHERE station_id = ?
 *   - findAll()           -> SELECT * FROM jpa_stations
 *   - deleteById(id)      -> DELETE WHERE station_id = ?
 *   - count()             -> SELECT COUNT(*) FROM jpa_stations
 *
 * No SQL, no JDBC boilerplate — Spring Data JPA generates all queries at runtime.
 */
@Repository
public interface RiverStationJpaRepository extends JpaRepository<RiverStationEntity, String> {

    /**
     * Derived query method — Spring Data JPA translates the method name into:
     *   SELECT COUNT(*) > 0 FROM jpa_stations WHERE station_id = ?
     */
    boolean existsByStationId(String stationId);
}
