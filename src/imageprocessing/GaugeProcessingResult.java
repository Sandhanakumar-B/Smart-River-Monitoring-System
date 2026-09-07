package imageprocessing;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 6: Image Processing & Water Level Estimation Module
 * Syllabus Unit: UNIT V - Java Advanced Imaging, Data Processing & Algorithmic Integration
 *
 * Encapsulates the output of the image processing waterline detection and calibration pipeline.
 */
public class GaugeProcessingResult {

    private final boolean success;
    private final double estimatedWaterLevelMeters;
    private final double confidencePercent;
    private final int detectedWaterLineY;
    private final int imageHeight;
    private final String imagePath;
    private final String algorithmSummary;

    public GaugeProcessingResult(
            boolean success,
            double estimatedWaterLevelMeters,
            double confidencePercent,
            int detectedWaterLineY,
            int imageHeight,
            String imagePath,
            String algorithmSummary) {
        this.success = success;
        this.estimatedWaterLevelMeters = estimatedWaterLevelMeters;
        this.confidencePercent = confidencePercent;
        this.detectedWaterLineY = detectedWaterLineY;
        this.imageHeight = imageHeight;
        this.imagePath = imagePath;
        this.algorithmSummary = algorithmSummary;
    }

    public static GaugeProcessingResult failure(String imagePath, String errorMessage) {
        return new GaugeProcessingResult(false, 0.0, 0.0, -1, -1, imagePath, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public double getEstimatedWaterLevelMeters() {
        return estimatedWaterLevelMeters;
    }

    public double getConfidencePercent() {
        return confidencePercent;
    }

    public int getDetectedWaterLineY() {
        return detectedWaterLineY;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getAlgorithmSummary() {
        return algorithmSummary;
    }

    public void printSummary() {
        System.out.println("---------------- GAUGE IMAGE PROCESSING REPORT ----------------");
        System.out.println("Source Image          : " + imagePath);
        if (!success) {
            System.out.println("Processing Status     : FAILED");
            System.out.println("Error Detail          : " + algorithmSummary);
        } else {
            System.out.println("Processing Status     : SUCCESS");
            System.out.printf("Estimated Water Level : %.2f meters\n", estimatedWaterLevelMeters);
            System.out.printf("Detection Confidence  : %.1f%%\n", confidencePercent);
            System.out.println("Waterline Pixel Y     : " + detectedWaterLineY + " / " + imageHeight + " px");
            System.out.println("Analysis Pipeline     : " + algorithmSummary);
        }
        System.out.println("----------------------------------------------------------------");
    }
}
