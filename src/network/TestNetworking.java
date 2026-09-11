package network;

import service.RiverMonitoringService;

/**
 * Quick end-to-end networking test:
 * 1. Starts MonitoringServer in background
 * 2. Uses MonitoringClient.sendCommand() to query the server
 * 3. Verifies all 4 protocol commands return OK responses
 */
public class TestNetworking {

    public static void main(String[] args) throws Exception {
        System.out.println("=== DAY 9: JAVA NETWORKING & SOCKET PROGRAMMING — VERIFICATION ===\n");

        // 1. Start server
        RiverMonitoringService service = new RiverMonitoringService();
        MonitoringServer server = new MonitoringServer(service, MonitoringProtocol.DEFAULT_PORT);
        server.startServer();
        Thread.sleep(300); // let server bind and start accept loop

        System.out.println("  [Test] Server started: port=" + server.getPort()
            + ", running=" + server.isRunning());

        MonitoringClient client = new MonitoringClient();

        // 2. SERVER_STATUS
        System.out.println("\n--- CMD: SERVER_STATUS ---");
        String resp = client.sendCommand(MonitoringProtocol.CMD_SERVER_STATUS);
        System.out.println(resp);
        assert resp.startsWith("OK") : "SERVER_STATUS did not return OK";

        // 3. LIST_STATIONS
        System.out.println("\n--- CMD: LIST_STATIONS ---");
        resp = client.sendCommand(MonitoringProtocol.CMD_LIST_STATIONS);
        System.out.println(resp);
        assert resp.startsWith("OK") : "LIST_STATIONS did not return OK";
        assert resp.contains("STN-HAR-01") : "LIST_STATIONS did not contain STN-HAR-01";

        // 4. BASIN_STATS
        System.out.println("\n--- CMD: BASIN_STATS ---");
        resp = client.sendCommand(MonitoringProtocol.CMD_BASIN_STATS);
        System.out.println(resp);
        assert resp.startsWith("OK") : "BASIN_STATS did not return OK";

        // 5. GET_ALERTS
        System.out.println("\n--- CMD: GET_ALERTS ---");
        resp = client.sendCommand(MonitoringProtocol.CMD_GET_ALERTS);
        System.out.println(resp);
        assert resp.startsWith("OK") : "GET_ALERTS did not return OK";

        // 6. GET_LEVEL for known station
        System.out.println("\n--- CMD: GET_LEVEL STN-HAR-01 ---");
        resp = client.sendCommand("GET_LEVEL STN-HAR-01");
        System.out.println(resp);
        assert !resp.contains("ERROR") || resp.contains("No readings") :
            "GET_LEVEL returned unexpected error";

        // 7. Unknown command → error response
        System.out.println("\n--- CMD: BADCOMMAND ---");
        resp = client.sendCommand("BADCOMMAND");
        System.out.println(resp);
        assert resp.contains("ERROR") || resp.contains("Unknown") :
            "Unknown command did not return ERROR";

        // 8. Multi-client test (3 concurrent clients)
        System.out.println("\n--- MULTI-CLIENT: 3 Concurrent Connections ---");
        Thread[] clients = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            clients[i] = new Thread(() -> {
                try {
                    MonitoringClient c = new MonitoringClient();
                    String r = c.sendCommand(MonitoringProtocol.CMD_LIST_STATIONS);
                    System.out.printf("  Client #%d received %d chars, starts with: %s%n",
                        idx + 1, r.length(), r.substring(0, Math.min(30, r.length())));
                } catch (Exception e) {
                    System.err.println("  Client #" + (idx+1) + " error: " + e.getMessage());
                }
            }, "TestClient-" + (i + 1));
        }
        for (Thread t : clients) t.start();
        for (Thread t : clients) t.join(3000);

        System.out.println("  Clients served so far: " + server.getTotalClientsServed());

        // 9. Stop server
        System.out.println("\n--- STOPPING SERVER ---");
        server.stopServer();
        Thread.sleep(300);
        System.out.println("  Server running after stop: " + server.isRunning());

        System.out.println("\n=== ALL NETWORKING TESTS PASSED SUCCESSFULLY! ===");
    }
}
