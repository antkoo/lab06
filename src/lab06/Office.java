package lab06;

import java.io.*;
import java.net.*;
import java.util.*;

public class Office extends GUIInterface implements IOffice {
    private int port;
    private String sewagePlantHost;
    private int sewagePlantPort;

    private Map<Integer, String> tankerRegistry = new HashMap<>();
    private Map<Integer, Boolean> tankerAvailability = new HashMap<>();
    private Queue<String> houseOrderQueue = new LinkedList<>();
    private int tankerCounter = 1;

    public Office(int port, String sewagePlantHost, int sewagePlantPort) {
        super("Office", port, "", -1, sewagePlantHost, sewagePlantPort);
        if (!isValidHost(sewagePlantHost)) {
            throw new IllegalArgumentException("Invalid IP address format: " + sewagePlantHost);
        }
        this.port = port;
        this.sewagePlantHost = sewagePlantHost;
        this.sewagePlantPort = sewagePlantPort;
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Office running on port: " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String request = in.readLine();

                    if (request.startsWith("r:")) {
                        String[] params = request.substring(2).split(",");
                        int tankerId = register(params[0], params[1]);
                        out.println(tankerId);

                    } else if (request.startsWith("o:")) {
                        String[] params = request.substring(2).split(",");
                        int status = order(params[0], params[1]);
                        out.println(status);

                    } else if (request.startsWith("sr:")) {
                        int tankerId = Integer.parseInt(request.substring(3));
                        setReadyToServe(tankerId);

                    } else if (request.startsWith("sj:")) {
                        String[] params = request.substring(3).split(",");
                        assignTankerToHouse(params[0], Integer.parseInt(params[1]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int register(String host, String port) {
        int tankerId = tankerCounter++;
        tankerRegistry.put(tankerId, host + ":" + port);
        tankerAvailability.put(tankerId, false);
        log("Registered Tanker #" + tankerId + " at " + host + ":" + port);
        return tankerId;
    }

    @Override
    public int order(String host, String port) {
        if (tankerRegistry.isEmpty()) {
            log("ERROR: No Tankers registered. Rejecting House order.");
            return 0;
        }
        houseOrderQueue.add(host + "," + port);
        log("House request queued: " + host + ":" + port);
        return 1;
    }

    @Override
    public void setReadyToServe(int tankerId) {
        if (!tankerRegistry.containsKey(tankerId)) {
            log("ERROR: Tanker #" + tankerId + " is not registered.");
            return;
        }
        if (tankerAvailability.get(tankerId)) {
            log("ERROR: Tanker #" + tankerId + " is already ready to serve.");
            return;
        }
        tankerAvailability.put(tankerId, true);
        log("Tanker #" + tankerId + " is now ready to serve.");
    }

    private void assignTankerToHouse(String houseHost, int housePort) {
        int availableTankerId = -1;

        for (Map.Entry<Integer, Boolean> entry : tankerAvailability.entrySet()) {
            if (entry.getValue()) {
                availableTankerId = entry.getKey();
                break;
            }
        }

        if (availableTankerId == -1) {
            log("ERROR: No available Tankers to assign.");
            return;
        }

        if (!tankerRegistry.containsKey(availableTankerId)) {
            log("ERROR: Tanker #" + availableTankerId + " is not registered.");
            return;
        }

        String houseKey = houseHost + "," + housePort;
        if (!houseOrderQueue.contains(houseKey)) {
            log("ERROR: House " + houseKey + " never placed an order. Rejecting assignment.");
            return;
        }

        String[] tankerInfo = tankerRegistry.get(availableTankerId).split(":");
        String tankerHost = tankerInfo[0];
        int tankerPort = Integer.parseInt(tankerInfo[1]);
        try (Socket socket = new Socket(tankerHost, tankerPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("sj:" + houseHost + "," + housePort);
            houseOrderQueue.remove(houseKey);
            tankerAvailability.put(availableTankerId, false);
            log("Assigned Tanker #" + availableTankerId + " to House " + houseHost + ":" + housePort);
        } catch (IOException e) {
            log("Connection error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            log("Usage: java Office <port> <sewagePlantHost> <sewagePlantPort>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String sewagePlantHost = args[1];
        int sewagePlantPort = Integer.parseInt(args[2]);
        new Thread(new Office(port, sewagePlantHost, sewagePlantPort)::runServer).start();
    }
}
