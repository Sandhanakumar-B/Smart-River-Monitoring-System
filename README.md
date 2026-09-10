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
│   │   └── WaterLevelRecordFileDAO.java
│   └── simulation/
│       ├── RiverSimulationManager.java
│       ├── SensorEvent.java
│       ├── SensorEventListener.java
│       └── StationSensorSimulator.java
├── data/
│   ├── readings.csv
│   └── stations.csv
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

---

## 💻 How to Compile and Run:
```bash
# Compile all source files into bin/
javac -d bin src/model/*.java src/exception/*.java src/imageprocessing/*.java src/dao/*.java src/service/*.java src/simulation/*.java src/main/Main.java

# Run the application
java -cp bin main.Main
```

