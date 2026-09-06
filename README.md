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
│   ├── imageprocessing/
│   ├── exception/
│   └── dao/
├── images/
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

---

## 💻 How to Compile and Run:
```bash
# Compile all source files into bin/
javac -d bin src/model/*.java src/service/*.java src/main/Main.java

# Run the application
java -cp bin main.Main
```
