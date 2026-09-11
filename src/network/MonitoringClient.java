package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 9: Java Networking & Socket Programming — Interactive TCP Client
 * Syllabus Unit: UNIT V - Socket, InputStream / OutputStream, Client-Server Communication
 *
 * MonitoringClient connects to a running MonitoringServer over TCP and provides
 * an interactive command-line session so a user can remotely query river data.
 *
 * Demonstrates:
 *   - java.net.Socket (client-side) — creation, connection, and I/O stream wrapping
 *   - BufferedReader / PrintWriter for line-based text protocol
 *   - SocketTimeoutException handling for unresponsive server
 *   - Clean resource management with try-with-resources
 *   - Can also be used programmatically (headless mode) for automated queries
 */
public class MonitoringClient {

    private final String host;
    private final int    port;

    public MonitoringClient() {
        this(MonitoringProtocol.SERVER_HOST, MonitoringProtocol.DEFAULT_PORT);
    }

    public MonitoringClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // -----------------------------------------------------------------------
    // Interactive REPL — used from Main menu (Option 10-7)
    // -----------------------------------------------------------------------

    /**
     * Opens a TCP connection and runs an interactive command session via the
     * provided Scanner.  Prints server responses to stdout.
     *
     * @param userScanner the Scanner already attached to System.in in Main
     */
    public void runInteractiveSession(Scanner userScanner) {
        System.out.println("\n  [Client] Connecting to " + host + ":" + port + " ...");

        try (
            Socket       socket = new Socket(host, port);
            BufferedReader in   = new BufferedReader(
                                      new InputStreamReader(socket.getInputStream()));
            PrintWriter   out   = new PrintWriter(socket.getOutputStream(), true)
        ) {
            socket.setSoTimeout(15_000); // 15 s read timeout per response line

            System.out.println("  [Client] ✅ Connected! Receiving server banner...\n");

            // Print welcome banner (multi-line, terminated by END)
            printServerResponse(in);

            System.out.println("  [Client] Enter commands (or QUIT to disconnect):");
            System.out.println("  Available: LIST_STATIONS | GET_LEVEL <id> | GET_HISTORY <id>");
            System.out.println("             GET_ALERTS | BASIN_STATS | SERVER_STATUS | QUIT");
            System.out.println("  -----------------------------------------------------------");

            while (true) {
                System.out.print("\n  client> ");
                String cmd = userScanner.nextLine().trim();
                if (cmd.isEmpty()) continue;

                out.println(cmd); // send command to server

                if (cmd.equalsIgnoreCase(MonitoringProtocol.CMD_QUIT)) {
                    // Read the goodbye message then exit
                    String reply = in.readLine();
                    if (reply != null) System.out.println("  " + reply);
                    break;
                }

                printServerResponse(in); // print multi-line response until END
            }

            System.out.println("\n  [Client] Session closed.");

        } catch (UnknownHostException e) {
            System.out.println("  [Client] ❌ Unknown host: " + host);
        } catch (SocketTimeoutException e) {
            System.out.println("  [Client] ❌ Server did not respond in time (timeout).");
        } catch (IOException e) {
            System.out.println("  [Client] ❌ Connection error: " + e.getMessage());
            System.out.println("  [Client] Is the server running? Start it via Menu Option 10 → 1.");
        }
    }

    // -----------------------------------------------------------------------
    // Headless / programmatic single-command query
    // -----------------------------------------------------------------------

    /**
     * Sends a single command to the server, collects the response lines into a
     * String and returns them.  Useful for automated / test usage.
     *
     * @param command the protocol command to send
     * @return the full server response as a newline-separated string
     * @throws IOException on any socket or I/O error
     */
    public String sendCommand(String command) throws IOException {
        try (
            Socket       socket = new Socket(host, port);
            BufferedReader in   = new BufferedReader(
                                      new InputStreamReader(socket.getInputStream()));
            PrintWriter   out   = new PrintWriter(socket.getOutputStream(), true)
        ) {
            socket.setSoTimeout(10_000);

            // Consume welcome banner
            String line;
            while ((line = in.readLine()) != null
                    && !line.equals(MonitoringProtocol.RESP_END)) {
                // skip welcome lines
            }

            // Send the command
            out.println(command);

            // Collect response
            StringBuilder response = new StringBuilder();
            while ((line = in.readLine()) != null) {
                if (line.equals(MonitoringProtocol.RESP_END)) break;
                response.append(line).append("\n");
            }

            // Close session
            out.println(MonitoringProtocol.CMD_QUIT);
            return response.toString().trim();
        }
    }

    // -----------------------------------------------------------------------
    // Helper: print server lines until END marker
    // -----------------------------------------------------------------------
    private void printServerResponse(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals(MonitoringProtocol.RESP_END)) break;
            System.out.println("  " + line);
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getHost() { return host; }
    public int    getPort() { return port; }
}
