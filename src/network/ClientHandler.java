package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.RiverStation;
import model.WaterLevelRecord;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 9: Java Networking & Socket Programming — Per-Client Request Handler Thread
 * Syllabus Unit: UNIT V - ServerSocket, Socket I/O Streams, Multi-Client Threading
 *
 * Each accepted client connection is handed off to a dedicated ClientHandler
 * Runnable that runs on its own thread inside the server.  The handler:
 *   1. Greets the client with a WELCOME banner.
 *   2. Reads line-delimited text commands from the client's InputStream.
 *   3. Delegates to RiverMonitoringService for business logic.
 *   4. Writes line-delimited text responses back on the client's OutputStream.
 *   5. Gracefully closes the socket when the client sends QUIT or disconnects.
 *
 * This class demonstrates:
 *   - java.net.Socket and its I/O streams (BufferedReader / PrintWriter)
 *   - Thread-per-client concurrency model
 *   - Clean resource management via try-with-resources
 */
public class ClientHandler implements Runnable {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Socket         clientSocket;
    private final RiverMonitoringService monitoringService;
    private final int            clientId;
    private final String         remoteAddress;

    public ClientHandler(Socket clientSocket,
                         RiverMonitoringService monitoringService,
                         int clientId) {
        this.clientSocket      = clientSocket;
        this.monitoringService = monitoringService;
        this.clientId          = clientId;
        this.remoteAddress     = clientSocket.getRemoteSocketAddress().toString();
    }

    // -----------------------------------------------------------------------
    // Runnable entry-point
    // -----------------------------------------------------------------------
    @Override
    public void run() {
        System.out.printf("  [Server] Client #%d connected from %s%n", clientId, remoteAddress);

        try {
            clientSocket.setSoTimeout(MonitoringProtocol.SOCKET_TIMEOUT_MS);
        } catch (IOException e) {
            System.err.println("  [Server] Failed to set socket timeout: " + e.getMessage());
        }

        // try-with-resources auto-closes streams and socket
        try (
            BufferedReader in  = new BufferedReader(
                                     new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter    out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // 1. Send welcome banner
            out.println(MonitoringProtocol.RESP_WELCOME +
                " SmartRiverMonitor-Server v0.9 | Client #" + clientId +
                " | Connected: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            out.println("Commands: LIST_STATIONS | GET_LEVEL <id> | GET_HISTORY <id> " +
                "| GET_ALERTS | BASIN_STATS | SERVER_STATUS | QUIT");
            out.println(MonitoringProtocol.RESP_END);

            // 2. Command loop
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                System.out.printf("  [Client #%d] CMD: %s%n", clientId, line);

                if (line.equalsIgnoreCase(MonitoringProtocol.CMD_QUIT)) {
                    out.println(MonitoringProtocol.RESP_OK + " Goodbye! Connection closing.");
                    break;
                }

                handleCommand(line, out);
            }

        } catch (SocketTimeoutException e) {
            System.out.printf("  [Server] Client #%d timed out (no activity for %ds).%n",
                clientId, MonitoringProtocol.SOCKET_TIMEOUT_MS / 1000);
        } catch (IOException e) {
            System.out.printf("  [Server] Client #%d disconnected: %s%n",
                clientId, e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) { }
            System.out.printf("  [Server] Client #%d session closed.%n", clientId);
        }
    }

    // -----------------------------------------------------------------------
    // Command dispatch
    // -----------------------------------------------------------------------
    private void handleCommand(String rawLine, PrintWriter out) {
        String[] parts   = rawLine.split("\\s+", 2);
        String   command = parts[0].toUpperCase();
        String   arg     = (parts.length > 1) ? parts[1].trim() : "";

        switch (command) {
            case MonitoringProtocol.CMD_LIST_STATIONS:
                handleListStations(out);
                break;
            case MonitoringProtocol.CMD_GET_LEVEL:
                handleGetLevel(arg, out);
                break;
            case MonitoringProtocol.CMD_GET_HISTORY:
                handleGetHistory(arg, out);
                break;
            case MonitoringProtocol.CMD_GET_ALERTS:
                handleGetAlerts(out);
                break;
            case MonitoringProtocol.CMD_BASIN_STATS:
                handleBasinStats(out);
                break;
            case MonitoringProtocol.CMD_SERVER_STATUS:
                handleServerStatus(out);
                break;
            default:
                out.println(MonitoringProtocol.RESP_ERROR +
                    " Unknown command: " + command +
                    ". Type QUIT to disconnect.");
                out.println(MonitoringProtocol.RESP_END);
        }
    }

    // -----------------------------------------------------------------------
    // Individual command handlers
    // -----------------------------------------------------------------------

    /** LIST_STATIONS → returns one line per station: OK|stationId|name|river|normalM|dangerM */
    private void handleListStations(PrintWriter out) {
        List<RiverStation> stations = monitoringService.getAllStations();
        out.println(MonitoringProtocol.RESP_OK + " " + stations.size() + " station(s) registered:");
        for (RiverStation s : stations) {
            out.println(MonitoringProtocol.FIELD_SEP
                + s.getStationId()           + MonitoringProtocol.FIELD_SEP
                + s.getStationName()         + MonitoringProtocol.FIELD_SEP
                + s.getRiverName()           + MonitoringProtocol.FIELD_SEP
                + String.format("%.2f", s.getNormalLevelMeters()) + "m"
                                             + MonitoringProtocol.FIELD_SEP
                + String.format("%.2f", s.getDangerLevelMeters()) + "m danger"
            );
        }
        out.println(MonitoringProtocol.RESP_END);
    }

    /** GET_LEVEL <stationId> → returns the most-recent reading for that station */
    private void handleGetLevel(String stationId, PrintWriter out) {
        if (stationId.isEmpty()) {
            out.println(MonitoringProtocol.RESP_ERROR + " Usage: GET_LEVEL <stationId>");
            return;
        }

        List<WaterLevelRecord> all = monitoringService.getAllRecords();
        WaterLevelRecord latest = null;
        for (WaterLevelRecord r : all) {
            if (r.getStationLocation().toUpperCase().contains(stationId.toUpperCase())) {
                latest = r; // CSV is appended-in-order, so last match is latest
            }
        }

        if (latest == null) {
            out.println(MonitoringProtocol.RESP_ERROR
                + " No readings found for station: " + stationId);
            out.println(MonitoringProtocol.RESP_END);
        } else {
            out.println(MonitoringProtocol.RESP_OK
                + " Latest reading for " + stationId + ":");
            out.println(MonitoringProtocol.FIELD_SEP
                + latest.getStationLocation()                             + MonitoringProtocol.FIELD_SEP
                + String.format("%.2f", latest.getWaterLevelMeters()) + "m" + MonitoringProtocol.FIELD_SEP
                + latest.getAlertStatus()                               + MonitoringProtocol.FIELD_SEP
                + latest.getTimestamp()
            );
        }
        out.println(MonitoringProtocol.RESP_END);
    }

    /** GET_HISTORY <stationId> → returns all records for that station */
    private void handleGetHistory(String stationId, PrintWriter out) {
        if (stationId.isEmpty()) {
            out.println(MonitoringProtocol.RESP_ERROR + " Usage: GET_HISTORY <stationId>");
            return;
        }

        List<WaterLevelRecord> all = monitoringService.getAllRecords();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (WaterLevelRecord r : all) {
            if (r.getStationLocation().toUpperCase().contains(stationId.toUpperCase())) {
                sb.append(MonitoringProtocol.FIELD_SEP)
                  .append(r.getStationLocation()).append(MonitoringProtocol.FIELD_SEP)
                  .append(String.format("%.2f", r.getWaterLevelMeters())).append("m")
                  .append(MonitoringProtocol.FIELD_SEP)
                  .append(r.getAlertStatus()).append(MonitoringProtocol.FIELD_SEP)
                  .append(r.getTimestamp())
                  .append("\n");
                count++;
            }
        }

        if (count == 0) {
            out.println(MonitoringProtocol.RESP_ERROR
                + " No history found for station: " + stationId);
            out.println(MonitoringProtocol.RESP_END);
        } else {
            out.println(MonitoringProtocol.RESP_OK
                + " History for " + stationId + " (" + count + " records):");
            out.print(sb.toString()); // already newline-terminated
        }
        out.println(MonitoringProtocol.RESP_END);
    }

    /** GET_ALERTS → returns all readings that triggered a flood alert */
    private void handleGetAlerts(PrintWriter out) {
        List<WaterLevelRecord> alerts = monitoringService.getCriticalAlertRecords();
        out.println(MonitoringProtocol.RESP_OK
            + " Critical flood-alert records (" + alerts.size() + "):");
        for (WaterLevelRecord r : alerts) {
            out.println(MonitoringProtocol.FIELD_SEP
                + r.getStationLocation()                              + MonitoringProtocol.FIELD_SEP
                + String.format("%.2f", r.getWaterLevelMeters()) + "m" + MonitoringProtocol.FIELD_SEP
                + r.getAlertStatus()                                  + MonitoringProtocol.FIELD_SEP
                + r.getTimestamp()
            );
        }
        out.println(MonitoringProtocol.RESP_END);
    }

    /** BASIN_STATS → average level, peak level, alert count, total records */
    private void handleBasinStats(PrintWriter out) {
        List<WaterLevelRecord> all = monitoringService.getAllRecords();
        int     total    = all.size();
        int     alerts   = monitoringService.getCriticalAlertRecords().size();
        double  avg      = monitoringService.getAverageWaterLevel();
        double  peak     = monitoringService.getMaxRecordedWaterLevel();
        int     stations = monitoringService.getTotalStationsCount();

        out.println(MonitoringProtocol.RESP_OK + " Basin-Wide Analytics:");
        out.println(MonitoringProtocol.FIELD_SEP + "Registered Stations  : " + stations);
        out.println(MonitoringProtocol.FIELD_SEP + "Total Readings       : " + total);
        out.println(MonitoringProtocol.FIELD_SEP + "Critical Flood Alerts: " + alerts);
        out.println(MonitoringProtocol.FIELD_SEP
            + "Average Water Level  : " + String.format("%.2f", avg) + " m");
        out.println(MonitoringProtocol.FIELD_SEP
            + "Peak Water Level     : " + String.format("%.2f", peak) + " m");
        out.println(MonitoringProtocol.RESP_END);
    }

    /** SERVER_STATUS → lightweight server health-check */
    private void handleServerStatus(PrintWriter out) {
        out.println(MonitoringProtocol.RESP_OK
            + " SmartRiverMonitor-Server | Port " + MonitoringProtocol.DEFAULT_PORT
            + " | Client #" + clientId
            + " | " + LocalDateTime.now().format(TIMESTAMP_FMT));
        out.println(MonitoringProtocol.RESP_END);
    }
}
