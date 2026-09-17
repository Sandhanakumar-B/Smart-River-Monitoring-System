# Smart River Water Level Monitoring and Data Collection System Using Image Processing

An academic console-based Java application designed to estimate river water levels using image processing techniques and collect environmental monitoring data.

---

## 📌 Project Overview
This project monitors river water levels by processing river gauge/surface images, validating measurements against alert thresholds, and recording monitoring history across multiple river stations.

---

## 📁 Project Structure
```
SmartRiverWaterLevel/
├── src/
│   ├── main/
│   │   └── Main.java
│   ├── model/
│   │   ├── RiverStation.java
│   │   └── WaterLevelRecord.java
│   ├── service/
│   │   └── RiverMonitoringService.java
│   ├── exception/
│   │   ├── DuplicateStationException.java
│   │   ├── InvalidWaterLevelException.java
│   │   └── StationNotFoundException.java
│   ├── imageprocessing/
│   │   ├── GaugeProcessingResult.java
│   │   ├── GenerateBenchmarkImages.java
│   │   └── WaterLevelImageProcessor.java
│   ├── dao/
│   │   ├── StationDAO.java
│   │   ├── StationFileDAO.java
│   │   ├── WaterLevelRecordDAO.java
│   │   ├── WaterLevelRecordFileDAO.java
│   │   └── jdbc/
│   │       ├── DatabaseConnectionManager.java
│   │       ├── RiverJdbcDriver.java
│   │       ├── StationJdbcDAO.java
│   │       ├── WaterLevelRecordJdbcDAO.java
│   │       └── TestJdbc.java
│   ├── simulation/
│   │   ├── RiverSimulationManager.java
│   │   ├── SensorEvent.java
│   │   ├── SensorEventListener.java
│   │   └── StationSensorSimulator.java
│   ├── network/
│   │   ├── MonitoringProtocol.java
│   │   ├── MonitoringServer.java
│   │   ├── ClientHandler.java
│   │   ├── MonitoringClient.java
│   │   └── TestNetworking.java
│   └── gui/
│       ├── RiverGaugeVisualizerPanel.java
│       ├── RiverMonitoringGUI.java
│       └── TestGUI.java
│   ├── main/
│   │   └── Main.java
├── data/
│   ├── readings.csv
│   ├── stations.csv
│   └── riverdb.mv.db
├── images/
│   ├── gauge_flood.png
│   ├── gauge_normal.png
│   └── gauge_warning.png
├── screenshots/
├── docs/
├── README.md
└── .gitignore
```

---

## 🚀 Daily Development Progress

### Day 1: Project Initialization & Console Scaffold
- Created initial directory structure and `.gitignore`
- Built interactive console menu loop with `Scanner` and `switch-case`
- Added system status and placeholder module routing

### Day 2: Domain Model & OOP Encapsulation
- Created `WaterLevelRecord.java` model class with private fields
- Implemented default and parameterized constructors
- Added getters/setters, threshold evaluation logic, and record formatting
- Integrated model instantiation and testing inside `Main.java`

### Day 3: River Station Entity & Object Relationships
- Created `RiverStation.java` model class representing monitoring stations
- Implemented object relationship (Association: Station generates `WaterLevelRecord`)
- Demonstrated **Method Overriding** with `toString()`
- Added interactive station details and reading generation in `Main.java`

### Day 4: Service Layer & Collections Framework
- Created `RiverMonitoringService.java` in the `service` package to decouple business logic
- Utilized Java Collections Framework (`List`, `ArrayList`) for dynamic station registry and historical reading audit logs
- Added basin-wide analytics: average water level, peak recorded level, and critical flood warning filtration
- Enhanced `Main.java` with multi-station switching, interactive reading entry, and real-time alert dispatch

### Day 5: Custom Exception Handling & Robust Input Validation
- Created custom domain checked exceptions extending `java.lang.Exception`:
  - `InvalidWaterLevelException.java`: Rejects out-of-bounds readings (`< 0.0m` or `> 50.0m`)
  - `StationNotFoundException.java`: Catches lookups for unregistered station IDs
  - `DuplicateStationException.java`: Prevents registration collisions for duplicate station IDs
- Integrated `throw` and `throws` declarations across the service layer
- Implemented structured `try-catch-finally` blocks in `Main.java` ensuring graceful error recovery without console crashes
- Added interactive registration feature for custom river stations

### Day 6: Computer Vision & Water Level Estimation from Gauge Images
- Created `WaterLevelImageProcessor.java` utilizing standard Java 2D Image I/O (`BufferedImage`, `Raster`):
  - Multi-column surface boundary scan across river flanks and gauge staff to filter tick-mark artifacts
  - Grayscale luminance profiling and vertical edge convolution kernel `[-1, 0, 1]`
  - Otsu threshold fallback segmentation for subtle gradients
  - Pixel-to-meter physical calibration mapping detected pixel waterline to physical river water level
- Created `GaugeProcessingResult.java` value object storing detection metrics, confidence scores, pixel coordinates, and diagnostic notes
- Created `GenerateBenchmarkImages.java` producing calibrated test gauge images (`gauge_normal.png`, `gauge_warning.png`, `gauge_flood.png`)
- Integrated image processing pipeline into `RiverMonitoringService` and interactive Menu Option 4 in `Main.java`

### Day 7: Data Access Object (DAO) Pattern & CSV File Persistence
- Designed and implemented **DAO Interface abstraction layer** (`src/dao/`):
  - `StationDAO.java`: Interface defining CRUD persistence contracts for `RiverStation` entities
  - `WaterLevelRecordDAO.java`: Interface defining persistence contracts for `WaterLevelRecord` entities
  - `StationFileDAO.java`: Concrete CSV file-backed implementation (reads/writes `data/stations.csv`)
  - `WaterLevelRecordFileDAO.java`: Concrete CSV file-backed implementation (reads/writes `data/readings.csv`)
- Auto-seeds `data/stations.csv` and `data/readings.csv` with initial data on first launch
- Integrated DAOs into `RiverMonitoringService` via **Dependency Injection constructor** for testability
- All `registerStation()`, `recordMeasurement()`, and `processAndRecordGaugeImage()` now persist to disk via DAO
- Added **Menu Option 8: DAO Storage & Data Persistence Management** — view/reload/flush CSV files from console
- Updated `Main.java` to version **v0.7 (Day 7: DAO & File Persistence)**

### Day 8: Multithreading & Automated Real-Time Sensor / River Simulation
- Implemented concurrent **Multithreading & Telemetry Simulation Architecture** (`src/simulation/`):
  - `StationSensorSimulator.java`: Worker thread implementing `java.lang.Runnable` representing an autonomous IoT river station sensor
  - `RiverSimulationManager.java`: Concurrency orchestrator managing multi-threaded sensor workers across all registered stations
  - `SensorEvent.java`: Immutable telemetry event encapsulating station data, thread identifier, water level, and alert level
  - `SensorEventListener.java`: Observer callback interface delivering asynchronous telemetry updates and flood warning notifications
- **Concurrency & Thread Safety Mechanisms**:
  - `CopyOnWriteArrayList` in `RiverMonitoringService` preventing `ConcurrentModificationException` during concurrent reads/writes
  - Thread-safe DAO operations (`synchronized` persistence to `data/readings.csv`)
  - Graceful thread lifecycle control using `volatile boolean running`, `volatile boolean paused`, and `Thread.interrupt()` / `join()`
- **Hydrological Simulation & Dynamic Scenarios**:
  - Realistic stochastic Markov Random Walk around baseline normal levels with natural ripple
  - Flash flood cloudburst / dam discharge surge injection (+4.5m basin-wide or station-specific) testing concurrent alert handling
- **Worker Thread Diagnostics & Telemetry Dashboard**:
  - Runtime inspection of thread names, thread states (`TIMED_WAITING`, `RUNNABLE`), priority, and alive flags
  - Added **Menu Option 9: ⚡ Real-Time Multithreaded Sensor & River Simulation (IoT Telemetry)**
  - Updated `Main.java` to version **v0.8 (Day 8: Multithreading & Real-Time Simulation)**

### Day 9: Java Networking & Socket Programming — TCP Client-Server Architecture
- Designed and implemented a full **TCP Client-Server Networking Layer** (`src/network/`):
  - `MonitoringProtocol.java`: Shared protocol constants — commands (`LIST_STATIONS`, `GET_LEVEL`, `GET_HISTORY`, `GET_ALERTS`, `BASIN_STATS`, `SERVER_STATUS`, `QUIT`) and response tokens (`OK`, `ERROR`, `END`, `WELCOME`) and pipe-delimited `FIELD_SEP`
  - `MonitoringServer.java`: TCP server using `java.net.ServerSocket` with an `ExecutorService` fixed thread-pool (up to 10 simultaneous clients), `AtomicInteger` client counter, and graceful `stopServer()` shutdown sequence
  - `ClientHandler.java`: `Runnable` per-client handler wrapping `Socket` I/O streams (`BufferedReader` / `PrintWriter`) — dispatches all 6 protocol commands to `RiverMonitoringService` and manages socket timeout (`setSoTimeout`)
  - `MonitoringClient.java`: TCP client (`java.net.Socket`) with an interactive REPL session and a headless `sendCommand()` API for programmatic / automated queries
  - `TestNetworking.java`: End-to-end verification — starts server, runs 6 command queries, verifies all `OK` responses, tests unknown command `ERROR` handling, and validates 3 concurrent multi-client connections simultaneously
- **Key Java Networking Concepts Demonstrated (Syllabus UNIT V)**:
  - `ServerSocket.accept()` blocking loop on a dedicated daemon thread
  - `Socket` bidirectional I/O via `InputStream` / `OutputStream` wrapped as text streams
  - Thread-pool (`ExecutorService`) for concurrent multi-client handling without raw `Thread` creation
  - `SocketTimeoutException` handling for idle client disconnection (`30 s` server / `15 s` client timeout)
  - `try-with-resources` for automatic stream and socket closure
  - `volatile boolean running` + `AtomicInteger` for thread-safe lifecycle management
- **Interactive Console Integration**:
  - Added **Menu Option 10: 🌐 TCP Networking & Remote Monitoring Server (Socket Programming)** with sub-options: start/stop server, connect as remote client, run automated demo, and view protocol reference
  - Updated `Main.java` to version **v0.9 (Day 9: Java Networking & Socket Programming)**
- **Verification**: All 9 networking test assertions passed — SERVER_STATUS, LIST_STATIONS, BASIN_STATS, GET_ALERTS, GET_LEVEL, unknown-command ERROR, and 3-concurrent-client multi-connection test

### Day 10: JDBC Database Connectivity & Relational Data Layer
- Implemented complete **JDBC Architecture & Relational Persistence Layer** (`src/dao/jdbc/`):
  - `DatabaseConnectionManager.java`: Singleton managing connection pooling, schema initialization (DDL `CREATE TABLE IF NOT EXISTS`), diagnostic metadata extraction, and arbitrary SQL query execution with formatted tabular printing
  - `RiverJdbcDriver.java`: Pure-Java relational engine and mock JDBC driver (`java.sql.Driver`, `Connection`, `Statement`, `PreparedStatement`, `ResultSet`, `DatabaseMetaData`, `ResultSetMetaData`) ensuring zero external JAR dependencies
  - `StationJdbcDAO.java`: Relational DAO implementing `StationDAO` using parameterized `PreparedStatement` queries (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) and `ResultSet` mapping
  - `WaterLevelRecordJdbcDAO.java`: Relational DAO implementing `WaterLevelRecordDAO` with indexed station lookups, date sorting, and alert filtration
  - `TestJdbc.java`: Comprehensive automated test suite verifying driver registration, connection acquisition, DDL execution, prepared statement inserts, and result set traversal
- **Key Java Database Concepts Demonstrated (Syllabus UNIT V)**:
  - `DriverManager.getConnection()` and JDBC URL connection lifecycle
  - `Statement` vs. `PreparedStatement` parameterized queries preventing SQL injection
  - `ResultSet` scrolling, row cursor navigation, and column metadata extraction
  - Relational schema modeling (`stations` table and `readings` table with foreign key relations)
  - Seamless DAO strategy switching between CSV File DAO and Relational JDBC DAO via `switchStorageEngine()`
- **Interactive Console Integration**:
  - Added **Menu Option 11: 🗄️ JDBC Database Connectivity & Relational SQL Console (UNIT V)** with diagnostics, schema DDL, row inspection, interactive SQL console, data migration, and storage engine switcher
  - Updated `Main.java` to version **v0.10 (Day 10: JDBC Database Connectivity & Relational Persistence)**

### Day 11: Java Swing Graphical User Interface & Real-Time Monitoring Dashboard
- Designed and built a desktop application using standard Java Desktop APIs (`javax.swing.*`, `java.awt.*`) (`src/gui/`):
  - `RiverGaugeVisualizerPanel.java`: Custom `JPanel` with overridden `paintComponent(Graphics g)` and `Graphics2D` rendering:
    - Metric staff gauge with graduated tick markings (0.0m to 25.0m) and color-coded alert zones
    - Dynamic sinusoidal water surface animation with translucent gradients and physical waterline indicator
    - Real-time numerical level pill (Normal / Warning / Critical) and interactive level manipulation
  - `RiverMonitoringGUI.java`: Multi-tabbed monitoring dashboard (`JFrame`) featuring 5 feature tabs:
    - **Tab 1: Basin Overview & Station Grid** — Summary metric cards, filterable `JTable` with custom status cell renderers, and new station registration dialog
    - **Tab 2: Interactive River Gauge Visualizer** — Embedded `RiverGaugeVisualizerPanel`, station selector dropdown, danger thresholds, and test water level slider
    - **Tab 3: Computer Vision & Gauge Image Analysis** — Gauge image preview, file chooser (`JFileChooser`), and one-click execution of `WaterLevelImageProcessor` with waterline overlay and detection confidence
    - **Tab 4: Real-Time IoT Telemetry & Concurrency Monitor** — Real-time sensor thread controls (Start/Stop), flash flood surge injection (+4.5m), worker thread diagnostic table, and live event log updated thread-safely via `SwingUtilities.invokeLater()`
    - **Tab 5: Relational JDBC SQL Console** — Dynamic storage engine toggle (CSV File vs. Relational JDBC), quick queries, and tabular `ResultSet` display
  - `TestGUI.java`: Headless-safe verification test suite validating environment detection, 2D offscreen `BufferedImage` rendering, GUI data binding, `SensorEventListener` telemetry dispatch, and JDBC querying
- **Key Java Desktop & Event-Driven Concepts Demonstrated (Syllabus UNIT V)**:
  - Swing component hierarchy (`JFrame`, `JTabbedPane`, `JSplitPane`, `JTable`, `JProgressBar`, `JSlider`, `JMenuBar`)
  - Layout managers (`BorderLayout`, `GridLayout`, `FlowLayout`, `BoxLayout`, `EmptyBorder`)
  - Custom 2D graphics rendering (`Graphics2D`, `RenderingHints.KEY_ANTIALIASING`, `GradientPaint`, `FontMetrics`)
  - Multi-threaded Event Dispatch Thread (EDT) safety using `SwingUtilities.invokeLater()`
  - Observer design pattern (`SensorEventListener`) for real-time telemetry updates from background worker threads
- **Interactive Console Integration**:
  - Added **Menu Option 12: 🖥️ Launch Java Swing GUI Dashboard (Event-Driven Desktop Application)** with options to launch the desktop application, run automated GUI tests, or inspect component hierarchy
  - Updated `Main.java` to version **v0.11 (Day 11: Java Swing GUI & Event-Driven Monitoring Dashboard)**

### Day 12: Advanced Data Analytics & Reporting (Java 8 Streams & Lambdas)
- Introduced Java 8 functional programming paradigms (Streams API, Lambdas, Method References) to replace legacy loops.
- Created `RiverAnalyticsService.java` for advanced aggregations:
  - `findHighestRecordedLevel()` using `Optional` and `max()`.
  - `getTopNHighestReadings(int n)` using `sorted()` and `limit()`.
  - `getAverageWaterLevelByStation()` using `Collectors.groupingBy()` and `Collectors.averagingDouble()`.
  - `getStationsWithCriticalAlerts()` using `filter()`, `map()`, and `distinct()`.
- Refactored `RiverMonitoringService.java` to use Stream API for filtering and averaging.
- Added headless testing suite `TestAnalytics.java` to verify functional analytics pipelines.
- Integrated **Menu Option 13** in `Main.java` to print a comprehensive Day 12 Analytics Report.
- Updated `Main.java` to version **v0.12 (Day 12: Advanced Data Analytics & Reporting)**

---

### Day 13: Spring Boot Migration & REST API Development
- Integrated **Spring Boot 3.2.4** (`spring-boot-starter-web`) via `pom.xml` with embedded **Apache Tomcat** on port 8080
- Created `SmartRiverApplication.java` — `@SpringBootApplication` entry point with `@ComponentScan` across all packages
- Annotated `StationFileDAO` and `WaterLevelRecordFileDAO` with `@Repository` and `RiverMonitoringService` with `@Service` for Spring IoC container management
- Implemented `RiverStationController.java` — `@RestController` exposing 3 JSON REST endpoints:
  - `GET /api/stations` — Returns JSON array of all registered monitoring stations
  - `GET /api/readings/{stationId}` — Returns historical water level records filtered by station ID
  - `GET /api/stats` — Returns basin-wide analytics: total stations, readings, avg level, peak level, critical alerts count
- Preserved backward compatibility: restored default no-arg `RiverMonitoringService()` constructor so all legacy `Main.java`, `TestGUI.java`, `TestNetworking.java`, `TestAnalytics.java` continue to compile
- **Verification**: All 3 REST endpoints tested and returning valid JSON responses from live data
- Run API server: `mvn spring-boot:run` → API live at `http://localhost:8080/api/`

---

## 💻 How to Compile and Run:
```bash
# Compile all source packages into bin/
javac -d bin src/model/*.java src/exception/*.java src/imageprocessing/*.java src/dao/*.java src/dao/jdbc/*.java src/service/*.java src/simulation/*.java src/network/*.java src/gui/*.java src/main/Main.java

# Run interactive console application
java -cp bin main.Main

# Run Desktop Swing GUI application directly
java -cp bin gui.RiverMonitoringGUI

# Run automated verification test suites
java -cp bin gui.TestGUI
java -cp bin dao.jdbc.TestJdbc
java -cp bin network.TestNetworking
```

