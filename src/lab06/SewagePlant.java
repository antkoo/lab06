package lab06;

import java.io.*;
import java.net.*;
import java.util.*;

public class SewagePlant extends GUIInterface implements ISewagePlant {
    private int port;
    private Map<Integer, Integer> tankerSewageMap = new HashMap<>();

    public SewagePlant(int port) {
        super("Sewage Plant", port, "", 0, "", 0);  // No remote connections needed
        this.port = port;
    }

    public void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Sewage Plant running on port: " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String request = in.readLine();

                    if (request.startsWith("spi:")) {
                        String[] params = request.substring(4).split(",");
                        int tankerId = Integer.parseInt(params[0]);
                        int volume = Integer.parseInt(params[1]);
                        setPumpIn(tankerId, volume);

                    } else if (request.startsWith("gs:")) {
                        int tankerId = Integer.parseInt(request.substring(3));
                        int status = getStatus(tankerId);
                        out.println(status);

                    } else if (request.startsWith("spo:")) {
                        int tankerId = Integer.parseInt(request.substring(4));
                        setPayoff(tankerId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPumpIn(int tankerId, int volume) {
        tankerSewageMap.put(tankerId, tankerSewageMap.getOrDefault(tankerId, 0) + volume);
        log("Tanker #" + tankerId + " delivered " + volume + " units of sewage.");
    }

    @Override
    public int getStatus(int tankerId) {
        if (!tankerSewageMap.containsKey(tankerId)) {
            log("ERROR: Tanker #" + tankerId + " does not exist. Rejecting status update.");
            return 0;
        }
        return tankerSewageMap.get(tankerId);
    }

    @Override
    public void setPayoff(int tankerId) {
        if (!tankerSewageMap.containsKey(tankerId)) {
            log("ERROR: Tanker #" + tankerId + " does not exist. Rejecting payoff.");
            return;
        }
        tankerSewageMap.put(tankerId, 0);
        log("Settled account for Tanker #" + tankerId + ". Sewage level reset to 0.");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            log("Usage: java SewagePlant <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        new Thread(new SewagePlant(port)::runServer).start();
    }
}
