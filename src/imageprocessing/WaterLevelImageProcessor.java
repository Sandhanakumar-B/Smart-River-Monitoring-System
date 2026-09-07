package imageprocessing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 6: Image Processing & Water Level Estimation Module
 * Syllabus Unit: UNIT V - Java Advanced Imaging, Data Processing & Algorithmic Integration
 *
 * Implements computer vision algorithms for river gauge waterline segmentation:
 * 1. Grayscale luminance transformation
 * 2. Gauge Region of Interest (ROI) extraction
 * 3. Vertical edge & intensity gradient profiling
 * 4. Boundary waterline localization
 * 5. Pixel-to-meter physical calibration
 */
public class WaterLevelImageProcessor {

    public static final double DEFAULT_GAUGE_MAX_HEIGHT_METERS = 20.0;
    public static final double DEFAULT_GAUGE_MIN_HEIGHT_METERS = 0.0;

    /**
     * Processes an image file from disk and calculates estimated water level in meters.
     */
    public GaugeProcessingResult processGaugeImage(String imagePath, double maxGaugeHeightMeters) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return GaugeProcessingResult.failure(imagePath, "Invalid image path provided (null or empty).");
        }

        File imageFile = new File(imagePath.trim());
        if (!imageFile.exists() || !imageFile.isFile()) {
            return GaugeProcessingResult.failure(imagePath, "Image file not found at: " + imageFile.getAbsolutePath());
        }

        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
            if (image == null) {
                return GaugeProcessingResult.failure(imagePath, "Unsupported or corrupted image format.");
            }
        } catch (IOException e) {
            return GaugeProcessingResult.failure(imagePath, "I/O error while reading image: " + e.getMessage());
        }

        return analyzeBufferedImage(image, imagePath, maxGaugeHeightMeters);
    }

    /**
     * Analyzes a BufferedImage to detect the waterline boundary and compute water level.
     */
    public GaugeProcessingResult analyzeBufferedImage(BufferedImage image, String imagePath, double maxGaugeHeightMeters) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width < 50 || height < 100) {
            return GaugeProcessingResult.failure(imagePath, "Image dimensions too small for reliable gauge reading (" + width + "x" + height + "px).");
        }

        // Multi-column sampling:
        // We sample columns across the river surface (left flank, right flank, and gauge borders)
        // Flanking columns are immune to tick-mark gradients and clearly exhibit the air-to-water transition.
        int[] sampleColumns = {
            (int) (width * 0.15),
            (int) (width * 0.25),
            (int) (width * 0.75),
            (int) (width * 0.85)
        };

        double[] rowIntensities = new double[height];
        for (int y = 0; y < height; y++) {
            double sum = 0.0;
            for (int colX : sampleColumns) {
                int rgb = image.getRGB(colX, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Standard luminance
                sum += (0.299 * r) + (0.587 * g) + (0.114 * b);
            }
            rowIntensities[y] = sum / sampleColumns.length;
        }

        // Vertical gradient convolution with [-1, 0, 1] kernel over a 2-pixel stride
        int margin = Math.max(5, (int) (height * 0.04));
        double maxGradient = 0.0;
        int candidateWaterlineY = -1;

        for (int y = margin + 2; y < height - margin - 2; y++) {
            double grad = Math.abs(rowIntensities[y - 2] - rowIntensities[y + 2]);
            if (grad > maxGradient) {
                maxGradient = grad;
                candidateWaterlineY = y;
            }
        }

        if (candidateWaterlineY == -1 || maxGradient < 5.0) {
            // Fallback threshold segmentation
            candidateWaterlineY = estimateWaterlineByOtsuThreshold(rowIntensities, margin, height - margin);
        }

        // Physical calibration: pixel coordinates to physical meters
        // In image space: y=0 is gauge top (maxGaugeHeightMeters), y=(height-1) is riverbed (0.0m)
        double fractionSubmerged = (double) (height - 1 - candidateWaterlineY) / (double) (height - 1);
        if (fractionSubmerged < 0.0) fractionSubmerged = 0.0;
        if (fractionSubmerged > 1.0) fractionSubmerged = 1.0;

        double estimatedWaterLevel = fractionSubmerged * maxGaugeHeightMeters;

        // Confidence evaluation based on gradient sharpness
        double confidence = Math.min(99.5, Math.max(75.0, 70.0 + (maxGradient * 0.25)));

        String summary = String.format(
            "Multi-Column Surface Scan | Gradient Peak=%.1f at Y=%d px | Submerged=%.1f%% of %.1fm gauge",
            maxGradient, candidateWaterlineY, (fractionSubmerged * 100.0), maxGaugeHeightMeters
        );

        return new GaugeProcessingResult(
            true,
            Math.round(estimatedWaterLevel * 100.0) / 100.0,
            Math.round(confidence * 10.0) / 10.0,
            candidateWaterlineY,
            height,
            imagePath,
            summary
        );
    }

    /**
     * Fallback thresholding: finds boundary row between higher air intensity and lower water intensity.
     */
    private int estimateWaterlineByOtsuThreshold(double[] intensities, int startY, int endY) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int y = startY; y < endY; y++) {
            if (intensities[y] < min) min = intensities[y];
            if (intensities[y] > max) max = intensities[y];
        }

        double threshold = (min + max) / 2.0;
        // Search from bottom up for the first transition crossing
        for (int y = endY - 1; y >= startY; y--) {
            if (intensities[y] > threshold) {
                return y;
            }
        }
        return (startY + endY) / 2;
    }

    /**
     * Generates a realistic synthetic calibrated gauge benchmark image with visual markers and water level.
     */
    public static void generateBenchmarkGaugeImage(
            String outputPath,
            double waterLevelMeters,
            double maxGaugeHeightMeters,
            int width,
            int height) throws IOException {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Background (Sky and river bank concrete pillar)
        g2d.setColor(new Color(185, 205, 220)); // Soft overcast sky
        g2d.fillRect(0, 0, width, height);

        // Pier background
        g2d.setColor(new Color(140, 142, 145));
        g2d.fillRect((int) (width * 0.20), 0, (int) (width * 0.60), height);

        // 2. Central River Staff Gauge (Yellow with black markings)
        int gaugeX = (int) (width * 0.35);
        int gaugeW = (int) (width * 0.30);
        g2d.setColor(new Color(245, 220, 60)); // Standard surveyor yellow
        g2d.fillRect(gaugeX, 0, gaugeW, height);

        // Border of staff gauge
        g2d.setColor(Color.BLACK);
        g2d.drawRect(gaugeX, 0, gaugeW, height);

        // 3. Draw meter markings & labels on the gauge staff
        g2d.setFont(new Font("Monospaced", Font.BOLD, Math.max(12, height / 35)));
        int meterDivisions = (int) maxGaugeHeightMeters;
        for (int m = 0; m <= meterDivisions; m++) {
            double fraction = (double) m / maxGaugeHeightMeters;
            int markY = (int) ((height - 1) * (1.0 - fraction));

            // Major meter line
            g2d.setColor(Color.BLACK);
            g2d.fillRect(gaugeX, markY - 2, gaugeW, 4);

            // Sub-divisions (0.5m tick)
            if (m < meterDivisions) {
                int halfMarkY = (int) ((height - 1) * (1.0 - (fraction + (0.5 / maxGaugeHeightMeters))));
                g2d.fillRect(gaugeX + (gaugeW / 2), halfMarkY - 1, gaugeW / 2, 2);
            }

            // Meter text
            String label = m + "m";
            g2d.drawString(label, gaugeX + 6, markY - 5);
        }

        // 4. Draw River Water Surface
        double fractionSubmerged = waterLevelMeters / maxGaugeHeightMeters;
        if (fractionSubmerged < 0.0) fractionSubmerged = 0.0;
        if (fractionSubmerged > 1.0) fractionSubmerged = 1.0;

        int waterlineY = (int) ((height - 1) * (1.0 - fractionSubmerged));
        int waterHeight = height - waterlineY;

        // Water body (turbid river water green-blue)
        g2d.setColor(new Color(35, 95, 125, 230));
        g2d.fillRect(0, waterlineY, width, waterHeight);

        // Waterline meniscus / foam line for clear edge detection
        g2d.setColor(new Color(210, 235, 245));
        g2d.fillRect(0, waterlineY - 2, width, 5);

        // Water ripples
        g2d.setColor(new Color(60, 130, 160));
        for (int ry = waterlineY + 15; ry < height; ry += 20) {
            g2d.drawLine(0, ry, width, ry);
        }

        g2d.dispose();

        File outFile = new File(outputPath);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        ImageIO.write(image, "png", outFile);
    }
}
