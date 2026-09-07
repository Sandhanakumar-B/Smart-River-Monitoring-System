package imageprocessing;

import java.io.File;
import java.io.IOException;

/**
 * Utility tool to generate sample river staff gauge benchmark images for automated testing.
 */
public class GenerateBenchmarkImages {

    public static void main(String[] args) {
        String baseDir = "images";
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            System.out.println("Generating benchmark river gauge images...");

            // Normal water level: 8.5 meters
            String normalPath = baseDir + File.separator + "gauge_normal.png";
            WaterLevelImageProcessor.generateBenchmarkGaugeImage(normalPath, 8.5, 20.0, 400, 600);
            System.out.println("✅ Generated: " + normalPath + " (Level: 8.5m / Safe)");

            // Warning level: 14.0 meters
            String warningPath = baseDir + File.separator + "gauge_warning.png";
            WaterLevelImageProcessor.generateBenchmarkGaugeImage(warningPath, 14.0, 20.0, 400, 600);
            System.out.println("✅ Generated: " + warningPath + " (Level: 14.0m / Warning)");

            // Flood level: 17.5 meters
            String floodPath = baseDir + File.separator + "gauge_flood.png";
            WaterLevelImageProcessor.generateBenchmarkGaugeImage(floodPath, 17.5, 20.0, 400, 600);
            System.out.println("✅ Generated: " + floodPath + " (Level: 17.5m / Flood Risk)");

            System.out.println("All benchmark images created successfully.");
        } catch (IOException e) {
            System.err.println("Failed to generate benchmark images: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
