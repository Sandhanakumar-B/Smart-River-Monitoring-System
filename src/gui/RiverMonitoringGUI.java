package gui;

import dao.jdbc.DatabaseConnectionManager;
import dao.jdbc.StationJdbcDAO;
import dao.jdbc.WaterLevelRecordJdbcDAO;
import exception.InvalidWaterLevelException;
import exception.StationNotFoundException;
import imageprocessing.GaugeProcessingResult;
import imageprocessing.WaterLevelImageProcessor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;
import simulation.RiverSimulationManager;
import simulation.SensorEvent;
import simulation.SensorEventListener;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 11: Java Swing Graphical User Interface & Real-Time Monitoring Dashboard
 * Syllabus Unit: UNIT V - Event-Driven Programming, Java Swing Components, Layout Managers,
 *                         Event Listeners, Table Models, Custom Dialogs, Real-Time Telemetry Binding
 * 
 * Complete Desktop GUI Application integrating all subsystem layers:
 * - Domain Entity management & Basin metrics
 * - Interactive 2D River Gauge visualizer
 * - Computer Vision gauge image analysis
 * - Multithreaded IoT Telemetry event streaming
 * - Relational JDBC SQL interactive console
 */
public class RiverMonitoringGUI extends JFrame implements SensorEventListener {

    private final RiverMonitoringService monitoringService;
    private final RiverSimulationManager simulationManager;
    private final WaterLevelImageProcessor imageProcessor;

    // GUI Components
    private JTabbedPane mainTabbedPane;
    private JTable stationsTable;
    private DefaultTableModel stationsTableModel;
    private JLabel lblTotalStations;
    private JLabel lblTotalReadings;
    private JLabel lblCriticalAlerts;
    private JLabel lblAvgLevel;
    private JLabel lblStorageEngine;

    // Visualizer tab components
    private RiverGaugeVisualizerPanel gaugePanel;
    private JComboBox<String> stationComboBox;
    private JSlider levelSlider;
    private JLabel lblSliderValue;

    // Image processing components
    private JLabel lblImagePreview;
    private JComboBox<String> cmbPresetImages;
    private JTextArea txtImageResults;
    private File selectedImageFile;

    // Telemetry components
    private JTextArea txtTelemetryLog;
    private JTable threadsTable;
    private DefaultTableModel threadsTableModel;
    private JButton btnStartSimulation;
    private JButton btnStopSimulation;
    private JButton btnSurgeFlood;

    // SQL Console components
    private JTextField txtSqlQuery;
    private JTable sqlResultsTable;
    private DefaultTableModel sqlResultsTableModel;
    private JLabel lblSqlStatus;

    // Styling Palette (Modern Dark / Slate)
    public static final Color COLOR_BG_DARK = new Color(15, 23, 42);
    public static final Color COLOR_SURFACE = new Color(30, 41, 59);
    public static final Color COLOR_CARD = new Color(51, 65, 85);
    public static final Color COLOR_ACCENT = new Color(14, 165, 233);
    public static final Color COLOR_ACCENT_HOVER = new Color(2, 132, 199);
    public static final Color COLOR_TEXT_LIGHT = new Color(248, 250, 252);
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);
    public static final Color COLOR_SUCCESS = new Color(34, 197, 94);
    public static final Color COLOR_WARNING = new Color(245, 158, 11);
    public static final Color COLOR_DANGER = new Color(239, 68, 68);

    public RiverMonitoringGUI(RiverMonitoringService service, RiverSimulationManager simManager) {
        this.monitoringService = (service != null) ? service : new RiverMonitoringService();
        this.simulationManager = (simManager != null) ? simManager : new RiverSimulationManager(this.monitoringService);
        this.imageProcessor = new WaterLevelImageProcessor();

        // Register GUI as a listener for real-time sensor events
        this.simulationManager.addGlobalListener(this);

        if (!GraphicsEnvironment.isHeadless()) {
            initUI();
            refreshAllData();
        }
    }

    private void initUI() {
        setTitle("Smart River Water Level Monitoring & Telemetry System (Day 11: Java Swing Dashboard)");
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set global dark background
        getContentPane().setBackground(COLOR_BG_DARK);
        setLayout(new BorderLayout());

        // Build Menu Bar
        setJMenuBar(createMenuBar());

        // Build Main Tabbed Pane
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        mainTabbedPane.setBackground(COLOR_SURFACE);
        mainTabbedPane.setForeground(COLOR_TEXT_LIGHT);

        mainTabbedPane.addTab("📊 Basin Overview", createBasinOverviewTab());
        mainTabbedPane.addTab("🌊 Gauge Visualizer", createGaugeVisualizerTab());
        mainTabbedPane.addTab("📷 Computer Vision", createImageProcessingTab());
        mainTabbedPane.addTab("⚡ IoT Telemetry", createTelemetryTab());
        mainTabbedPane.addTab("🗄️ JDBC SQL Console", createJdbcConsoleTab());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(mainTabbedPane, BorderLayout.CENTER);
        add(createFooterStatusPanel(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_SURFACE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD));

        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(COLOR_TEXT_LIGHT);
        JMenuItem miRefresh = new JMenuItem("Refresh All Data");
        miRefresh.addActionListener(e -> refreshAllData());
        JMenuItem miSwitchJdbc = new JMenuItem("Switch to JDBC Relational Storage");
        miSwitchJdbc.addActionListener(e -> switchToJdbcStorage());
        JMenuItem miSwitchCsv = new JMenuItem("Switch to CSV File Storage");
        miSwitchCsv.addActionListener(e -> switchToCsvStorage());
        JMenuItem miExit = new JMenuItem("Close Dashboard");
        miExit.addActionListener(e -> dispose());
        fileMenu.add(miRefresh);
        fileMenu.addSeparator();
        fileMenu.add(miSwitchJdbc);
        fileMenu.add(miSwitchCsv);
        fileMenu.addSeparator();
        fileMenu.add(miExit);

        JMenu simMenu = new JMenu("Simulation");
        simMenu.setForeground(COLOR_TEXT_LIGHT);
        JMenuItem miStartSim = new JMenuItem("Start IoT Sensor Threads");
        miStartSim.addActionListener(e -> startSimulationWorkers());
        JMenuItem miStopSim = new JMenuItem("Stop IoT Sensor Threads");
        miStopSim.addActionListener(e -> stopSimulationWorkers());
        JMenuItem miSurge = new JMenuItem("Inject Flood Surge (+4.5m)");
        miSurge.addActionListener(e -> injectFloodSurge());
        simMenu.add(miStartSim);
        simMenu.add(miStopSim);
        simMenu.addSeparator();
        simMenu.add(miSurge);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(COLOR_TEXT_LIGHT);
        JMenuItem miAbout = new JMenuItem("About System");
        miAbout.addActionListener(e -> showAboutDialog());
        JMenuItem miSyllabus = new JMenuItem("Syllabus Unit V Reference");
        miSyllabus.addActionListener(e -> showSyllabusDialog());
        helpMenu.add(miAbout);
        helpMenu.add(miSyllabus);

        menuBar.add(fileMenu);
        menuBar.add(simMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_SURFACE);
        panel.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(COLOR_SURFACE);

        JLabel title = new JLabel("SMART RIVER WATER LEVEL MONITORING SYSTEM");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(COLOR_TEXT_LIGHT);

        JLabel subtitle = new JLabel("Syllabus UNIT V: Event-Driven Desktop Dashboard with Custom Graphics2D & Concurrency Binding");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(COLOR_ACCENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(COLOR_SURFACE);

        JButton btnRefresh = createStyledButton("🔄 Refresh", COLOR_CARD);
        btnRefresh.addActionListener(e -> refreshAllData());

        JButton btnAddStation = createStyledButton("+ Register Station", COLOR_ACCENT);
        btnAddStation.addActionListener(e -> openRegisterStationDialog());

        actionPanel.add(btnRefresh);
        actionPanel.add(btnAddStation);

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createFooterStatusPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(COLOR_SURFACE);
        footer.setBorder(new EmptyBorder(6, 16, 6, 16));

        lblStorageEngine = new JLabel("Active Storage: " + monitoringService.getStationDAO().getStorageSource());
        lblStorageEngine.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStorageEngine.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVersion = new JLabel("Core Java v0.11 | Swing Desktop Dashboard Edition");
        lblVersion.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblVersion.setForeground(COLOR_TEXT_MUTED);

        footer.add(lblStorageEngine, BorderLayout.WEST);
        footer.add(lblVersion, BorderLayout.EAST);
        return footer;
    }

    // ==========================================
    // TAB 1: BASIN OVERVIEW & STATIONS GRID
    // ==========================================
    private JPanel createBasinOverviewTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBackground(COLOR_BG_DARK);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Metric Cards Panel
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsPanel.setBackground(COLOR_BG_DARK);

        lblTotalStations = new JLabel("0", SwingConstants.CENTER);
        lblTotalReadings = new JLabel("0", SwingConstants.CENTER);
        lblCriticalAlerts = new JLabel("0", SwingConstants.CENTER);
        lblAvgLevel = new JLabel("0.00 m", SwingConstants.CENTER);

        metricsPanel.add(createMetricCard("REGISTERED STATIONS", lblTotalStations, COLOR_ACCENT));
        metricsPanel.add(createMetricCard("TOTAL READINGS", lblTotalReadings, COLOR_SUCCESS));
        metricsPanel.add(createMetricCard("CRITICAL ALERTS", lblCriticalAlerts, COLOR_DANGER));
        metricsPanel.add(createMetricCard("AVERAGE WATER LEVEL", lblAvgLevel, COLOR_WARNING));

        tab.add(metricsPanel, BorderLayout.NORTH);

        // Stations Table
        String[] columns = {"Station ID", "Station Name", "Monitored River", "Normal Level", "Danger Threshold", "Latest Level", "Current Status"};
        stationsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        stationsTable = new JTable(stationsTableModel);
        styleTable(stationsTable);

        // Custom status cell renderer
        stationsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (value != null) ? value.toString() : "";
                if (status.startsWith("CRITICAL")) {
                    setForeground(COLOR_DANGER);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (status.startsWith("WARNING")) {
                    setForeground(COLOR_WARNING);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(COLOR_SUCCESS);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // Row selection links to gauge visualizer
        stationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && stationsTable.getSelectedRow() >= 0) {
                int selectedRow = stationsTable.getSelectedRow();
                String stnId = (String) stationsTableModel.getValueAt(selectedRow, 0);
                RiverStation stn = monitoringService.getStationById(stnId);
                if (stn != null) {
                    stationComboBox.setSelectedItem(stn.getStationId() + " - " + stn.getStationName());
                    updateGaugeToSelectedStation(stn);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(stationsTable);
        scrollPane.getViewport().setBackground(COLOR_SURFACE);
        scrollPane.setBorder(new LineBorder(COLOR_CARD, 1));

        // Station Actions Toolbar
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actionToolbar.setBackground(COLOR_SURFACE);
        actionToolbar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD));

        JButton btnManualReading = createStyledButton("📝 Record Water Level", COLOR_ACCENT);
        btnManualReading.addActionListener(e -> openManualReadingDialog());

        JButton btnViewAlerts = createStyledButton("⚠️ View Critical Alerts", COLOR_DANGER);
        btnViewAlerts.addActionListener(e -> showCriticalAlertsDialog());

        JButton btnSwitchToGauge = createStyledButton("🔍 Inspect on River Gauge", COLOR_CARD);
        btnSwitchToGauge.addActionListener(e -> {
            if (stationsTable.getSelectedRow() >= 0) {
                mainTabbedPane.setSelectedIndex(1); // Jump to Visualizer tab
            } else {
                JOptionPane.showMessageDialog(this, "Please select a station row from the table first.", "Station Selection Required", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        actionToolbar.add(btnManualReading);
        actionToolbar.add(btnViewAlerts);
        actionToolbar.add(btnSwitchToGauge);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(actionToolbar, BorderLayout.SOUTH);

        tab.add(centerPanel, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(COLOR_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // TAB 2: INTERACTIVE RIVER GAUGE VISUALIZER
    // ==========================================
    private JPanel createGaugeVisualizerTab() {
        JPanel tab = new JPanel(new BorderLayout(14, 14));
        tab.setBackground(COLOR_BG_DARK);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Gauge custom graphics panel (Day 11 Graphics2D Component)
        gaugePanel = new RiverGaugeVisualizerPanel();
        gaugePanel.setBorder(new LineBorder(COLOR_CARD, 1, true));

        // Controls Panel on right
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(COLOR_SURFACE);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        controlPanel.setPreferredSize(new Dimension(380, 500));

        JLabel ctrlTitle = new JLabel("RIVER GAUGE TELEMETRY CONTROLS");
        ctrlTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        ctrlTitle.setForeground(COLOR_TEXT_LIGHT);
        ctrlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ctrlDesc = new JLabel("Select station to view physical water height against warning/danger scale:");
        ctrlDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ctrlDesc.setForeground(COLOR_TEXT_MUTED);
        ctrlDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Station Selector Dropdown
        stationComboBox = new JComboBox<>();
        stationComboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stationComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        stationComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        stationComboBox.addActionListener(e -> {
            String selected = (String) stationComboBox.getSelectedItem();
            if (selected != null) {
                String stnId = selected.split(" - ")[0].trim();
                RiverStation stn = monitoringService.getStationById(stnId);
                if (stn != null) {
                    updateGaugeToSelectedStation(stn);
                }
            }
        });

        // Interactive Level Slider for testing & manual simulation
        JLabel lblSliderTitle = new JLabel("Interactive Water Level Simulation Slider:");
        lblSliderTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblSliderTitle.setForeground(COLOR_TEXT_LIGHT);
        lblSliderTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        levelSlider = new JSlider(0, 300, 75); // 0.0m to 30.0m (scaled x10)
        levelSlider.setBackground(COLOR_SURFACE);
        levelSlider.setForeground(COLOR_TEXT_LIGHT);
        levelSlider.setMajorTickSpacing(50); // every 5.0m
        levelSlider.setMinorTickSpacing(10); // every 1.0m
        levelSlider.setPaintTicks(true);
        levelSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSliderValue = new JLabel("Selected Level: 7.50 meters", SwingConstants.CENTER);
        lblSliderValue.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblSliderValue.setForeground(COLOR_ACCENT);
        lblSliderValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        levelSlider.addChangeListener(e -> {
            double meters = levelSlider.getValue() / 10.0;
            lblSliderValue.setText(String.format("Selected Level: %.2f meters", meters));
            gaugePanel.setCurrentWaterLevel(meters);
        });

        JButton btnCommitMeasurement = createStyledButton("💾 Record Slider Reading to Storage", COLOR_ACCENT);
        btnCommitMeasurement.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCommitMeasurement.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnCommitMeasurement.addActionListener(e -> {
            RiverStation stn = gaugePanel.getStation();
            if (stn != null) {
                double meters = levelSlider.getValue() / 10.0;
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
                try {
                    monitoringService.recordMeasurement(stn.getStationId(), meters, ts);
                    refreshAllData();
                    JOptionPane.showMessageDialog(this, "Water level of " + meters + "m recorded successfully for " + stn.getStationName() + "!", "Measurement Logged", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error logging measurement: " + ex.getMessage(), "Recording Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton btnResetNormal = createStyledButton("↺ Reset to Station Normal Level", COLOR_CARD);
        btnResetNormal.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnResetNormal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnResetNormal.addActionListener(e -> {
            RiverStation stn = gaugePanel.getStation();
            if (stn != null) {
                levelSlider.setValue((int) (stn.getNormalLevelMeters() * 10));
            }
        });

        JButton btnSimulateSurge = createStyledButton("⚡ Simulate Danger Flood Surge (+4.5m)", COLOR_DANGER);
        btnSimulateSurge.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSimulateSurge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnSimulateSurge.addActionListener(e -> {
            RiverStation stn = gaugePanel.getStation();
            if (stn != null) {
                double surge = stn.getDangerLevelMeters() + 2.5;
                levelSlider.setValue((int) (surge * 10));
            }
        });

        controlPanel.add(ctrlTitle);
        controlPanel.add(Box.createVerticalStrut(4));
        controlPanel.add(ctrlDesc);
        controlPanel.add(Box.createVerticalStrut(12));
        controlPanel.add(stationComboBox);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(lblSliderTitle);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(levelSlider);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(lblSliderValue);
        controlPanel.add(Box.createVerticalStrut(18));
        controlPanel.add(btnCommitMeasurement);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(btnResetNormal);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(btnSimulateSurge);
        controlPanel.add(Box.createVerticalGlue());

        tab.add(gaugePanel, BorderLayout.CENTER);
        tab.add(controlPanel, BorderLayout.EAST);
        return tab;
    }

    private void updateGaugeToSelectedStation(RiverStation stn) {
        if (stn == null) return;
        double currentLevel = stn.getNormalLevelMeters();
        List<WaterLevelRecord> records = monitoringService.getAllRecords();
        for (int i = records.size() - 1; i >= 0; i--) {
            WaterLevelRecord r = records.get(i);
            if (r.getStationLocation().contains(stn.getStationId())) {
                currentLevel = r.getWaterLevelMeters();
                break;
            }
        }
        gaugePanel.updateTelemetry(stn, currentLevel);
        if (levelSlider != null) {
            levelSlider.setValue((int) (currentLevel * 10));
        }
    }

    // ==========================================
    // TAB 3: COMPUTER VISION & IMAGE PROCESSING
    // ==========================================
    private JPanel createImageProcessingTab() {
        JPanel tab = new JPanel(new BorderLayout(14, 14));
        tab.setBackground(COLOR_BG_DARK);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Left Panel: Image Preview
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(COLOR_SURFACE);
        previewPanel.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(COLOR_CARD, 1), "River Gauge Image Preview",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif", Font.BOLD, 12), COLOR_TEXT_LIGHT
        ));

        lblImagePreview = new JLabel("No image loaded", SwingConstants.CENTER);
        lblImagePreview.setForeground(COLOR_TEXT_MUTED);
        lblImagePreview.setPreferredSize(new Dimension(450, 400));
        previewPanel.add(new JScrollPane(lblImagePreview), BorderLayout.CENTER);

        // Right Panel: Controls and Results
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(COLOR_SURFACE);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        controlPanel.setPreferredSize(new Dimension(460, 500));

        JLabel cvTitle = new JLabel("COMPUTER VISION ESTIMATION PIPELINE");
        cvTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        cvTitle.setForeground(COLOR_TEXT_LIGHT);
        cvTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cvDesc = new JLabel("Select benchmark gauge image or load custom river photo:");
        cvDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cvDesc.setForeground(COLOR_TEXT_MUTED);
        cvDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Preset combo box
        String[] presetImages = {
            "images/gauge_normal.png (Normal Flow - ~7.5m)",
            "images/gauge_warning.png (High Flow - ~13.5m)",
            "images/gauge_flood.png (Critical Flood - ~19.0m)"
        };
        cmbPresetImages = new JComboBox<>(presetImages);
        cmbPresetImages.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cmbPresetImages.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbPresetImages.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbPresetImages.addActionListener(e -> {
            String selected = (String) cmbPresetImages.getSelectedItem();
            if (selected != null) {
                String path = selected.split(" ")[0].trim();
                loadImagePreview(new File(path));
            }
        });

        JPanel fileBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fileBtnRow.setBackground(COLOR_SURFACE);
        fileBtnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnBrowse = createStyledButton("📁 Browse Disk Image...", COLOR_CARD);
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("images"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loadImagePreview(chooser.getSelectedFile());
            }
        });

        JButton btnRunCv = createStyledButton("⚡ Run Waterline Detection", COLOR_ACCENT);
        btnRunCv.addActionListener(e -> executeImageProcessing());

        fileBtnRow.add(btnBrowse);
        fileBtnRow.add(btnRunCv);

        // Results text area
        txtImageResults = new JTextArea();
        txtImageResults.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtImageResults.setBackground(new Color(15, 23, 42));
        txtImageResults.setForeground(COLOR_TEXT_LIGHT);
        txtImageResults.setEditable(false);
        txtImageResults.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane resultsScroll = new JScrollPane(txtImageResults);
        resultsScroll.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(COLOR_CARD, 1), "Algorithmic Detection Metrics (UNIT V)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif", Font.BOLD, 11), COLOR_TEXT_LIGHT
        ));
        resultsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        controlPanel.add(cvTitle);
        controlPanel.add(Box.createVerticalStrut(4));
        controlPanel.add(cvDesc);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(cmbPresetImages);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(fileBtnRow);
        controlPanel.add(Box.createVerticalStrut(14));
        controlPanel.add(resultsScroll);

        tab.add(previewPanel, BorderLayout.CENTER);
        tab.add(controlPanel, BorderLayout.EAST);

        // Initial preview load
        loadImagePreview(new File("images/gauge_normal.png"));

        return tab;
    }

    private void loadImagePreview(File file) {
        if (file != null && file.exists()) {
            this.selectedImageFile = file;
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            Image img = icon.getImage();
            Image scaled = img.getScaledInstance(380, 460, Image.SCALE_SMOOTH);
            lblImagePreview.setIcon(new ImageIcon(scaled));
            lblImagePreview.setText("");
        } else {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Image not found: " + (file != null ? file.getPath() : "null"));
        }
    }

    private void executeImageProcessing() {
        if (selectedImageFile == null || !selectedImageFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select an existing gauge image file.", "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double maxScale = 25.0;
            GaugeProcessingResult result = imageProcessor.processGaugeImage(selectedImageFile.getAbsolutePath(), maxScale);

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================\n");
            sb.append("   IMAGE PROCESSING ANALYSIS REPORT      \n");
            sb.append("=========================================\n");
            sb.append(String.format("Target Image     : %s\n", selectedImageFile.getName()));
            sb.append(String.format("Status           : %s\n", result.isSuccess() ? "SUCCESS" : "FAILED"));
            sb.append(String.format("Detected Line Y  : %d px\n", result.getDetectedWaterLineY()));
            sb.append(String.format("Estimated Level  : %.2f meters\n", result.getEstimatedWaterLevelMeters()));
            sb.append(String.format("Confidence Score : %.1f %%\n", result.getConfidencePercent()));
            sb.append(String.format("Calculated Alert : %s\n", WaterLevelRecord.evaluateAlertStatus(result.getEstimatedWaterLevelMeters())));
            sb.append("-----------------------------------------\n");
            sb.append("Processing Logs:\n");
            sb.append(result.getAlgorithmSummary());
            sb.append("\n=========================================\n");

            txtImageResults.setText(sb.toString());

            // Update gauge visualizer with detected waterline
            gaugePanel.setCurrentWaterLevel(result.getEstimatedWaterLevelMeters());

        } catch (Exception ex) {
            txtImageResults.setText("Execution Error: " + ex.getMessage());
        }
    }

    // ==========================================
    // TAB 4: REAL-TIME IOT TELEMETRY & CONCURRENCY
    // ==========================================
    private JPanel createTelemetryTab() {
        JPanel tab = new JPanel(new BorderLayout(14, 14));
        tab.setBackground(COLOR_BG_DARK);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Top Control Toolbar
        JPanel simToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        simToolbar.setBackground(COLOR_SURFACE);
        simToolbar.setBorder(new LineBorder(COLOR_CARD, 1, true));

        btnStartSimulation = createStyledButton("▶ Start IoT Sensor Threads", COLOR_SUCCESS);
        btnStartSimulation.addActionListener(e -> startSimulationWorkers());

        btnStopSimulation = createStyledButton("⏹ Stop Simulation", COLOR_CARD);
        btnStopSimulation.setEnabled(simulationManager.isRunning());
        btnStopSimulation.addActionListener(e -> stopSimulationWorkers());

        btnSurgeFlood = createStyledButton("⚡ Inject Basin Cloudburst Surge (+4.5m)", COLOR_DANGER);
        btnSurgeFlood.addActionListener(e -> injectFloodSurge());

        JButton btnClearLogs = createStyledButton("🗑 Clear Stream", COLOR_CARD);
        btnClearLogs.addActionListener(e -> txtTelemetryLog.setText(""));

        simToolbar.add(btnStartSimulation);
        simToolbar.add(btnStopSimulation);
        simToolbar.add(btnSurgeFlood);
        simToolbar.add(btnClearLogs);

        tab.add(simToolbar, BorderLayout.NORTH);

        // Center Split: Left = Thread Status Table, Right = Live Telemetry Log
        String[] threadCols = {"Worker Thread", "Station Target", "River", "Thread State", "Priority", "Status"};
        threadsTableModel = new DefaultTableModel(threadCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        threadsTable = new JTable(threadsTableModel);
        styleTable(threadsTable);

        JScrollPane threadScroll = new JScrollPane(threadsTable);
        threadScroll.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(COLOR_CARD, 1), "Concurrent Worker Threads (java.lang.Runnable)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif", Font.BOLD, 12), COLOR_TEXT_LIGHT
        ));

        txtTelemetryLog = new JTextArea();
        txtTelemetryLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtTelemetryLog.setBackground(new Color(15, 23, 42));
        txtTelemetryLog.setForeground(new Color(56, 189, 248)); // Cyan stream
        txtTelemetryLog.setEditable(false);

        JScrollPane logScroll = new JScrollPane(txtTelemetryLog);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(COLOR_CARD, 1), "Live Telemetry Event Log Stream (SensorEventListener Callback)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif", Font.BOLD, 12), COLOR_TEXT_LIGHT
        ));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, threadScroll, logScroll);
        splitPane.setResizeWeight(0.35);
        splitPane.setBackground(COLOR_BG_DARK);
        splitPane.setBorder(null);

        tab.add(splitPane, BorderLayout.CENTER);
        refreshThreadsTable();
        return tab;
    }

    private void refreshThreadsTable() {
        threadsTableModel.setRowCount(0);
        List<RiverStation> stations = monitoringService.getAllStations();
        boolean isRunning = simulationManager.isRunning();

        for (int i = 0; i < stations.size(); i++) {
            RiverStation s = stations.get(i);
            String threadName = "SensorWorker-" + s.getStationId();
            String state = isRunning ? "TIMED_WAITING (Active)" : "TERMINATED (Stopped)";
            String status = isRunning ? "RUNNING" : "IDLE";
            threadsTableModel.addRow(new Object[]{
                threadName,
                s.getStationName(),
                s.getRiverName(),
                state,
                "NORM_PRIORITY (5)",
                status
            });
        }
    }

    private void startSimulationWorkers() {
        if (!simulationManager.isRunning()) {
            simulationManager.setDefaultIntervalMillis(1200); // 1.2s tick
            simulationManager.startSimulation();
            btnStartSimulation.setEnabled(false);
            btnStopSimulation.setEnabled(true);
            refreshThreadsTable();
            appendTelemetryLog("[SIMULATION STARTED] Spawning independent IoT worker threads for all registered river stations.\n");
        }
    }

    private void stopSimulationWorkers() {
        if (simulationManager.isRunning()) {
            simulationManager.stopSimulation();
            btnStartSimulation.setEnabled(true);
            btnStopSimulation.setEnabled(false);
            refreshThreadsTable();
            appendTelemetryLog("[SIMULATION STOPPED] All background sensor threads safely stopped.\n");
        }
    }

    private void injectFloodSurge() {
        simulationManager.triggerBasinSurge(4.5);
        appendTelemetryLog("⚠️ [SURGE INJECTION] High cloudburst surge (+4.5m) injected across river stations!\n");
    }

    private void appendTelemetryLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtTelemetryLog.append(msg);
            txtTelemetryLog.setCaretPosition(txtTelemetryLog.getDocument().getLength());
        });
    }

    // SensorEventListener Callbacks (Delivered dynamically from worker threads)
    @Override
    public void onReadingReceived(SensorEvent event) {
        SwingUtilities.invokeLater(() -> {
            String line = String.format("[%s] %-12s | %-24s | %.2f m | %s\n",
                event.getTimestamp(), event.getStationId(), event.getStationName(), event.getWaterLevelMeters(), event.getAlertStatus());
            appendTelemetryLog(line);

            // If this event matches the currently visualized station, update gauge directly
            if (gaugePanel != null && gaugePanel.getStation() != null) {
                if (gaugePanel.getStation().getStationId().equalsIgnoreCase(event.getStationId())) {
                    gaugePanel.setCurrentWaterLevel(event.getWaterLevelMeters());
                }
            }
        });
    }

    @Override
    public void onAlertTriggered(SensorEvent event) {
        SwingUtilities.invokeLater(() -> {
            String alert = String.format("🚨 ALERT DISPATCHED: Station %s exceeded threshold! Water Level: %.2f m [%s]\n",
                event.getStationName(), event.getWaterLevelMeters(), event.getAlertStatus());
            appendTelemetryLog(alert);
        });
    }

    @Override
    public void onSimulationStatusChanged(String statusMessage) {
        appendTelemetryLog("ℹ️ " + statusMessage + "\n");
    }

    // ==========================================
    // TAB 5: RELATIONAL JDBC SQL CONSOLE
    // ==========================================
    private JPanel createJdbcConsoleTab() {
        JPanel tab = new JPanel(new BorderLayout(12, 12));
        tab.setBackground(COLOR_BG_DARK);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Top SQL Input Box
        JPanel queryPanel = new JPanel(new BorderLayout(8, 8));
        queryPanel.setBackground(COLOR_SURFACE);
        queryPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblSqlPrompt = new JLabel("Enter SQL Query (Executed via java.sql.Statement / PreparedStatement):");
        lblSqlPrompt.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblSqlPrompt.setForeground(COLOR_TEXT_LIGHT);

        txtSqlQuery = new JTextField("SELECT * FROM stations");
        txtSqlQuery.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtSqlQuery.setBackground(COLOR_BG_DARK);
        txtSqlQuery.setForeground(COLOR_TEXT_LIGHT);
        txtSqlQuery.setCaretColor(COLOR_TEXT_LIGHT);
        txtSqlQuery.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD, 1),
            new EmptyBorder(6, 8, 6, 8)
        ));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(COLOR_SURFACE);

        JButton btnExecute = createStyledButton("▶ Execute Query", COLOR_ACCENT);
        btnExecute.addActionListener(e -> executeSqlQuery());

        JButton btnPreset1 = createStyledButton("Stations Table", COLOR_CARD);
        btnPreset1.addActionListener(e -> {
            txtSqlQuery.setText("SELECT * FROM stations");
            executeSqlQuery();
        });

        JButton btnPreset2 = createStyledButton("Recent Readings", COLOR_CARD);
        btnPreset2.addActionListener(e -> {
            txtSqlQuery.setText("SELECT * FROM readings ORDER BY id DESC");
            executeSqlQuery();
        });

        JButton btnPreset3 = createStyledButton("Critical Flood Readings", COLOR_CARD);
        btnPreset3.addActionListener(e -> {
            txtSqlQuery.setText("SELECT * FROM readings WHERE alert_status LIKE 'CRITICAL%'");
            executeSqlQuery();
        });

        btnRow.add(btnExecute);
        btnRow.add(btnPreset1);
        btnRow.add(btnPreset2);
        btnRow.add(btnPreset3);

        queryPanel.add(lblSqlPrompt, BorderLayout.NORTH);
        queryPanel.add(txtSqlQuery, BorderLayout.CENTER);
        queryPanel.add(btnRow, BorderLayout.SOUTH);

        tab.add(queryPanel, BorderLayout.NORTH);

        // Results Table
        sqlResultsTableModel = new DefaultTableModel();
        sqlResultsTable = new JTable(sqlResultsTableModel);
        styleTable(sqlResultsTable);

        JScrollPane resultsScroll = new JScrollPane(sqlResultsTable);
        resultsScroll.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(COLOR_CARD, 1), "Relational ResultSet Tabular View (ResultSetMetaData)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif", Font.BOLD, 12), COLOR_TEXT_LIGHT
        ));

        lblSqlStatus = new JLabel("Ready. Enter SQL statement and click Execute.");
        lblSqlStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblSqlStatus.setForeground(COLOR_TEXT_MUTED);
        lblSqlStatus.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(resultsScroll, BorderLayout.CENTER);
        centerPanel.add(lblSqlStatus, BorderLayout.SOUTH);

        tab.add(centerPanel, BorderLayout.CENTER);
        return tab;
    }

    private void executeSqlQuery() {
        String sql = txtSqlQuery.getText().trim();
        if (sql.isEmpty()) return;

        try {
            Connection conn = DatabaseConnectionManager.getInstance().getConnection();
            try (Statement stmt = conn.createStatement()) {
                boolean isResultSet = stmt.execute(sql);
                if (isResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();

                        Vector<String> colNames = new Vector<>();
                        for (int i = 1; i <= colCount; i++) {
                            colNames.add(md.getColumnName(i));
                        }

                        Vector<Vector<Object>> data = new Vector<>();
                        int rows = 0;
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            data.add(row);
                            rows++;
                        }

                        sqlResultsTableModel.setDataVector(data, colNames);
                        lblSqlStatus.setText(String.format("Query executed successfully: %d row(s) returned.", rows));
                        lblSqlStatus.setForeground(COLOR_SUCCESS);
                    }
                } else {
                    int updateCount = stmt.getUpdateCount();
                    sqlResultsTableModel.setRowCount(0);
                    sqlResultsTableModel.setColumnCount(0);
                    lblSqlStatus.setText(String.format("Statement executed successfully: %d row(s) affected.", updateCount));
                    lblSqlStatus.setForeground(COLOR_SUCCESS);
                }
            }
        } catch (Exception ex) {
            lblSqlStatus.setText("SQL Error: " + ex.getMessage());
            lblSqlStatus.setForeground(COLOR_DANGER);
        }
    }

    // ==========================================
    // DATA REFRESH & PERSISTENCE ENGINE SWITCH
    // ==========================================
    public void refreshAllData() {
        List<RiverStation> stations = monitoringService.getAllStations();
        List<WaterLevelRecord> records = monitoringService.getAllRecords();

        // Update metrics
        if (lblTotalStations != null) lblTotalStations.setText(String.valueOf(stations.size()));
        if (lblTotalReadings != null) lblTotalReadings.setText(String.valueOf(records.size()));
        if (lblCriticalAlerts != null) lblCriticalAlerts.setText(String.valueOf(monitoringService.getCriticalAlertRecords().size()));
        if (lblAvgLevel != null) lblAvgLevel.setText(String.format("%.2f m", monitoringService.getAverageWaterLevel()));

        // Update stations table
        if (stationsTableModel != null) {
            stationsTableModel.setRowCount(0);
            for (RiverStation s : stations) {
                // Find latest reading for station
                double latest = s.getNormalLevelMeters();
                String status = "NORMAL (Baseline)";
                for (int i = records.size() - 1; i >= 0; i--) {
                    WaterLevelRecord r = records.get(i);
                    if (r.getStationLocation().contains(s.getStationId())) {
                        latest = r.getWaterLevelMeters();
                        status = r.getAlertStatus();
                        break;
                    }
                }
                stationsTableModel.addRow(new Object[]{
                    s.getStationId(),
                    s.getStationName(),
                    s.getRiverName(),
                    String.format("%.2f m", s.getNormalLevelMeters()),
                    String.format("%.2f m", s.getDangerLevelMeters()),
                    String.format("%.2f m", latest),
                    status
                });
            }
        }

        // Update station combo box
        if (stationComboBox != null) {
            stationComboBox.removeAllItems();
            for (RiverStation s : stations) {
                stationComboBox.addItem(s.getStationId() + " - " + s.getStationName());
            }
            if (!stations.isEmpty()) {
                updateGaugeToSelectedStation(stations.get(0));
            }
        }

        // Update storage footer label
        if (lblStorageEngine != null) {
            lblStorageEngine.setText("Active Storage: " + monitoringService.getStationDAO().getStorageSource());
        }
    }

    private void switchToJdbcStorage() {
        try {
            DatabaseConnectionManager connMgr = DatabaseConnectionManager.getInstance();
            StationJdbcDAO stationJdbc = new StationJdbcDAO(connMgr);
            WaterLevelRecordJdbcDAO recordJdbc = new WaterLevelRecordJdbcDAO(connMgr);
            monitoringService.switchStorageEngine(stationJdbc, recordJdbc, false);
            refreshAllData();
            JOptionPane.showMessageDialog(this, "Active persistence successfully switched to Relational JDBC Driver!", "Engine Switched", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to switch to JDBC: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void switchToCsvStorage() {
        try {
            dao.StationFileDAO stationFile = new dao.StationFileDAO();
            dao.WaterLevelRecordFileDAO recordFile = new dao.WaterLevelRecordFileDAO();
            monitoringService.switchStorageEngine(stationFile, recordFile, false);
            refreshAllData();
            JOptionPane.showMessageDialog(this, "Active persistence successfully switched to CSV File Storage!", "Engine Switched", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to switch to CSV: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // DIALOGS & ACTION POPUPS
    // ==========================================
    private void openRegisterStationDialog() {
        JDialog dialog = new JDialog(this, "Register New River Station", true);
        dialog.setSize(420, 360);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(COLOR_SURFACE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 12));
        form.setBackground(COLOR_SURFACE);
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtId = new JTextField("STN-NEW-01");
        JTextField txtName = new JTextField("Downstream Delta Station");
        JTextField txtRiver = new JTextField("Ganga River");
        JTextField txtNormal = new JTextField("7.0");
        JTextField txtDanger = new JTextField("16.0");

        form.add(createFormLabel("Station ID:"));
        form.add(txtId);
        form.add(createFormLabel("Station Name:"));
        form.add(txtName);
        form.add(createFormLabel("River Name:"));
        form.add(txtRiver);
        form.add(createFormLabel("Normal Level (m):"));
        form.add(txtNormal);
        form.add(createFormLabel("Danger Level (m):"));
        form.add(txtDanger);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setBackground(COLOR_SURFACE);

        JButton btnSave = createStyledButton("Save Station", COLOR_ACCENT);
        btnSave.addActionListener(e -> {
            try {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();
                String river = txtRiver.getText().trim();
                double normal = Double.parseDouble(txtNormal.getText().trim());
                double danger = Double.parseDouble(txtDanger.getText().trim());

                RiverStation stn = new RiverStation(id, name, river, normal, danger);
                monitoringService.registerStation(stn);
                refreshAllData();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Station " + id + " registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Registration Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = createStyledButton("Cancel", COLOR_CARD);
        btnCancel.addActionListener(e -> dialog.dispose());

        btnRow.add(btnCancel);
        btnRow.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openManualReadingDialog() {
        RiverStation active = gaugePanel.getStation();
        if (active == null && !monitoringService.getAllStations().isEmpty()) {
            active = monitoringService.getAllStations().get(0);
        }
        if (active == null) {
            JOptionPane.showMessageDialog(this, "No stations registered.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this, 
            "Enter current water level measurement in meters for " + active.getStationName() + " (" + active.getStationId() + "):",
            String.valueOf(active.getNormalLevelMeters()));

        if (input != null && !input.trim().isEmpty()) {
            try {
                double level = Double.parseDouble(input.trim());
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
                WaterLevelRecord rec = monitoringService.recordMeasurement(active.getStationId(), level, ts);
                refreshAllData();
                JOptionPane.showMessageDialog(this, 
                    "Measurement Recorded!\nRecord ID: " + rec.getRecordId() + "\nWater Level: " + level + "m\nAlert Status: " + rec.getAlertStatus(),
                    "Record Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Measurement Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCriticalAlertsDialog() {
        List<WaterLevelRecord> alerts = monitoringService.getCriticalAlertRecords();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total Critical Alerts: %d\n\n", alerts.size()));
        for (WaterLevelRecord r : alerts) {
            sb.append(String.format("• [%s] %s - %.2f m (%s)\n", r.getTimestamp(), r.getStationLocation(), r.getWaterLevelMeters(), r.getAlertStatus()));
        }
        if (alerts.isEmpty()) {
            sb.append("No critical flood warnings currently logged.");
        }

        JTextArea area = new JTextArea(sb.toString(), 16, 40);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Critical Flood Alert History", JOptionPane.WARNING_MESSAGE);
    }

    private void showAboutDialog() {
        String msg = "Smart River Water Level Monitoring System\n"
                   + "Version: v0.11 (Day 11: Java Swing Desktop Dashboard)\n"
                   + "Author: B.Sandhanakumar\n"
                   + "Academic Syllabus: UNIT V - Event-Driven Programming & Desktop GUI Architecture\n"
                   + "Core Technologies: Java SE 21, Swing, AWT, Graphics2D, JDBC, Socket Sockets";
        JOptionPane.showMessageDialog(this, msg, "About Smart River Monitoring System", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSyllabusDialog() {
        String msg = "Syllabus UNIT V Topics Demonstrated:\n\n"
                   + "1. Java Swing Event-Driven Architecture (JFrame, JTabbedPane, JTable, JSlider, JComboBox)\n"
                   + "2. Custom Graphics2D Painting (paintComponent, GeneralPath wave modeling, gradient shaders)\n"
                   + "3. Concurrency Thread Binding (SensorEventListener delivered via SwingUtilities.invokeLater)\n"
                   + "4. Computer Vision Image Processing Integration (BufferedImage, staff gauge waterline extraction)\n"
                   + "5. Relational JDBC Console (Connection, Statement, ResultSet, ResultSetMetaData dynamic tables)\n";
        JOptionPane.showMessageDialog(this, msg, "Syllabus UNIT V Reference", JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(COLOR_TEXT_LIGHT);
        return label;
    }

    public static JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bg.darker(), 1),
            new EmptyBorder(6, 12, 6, 12)
        ));
        btn.setOpaque(true);
        return btn;
    }

    public static void styleTable(JTable table) {
        table.setBackground(COLOR_SURFACE);
        table.setForeground(COLOR_TEXT_LIGHT);
        table.setGridColor(COLOR_CARD);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.getTableHeader().setBackground(COLOR_CARD);
        table.getTableHeader().setForeground(COLOR_TEXT_LIGHT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
    }

    // Getters for testing and programmatic automation
    public RiverMonitoringService getMonitoringService() {
        return monitoringService;
    }

    public RiverSimulationManager getSimulationManager() {
        return simulationManager;
    }

    public RiverGaugeVisualizerPanel getGaugePanel() {
        return gaugePanel;
    }

    public JTabbedPane getMainTabbedPane() {
        return mainTabbedPane;
    }

    public JTable getStationsTable() {
        return stationsTable;
    }

    public DefaultTableModel getStationsTableModel() {
        return stationsTableModel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RiverMonitoringService service = new RiverMonitoringService();
            RiverSimulationManager simManager = new RiverSimulationManager(service);
            RiverMonitoringGUI gui = new RiverMonitoringGUI(service, simManager);
            gui.setVisible(true);
        });
    }
}
