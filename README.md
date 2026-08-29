# Smart River Water Level Monitoring and Data Collection System Using Image Processing

An academic console-based Java application designed to estimate river water levels using image processing techniques and collect environmental monitoring data.

---

## 📌 Project Overview
This project monitors river water levels by processing river gauge/surface images, validating measurements against alert thresholds, and recording monitoring history.

---

## 📁 Project Structure
```
SmartRiverWaterLevel/
├── src/
│   ├── main/
│   │   └── Main.java
│   ├── model/
│   │   └── WaterLevelRecord.java
│   ├── service/
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
- Added getters/setters, threshold evaluation logic (`NORMAL`, `WARNING`, `CRITICAL`), and record formatting
- Integrated model instantiation and testing inside `Main.java`

---

## 💻 How to Compile and Run:
```bash
# Compile all source files into bin/
javac -d bin src/model/WaterLevelRecord.java src/main/Main.java

# Run the application
java -cp bin main.Main
```
