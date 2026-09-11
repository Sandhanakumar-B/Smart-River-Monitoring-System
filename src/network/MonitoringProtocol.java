package network;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 9: Java Networking & Socket Programming — Shared Protocol Constants
 * Syllabus Unit: UNIT V - Java Networking, TCP/IP Sockets, Client-Server Architecture
 *
 * Defines all text-based command and response tokens for the river monitoring
 * client-server communication protocol.  Both MonitoringServer and
 * MonitoringClient reference these constants so that the protocol is defined
 * in a single place.
 */
public final class MonitoringProtocol {

    private MonitoringProtocol() { /* utility class — no instances */ }

    // ---------------------------------------------------------------------------
    // Default network settings
    // ---------------------------------------------------------------------------
    public static final int    DEFAULT_PORT    = 9876;
    public static final String SERVER_HOST     = "127.0.0.1";
    public static final int    SOCKET_TIMEOUT_MS = 30_000; // 30 s read timeout

    // ---------------------------------------------------------------------------
    // Client → Server commands
    // ---------------------------------------------------------------------------
    /** List all registered river stations */
    public static final String CMD_LIST_STATIONS     = "LIST_STATIONS";

    /** Get the latest water level reading for a specific station.
     *  Usage: GET_LEVEL <stationId> */
    public static final String CMD_GET_LEVEL         = "GET_LEVEL";

    /** Retrieve all historical readings for a station.
     *  Usage: GET_HISTORY <stationId> */
    public static final String CMD_GET_HISTORY       = "GET_HISTORY";

    /** Retrieve only flood-alert (danger-level) records */
    public static final String CMD_GET_ALERTS        = "GET_ALERTS";

    /** Query overall basin analytics (average level, peak, alert count) */
    public static final String CMD_BASIN_STATS       = "BASIN_STATS";

    /** Request server status / uptime info */
    public static final String CMD_SERVER_STATUS     = "SERVER_STATUS";

    /** Gracefully close this client session */
    public static final String CMD_QUIT              = "QUIT";

    // ---------------------------------------------------------------------------
    // Server → Client response prefixes
    // ---------------------------------------------------------------------------
    public static final String RESP_OK               = "OK";
    public static final String RESP_ERROR            = "ERROR";
    public static final String RESP_END              = "END";   // marks end of multi-line response
    public static final String RESP_WELCOME          = "WELCOME";

    // ---------------------------------------------------------------------------
    // Field separator used inside compound responses
    // ---------------------------------------------------------------------------
    public static final String FIELD_SEP             = "|";
}
