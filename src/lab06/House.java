package lab06;

import java.io.*;
import java.net.*;

public class House extends GUIInterface implements IHouse {
    private int port;
    private int tankCapacity;
    private int currentWaste;
    private String officeHost;
    private int officePort;
    private float warning_level = 0.5f;


    public House(int port, int tankCapacity, String officeHost, int officePort) {
        super("House", port, officeHost, officePort, "", 0);
        if (!isValidHost(officeHost)) {
            throw new IllegalArgumentException("Invalid IP address format: " + officeHost);
        }

        this.port = port;
        this.tankCapacity = tankCapacity;
        this.currentWaste = 0;
        this.officeHost = officeHost;
        this.officePort = officePort;
        new Thread(this::simulateSepticFilling, "SepticFillingThread").start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("House running on port: " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String request = in.readLine();

                    if (request.startsWith("gp:")) {
                        int max = Integer.parseInt(request.substring(3));
                        if (Math.min(max, currentWaste) < tankCapacity*warning_level) {
                            log("Septic tank too low to empty.");
                            out.println(0);
                        } else {
                            int pumpedOut = getPumpOut(max);
                            log("Tanker requested to pump " + max + " units. Pumped out: " + pumpedOut);
                            out.println(pumpedOut);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public int getPumpOut(int max) {
        int pumpedOut = Math.min(max, currentWaste);
        currentWaste -= pumpedOut;
        return pumpedOut;
    }

    private synchronized void simulateSepticFilling() {
        while (true) {
            try {
                Thread.sleep(15000);
                if (currentWaste < tankCapacity) {
                    currentWaste += 10;
                    if (currentWaste > tankCapacity) {
                        currentWaste = tankCapacity;
                    }
                    log("Septic tank filling: " + currentWaste + "/" + tankCapacity);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            log("Usage: java House <port> <tankCapacity> <officeHost> <officePort>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int capacity = Integer.parseInt(args[1]);
        String officeHost = args[2];
        int officePort = Integer.parseInt(args[3]);

        new Thread(new House(port, capacity, officeHost, officePort)::runServer).start();
    }
}
