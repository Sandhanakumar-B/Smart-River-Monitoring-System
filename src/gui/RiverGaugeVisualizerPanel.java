package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import javax.swing.JPanel;
import model.RiverStation;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 11: Java Swing Graphical User Interface & Real-Time Monitoring Dashboard
 * Syllabus Unit: UNIT V - Event-Driven Programming, Graphics2D Custom Rendering, AWT/Swing Painting
 * 
 * Custom JPanel providing 2D graphics rendering of an interactive river gauge staff,
 * dynamic waterline with wave oscillation, color-coded danger/warning threshold lines,
 * and high-visibility digital telemetry readout.
 */
public class RiverGaugeVisualizerPanel extends JPanel {

    private RiverStation station;
    private double currentWaterLevel;
    private double warningThreshold;
    private double dangerThreshold;
    private double maxScaleMeters;
    private double wavePhase;

    // Theme Colors
    private static final Color BG_DARK = new Color(15, 23, 42);          // Deep Slate
    private static final Color BANK_COLOR = new Color(51, 65, 85);        // Slate 700
    private static final Color STAFF_BODY = new Color(241, 245, 249);     // Gauge Staff White
    private static final Color STAFF_BORDER = new Color(30, 41, 59);      // Dark Border
    private static final Color WATER_SURFACE = new Color(56, 189, 248, 220); // Cyan Sky
    private static final Color WATER_DEEP = new Color(2, 132, 199, 240);    // River Blue
    private static final Color COLOR_NORMAL = new Color(34, 197, 94);     // Green
    private static final Color COLOR_WARNING = new Color(245, 158, 11);   // Amber
    private static final Color COLOR_DANGER = new Color(239, 68, 68);     // Crimson
    private static final Color BADGE_BG = new Color(30, 41, 59, 230);

    public RiverGaugeVisualizerPanel() {
        this(null, 7.5, 12.0, 16.5, 25.0);
    }

    public RiverGaugeVisualizerPanel(RiverStation station, double initialLevel, double warning, double danger, double maxScale) {
        this.station = station;
        this.currentWaterLevel = initialLevel;
        this.warningThreshold = warning;
        this.dangerThreshold = danger;
        this.maxScaleMeters = maxScale;
        this.wavePhase = 0.0;
        setPreferredSize(new Dimension(360, 460));
        setBackground(BG_DARK);
    }

    public synchronized void updateTelemetry(RiverStation station, double level) {
        this.station = station;
        this.currentWaterLevel = level;
        if (station != null) {
            this.dangerThreshold = station.getDangerLevelMeters();
            this.warningThreshold = station.getNormalLevelMeters() * 1.5;
            this.maxScaleMeters = Math.max(25.0, station.getDangerLevelMeters() * 1.35);
        }
        this.wavePhase = (this.wavePhase + 0.3) % (2 * Math.PI);
        repaint();
    }

    public synchronized void setCurrentWaterLevel(double level) {
        this.currentWaterLevel = Math.max(0.0, level);
        repaint();
    }

    public synchronized double getCurrentWaterLevel() {
        return currentWaterLevel;
    }

    public synchronized RiverStation getStation() {
        return station;
    }

    public synchronized void setStation(RiverStation station) {
        this.station = station;
        if (station != null) {
            this.dangerThreshold = station.getDangerLevelMeters();
            this.warningThreshold = station.getNormalLevelMeters() * 1.5;
            this.maxScaleMeters = Math.max(25.0, station.getDangerLevelMeters() * 1.35);
        }
        repaint();
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public double getDangerThreshold() {
        return dangerThreshold;
    }

    public double getMaxScaleMeters() {
        return maxScaleMeters;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background sky / gradient
        GradientPaint bgGrad = new GradientPaint(0, 0, BG_DARK, 0, height, new Color(11, 15, 26));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, width, height);

        int topMargin = 50;
        int bottomMargin = 45;
        int plotHeight = height - topMargin - bottomMargin;
        int plotTop = topMargin;
        int plotBottom = height - bottomMargin;

        // River Bed & Banks
        g2.setColor(BANK_COLOR);
        g2.fillRect(0, plotBottom, width, bottomMargin);

        // Staff gauge geometry
        int staffWidth = 46;
        int staffLeft = 45;
        int staffRight = staffLeft + staffWidth;

        // River channel bounds
        int riverLeft = staffRight + 10;
        int riverWidth = width - riverLeft - 20;

        // Map meters to Y coordinate
        int currentY = meterToY(currentWaterLevel, plotTop, plotHeight);
        int dangerY = meterToY(dangerThreshold, plotTop, plotHeight);
        int warningY = meterToY(warningThreshold, plotTop, plotHeight);

        // Draw River Water Body with realistic animated waves
        if (currentY < plotBottom) {
            GeneralPath wavePath = new GeneralPath();
            wavePath.moveTo(riverLeft, plotBottom);
            wavePath.lineTo(riverLeft, currentY);

            int segments = Math.max(10, riverWidth / 8);
            for (int i = 0; i <= segments; i++) {
                double x = riverLeft + (i * (double) riverWidth / segments);
                double wave = Math.sin((i * 0.4) + wavePhase) * 3.5;
                wavePath.lineTo(x, currentY + wave);
            }
            wavePath.lineTo(riverLeft + riverWidth, plotBottom);
            wavePath.closePath();

            GradientPaint waterGrad = new GradientPaint(
                0, currentY, WATER_SURFACE,
                0, plotBottom, WATER_DEEP
            );
            g2.setPaint(waterGrad);
            g2.fill(wavePath);

            // Water surface highlight line
            g2.setColor(new Color(224, 242, 254, 180));
            g2.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < segments; i++) {
                double x1 = riverLeft + (i * (double) riverWidth / segments);
                double y1 = currentY + Math.sin((i * 0.4) + wavePhase) * 3.5;
                double x2 = riverLeft + ((i + 1) * (double) riverWidth / segments);
                double y2 = currentY + Math.sin(((i + 1) * 0.4) + wavePhase) * 3.5;
                g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
        }

        // Draw Staff Gauge Body
        g2.setColor(STAFF_BODY);
        g2.fillRect(staffLeft, plotTop, staffWidth, plotHeight);
        g2.setColor(STAFF_BORDER);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRect(staffLeft, plotTop, staffWidth, plotHeight);

        // Draw Gauge Staff Graduations (Tick Marks every meter, major every 5m)
        Font tickFont = new Font("SansSerif", Font.PLAIN, 10);
        g2.setFont(tickFont);
        FontMetrics fm = g2.getFontMetrics();

        int maxTick = (int) Math.floor(maxScaleMeters);
        for (int m = 0; m <= maxTick; m++) {
            int y = meterToY(m, plotTop, plotHeight);
            if (y < plotTop || y > plotBottom) continue;

            boolean isMajor = (m % 5 == 0);
            int tickLength = isMajor ? 14 : 7;

            g2.setColor(isMajor ? Color.BLACK : new Color(100, 116, 139));
            g2.setStroke(new BasicStroke(isMajor ? 1.5f : 1.0f));
            g2.drawLine(staffRight - tickLength, y, staffRight, y);

            if (isMajor) {
                String label = String.valueOf(m);
                int labelWidth = fm.stringWidth(label);
                g2.drawString(label, staffLeft + 4, y + (fm.getAscent() / 2) - 1);
            }
        }

        // Threshold Lines across river channel
        drawThresholdLine(g2, dangerY, riverLeft, riverWidth, "DANGER: " + String.format("%.1fm", dangerThreshold), COLOR_DANGER);
        drawThresholdLine(g2, warningY, riverLeft, riverWidth, "WARNING: " + String.format("%.1fm", warningThreshold), COLOR_WARNING);

        // Current Water Level Line & Digital Flag
        g2.setColor(getStatusColor());
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine(staffLeft - 6, currentY, riverLeft + riverWidth, currentY);

        // Header Title / Station Banner
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String title = (station != null) ? station.getStationName() : "Basin River Gauge";
        g2.drawString(title, 14, 22);

        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String subtitle = (station != null) ? (station.getRiverName() + " | ID: " + station.getStationId()) : "Real-Time Gauge Visualization";
        g2.drawString(subtitle, 14, 38);

        // Telemetry Digital Readout Badge
        drawTelemetryBadge(g2, width, height);

        g2.dispose();
    }

    private void drawThresholdLine(Graphics2D g2, int y, int x, int width, String label, Color color) {
        if (y < 45 || y > getHeight() - 40) return;

        float[] dash = {6.0f, 4.0f};
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2.drawLine(x, y, x + width, y);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(label, x + 6, y - 4);
    }

    private void drawTelemetryBadge(Graphics2D g2, int width, int height) {
        int badgeWidth = 160;
        int badgeHeight = 56;
        int badgeX = width - badgeWidth - 14;
        int badgeY = 10;

        g2.setColor(BADGE_BG);
        g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 10, 10);
        g2.setColor(getStatusColor());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 10, 10);

        // Level text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        String levelStr = String.format("%.2f m", currentWaterLevel);
        g2.drawString(levelStr, badgeX + 12, badgeY + 24);

        // Status pill
        g2.setColor(getStatusColor());
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        String statusStr = getStatusString();
        g2.drawString(statusStr, badgeX + 12, badgeY + 44);
    }

    private Color getStatusColor() {
        if (currentWaterLevel >= dangerThreshold) {
            return COLOR_DANGER;
        } else if (currentWaterLevel >= warningThreshold) {
            return COLOR_WARNING;
        } else {
            return COLOR_NORMAL;
        }
    }

    private String getStatusString() {
        if (currentWaterLevel >= dangerThreshold) {
            return "CRITICAL (Flood Alert)";
        } else if (currentWaterLevel >= warningThreshold) {
            return "WARNING (High Flow)";
        } else {
            return "NORMAL (Safe Level)";
        }
    }

    private int meterToY(double meters, int top, int plotHeight) {
        double clamped = Math.max(0.0, Math.min(maxScaleMeters, meters));
        double fraction = clamped / maxScaleMeters;
        return top + (int) ((1.0 - fraction) * plotHeight);
    }
}
