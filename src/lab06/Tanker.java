package lab06;

import java.io.*;
import java.net.*;

public class Tanker extends GUIInterface implements ITanker {
    private int port;
    private String officeHost;
    private int officePort;
    private String sewagePlantHost;
    private int sewagePlantPort;
    private int capacity;
    private int currentLoad = 0;
    private int requestedLoad;
    private String currentHouseHost;
    private int currentHousePort;

    public Tanker(int port, String officeHost, int officePort, String sewagePlantHost, int sewagePlantPort, int capacity) {
        super("Tanker", port, officeHost, officePort, sewagePlantHost, sewagePlantPort);
        if (!isValidHost(officeHost)) {
            throw new IllegalArgumentException("Invalid IP address format: " + officeHost);
        } else if (!isValidHost(sewagePlantHost)) {
            throw new IllegalArgumentException("Invalid IP address format: " + sewagePlantHost);
        }
        this.port = port;
        this.officeHost = officeHost;
        this.officePort = officePort;
        this.sewagePlantHost = sewagePlantHost;
        this.sewagePlantPort = sewagePlantPort;
        this.capacity = capacity;
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Tanker running on port: " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String request = in.readLine();

                    if (request.startsWith("sj:")) {
                        String[] params = request.substring(3).split(",");
                        setJob(params[0], params[1]);

                    } else if (request.startsWith("gp:")) {
                        requestedLoad = Math.min(Integer.parseInt(request.substring(3)),capacity);
                        pumpOutWaste(currentHouseHost,currentHousePort);

                    } else if (request.startsWith("spi:")) {
                        String[] params = request.substring(4).split(",");
                        transportToSewagePlant(Math.min(currentLoad, Integer.parseInt(params[1])), Integer.parseInt(params[0]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setJob(String houseHost, String housePort) {
        this.currentHouseHost = houseHost;
        this.currentHousePort = Integer.parseInt(housePort);
        log("Received job for House at " + currentHouseHost + ":" + currentHousePort);
    }

    private void pumpOutWaste(String houseHost, int housePort) {
        try (Socket socket = new Socket(houseHost, housePort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("gp:" + requestedLoad);
            int pumpedOut = Integer.parseInt(in.readLine());
            currentLoad = pumpedOut;
            log("Pumped out " + pumpedOut + " units from house at " + houseHost + ":" + housePort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void transportToSewagePlant(int volume, int tankerId) {
        try (Socket socket = new Socket(sewagePlantHost, sewagePlantPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("spi:" + tankerId + "," + volume);
            log("Transported " + volume + " units to Sewage Plant.");
            currentLoad = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 6) {
            log("Usage: java Tanker <port> <officeHost> <officePort> <sewagePlantHost> <sewagePlantPort> <capacity>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String officeHost = args[1];
        int officePort = Integer.parseInt(args[2]);
        String sewagePlantHost = args[3];
        int sewagePlantPort = Integer.parseInt(args[4]);
        int capacity = Integer.parseInt(args[5]);

        new Thread(new Tanker(port, officeHost, officePort, sewagePlantHost, sewagePlantPort, capacity)::runServer).start();
    }
}
