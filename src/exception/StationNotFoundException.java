package exception;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 5: Custom Exception Handling (Entity Lookup Validation)
 * Syllabus Unit: UNIT IV - Exception Handling, Custom Exceptions, throw and throws keywords
 */
public class StationNotFoundException extends Exception {

    private final String stationId;

    public StationNotFoundException(String stationId) {
        super(String.format("River monitoring station with ID '%s' was not found in the active registry.", stationId));
        this.stationId = stationId;
    }

    public String getStationId() {
        return stationId;
    }
}
