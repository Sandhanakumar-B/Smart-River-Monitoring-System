package exception;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 5: Custom Exception Handling (Registration Conflict Validation)
 * Syllabus Unit: UNIT IV - Exception Handling, Custom Exceptions, throw and throws keywords
 */
public class DuplicateStationException extends Exception {

    private final String duplicateStationId;

    public DuplicateStationException(String duplicateStationId) {
        super(String.format("Station registration failed: Station with ID '%s' already exists in the system.", duplicateStationId));
        this.duplicateStationId = duplicateStationId;
    }

    public String getDuplicateStationId() {
        return duplicateStationId;
    }
}
