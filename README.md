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
│   └── dao/
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

---

## 💻 How to Compile and Run:
```bash
# Compile all source files into bin/
javac -d bin src/model/*.java src/exception/*.java src/imageprocessing/*.java src/service/*.java src/main/Main.java

# Run the application
java -cp bin main.Main
```
