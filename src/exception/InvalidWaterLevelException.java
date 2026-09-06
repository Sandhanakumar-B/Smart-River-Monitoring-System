package exception;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 5: Custom Exception Handling (Domain Validation)
 * Syllabus Unit: UNIT IV - Exception Handling, Custom Exceptions, throw and throws keywords
 */
public class InvalidWaterLevelException extends Exception {

    private final double invalidLevel;

    public InvalidWaterLevelException(double invalidLevel) {
        super(String.format("Invalid water level measurement: %.2f meters. Value must be between 0.0m and 50.0m.", invalidLevel));
        this.invalidLevel = invalidLevel;
    }

    public InvalidWaterLevelException(double invalidLevel, String customReason) {
        super(String.format("Invalid water level measurement (%.2fm): %s", invalidLevel, customReason));
        this.invalidLevel = invalidLevel;
    }

    public double getInvalidLevel() {
        return invalidLevel;
    }
}
