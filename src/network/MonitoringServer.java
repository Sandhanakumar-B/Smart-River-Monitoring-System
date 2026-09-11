package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 9: Java Networking & Socket Programming — TCP Monitoring Server
 * Syllabus Unit: UNIT V - ServerSocket, Multi-Client Thread Pool, Socket Accept Loop
 *
 * MonitoringServer opens a TCP ServerSocket on DEFAULT_PORT and runs an accept loop
 * that hands each incoming connection to a dedicated ClientHandler thread pooled
 * via a fixed-thread ExecutorService.
 *
 * Key Java Networking concepts demonstrated:
 *   - java.net.ServerSocket     — server-side socket that listens for connections
 *   - java.net.Socket           — bidirectional endpoint for communication
 *   - InputStream / OutputStream via BufferedReader & PrintWriter for text protocol
 *   - ExecutorService (thread pool) instead of raw Thread creation for scalability
 *   - Graceful shutdown: closing ServerSocket breaks the blocking accept() call
 *   - AtomicInteger for thread-safe client ID counter
 */
public class MonitoringServer implements Runnable {

    private final RiverMonitoringService monitoringService;
    private final int                    port;

    private ServerSocket                 serverSocket;
    private ExecutorService              threadPool;
    private volatile boolean             running;
    private final AtomicInteger          clientCounter = new AtomicInteger(0);
    private Thread                       serverThread;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------
    public MonitoringServer(RiverMonitoringService monitoringService) {
        this(monitoringService, MonitoringProtocol.DEFAULT_PORT);
    }

    public MonitoringServer(RiverMonitoringService monitoringService, int port) {
        this.monitoringService = monitoringService;
        this.port              = port;
    }

    // -----------------------------------------------------------------------
    // Lifecycle control
    // -----------------------------------------------------------------------

    /**
     * Starts the TCP server in its own daemon thread.
     * Returns immediately; the server accept-loop runs in background.
     *
     * @throws IOException if the ServerSocket cannot bind to the given port
     */
    public synchronized void startServer() throws IOException {
        if (running) {
            System.out.println("  [Server] Already running on port " + port + ".");
            return;
        }

        serverSocket = new ServerSocket(port);
        threadPool   = Executors.newFixedThreadPool(10); // up to 10 simultaneous clients
        running      = true;

        serverThread = new Thread(this, "MonitoringServer-" + port);
        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("  [Server] ✅ TCP Monitoring Server started on port " + port);
        System.out.println("  [Server] Waiting for remote client connections...");
    }

    /**
     * Gracefully stops the server: closes the ServerSocket (breaking accept()),
     * shuts down the thread pool, and waits for active handlers to finish.
     */
    public synchronized void stopServer() {
        if (!running) {
            System.out.println("  [Server] Server is not currently running.");
            return;
        }

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // causes accept() to throw SocketException -> loop exits
            }
        } catch (IOException e) {
            System.err.println("  [Server] Error closing server socket: " + e.getMessage());
        }

        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("  [Server] 🔴 TCP Monitoring Server stopped.");
    }

    // -----------------------------------------------------------------------
    // Accept loop (Runnable)
    // -----------------------------------------------------------------------
    @Override
    public void run() {
        System.out.println("  [Server] Accept loop active on "
            + MonitoringProtocol.SERVER_HOST + ":" + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept(); // blocks until client connects
                int id = clientCounter.incrementAndGet();
                ClientHandler handler = new ClientHandler(clientSocket, monitoringService, id);
                threadPool.submit(handler); // hand off to thread pool — non-blocking
            } catch (SocketException e) {
                if (!running) {
                    // Normal shutdown — ServerSocket was deliberately closed
                    break;
                }
                System.err.println("  [Server] Socket error in accept loop: " + e.getMessage());
            } catch (IOException e) {
                if (running) {
                    System.err.println("  [Server] Accept error: " + e.getMessage());
                }
            }
        }

        System.out.println("  [Server] Accept loop exited.");
    }

    // -----------------------------------------------------------------------
    // Status accessors
    // -----------------------------------------------------------------------
    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public int getTotalClientsServed() {
        return clientCounter.get();
    }
}
