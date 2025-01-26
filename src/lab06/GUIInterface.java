package lab06;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GUIInterface extends JFrame {
    private static JTextArea logArea;
    private JTextField commandField;
    private JButton sendButton;
    private int port;
    private String componentName;
    private String remoteHost;
    private int remotePort;
    private String secondaryHost;
    private int secondaryPort;

    protected static final String HOST_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";

    public GUIInterface(String componentName, int port, String remoteHost, int remotePort, String secondaryHost, int secondaryPort) {
        this.componentName = componentName;
        this.port = port;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.secondaryHost = secondaryHost;
        this.secondaryPort = secondaryPort;
        setupUI();
    }

    private void setupUI() {
        setTitle(componentName + " - Port " + port);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        commandField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(commandField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> {
            executeCommand(commandField.getText());
            commandField.setText("");
        });

        setVisible(true);
    }

    private void executeCommand(String command) {
        log("Executing: " + command);
        try {
            String targetHost = null;
            int targetPort = -1;

            if (command.matches("^gp:\\d+$")) {
                targetHost = "localhost";
                targetPort = port;

            } else if (command.matches("^gs:\\d+$")) {
                targetHost = secondaryHost;
                targetPort = secondaryPort;

            } else if (command.matches("^sr:\\d+$")) {
                targetHost = remoteHost;
                targetPort = remotePort;

            } else if (command.matches("^sj:\\d+\\.\\d+\\.\\d+\\.\\d+,\\d+$")) {
                targetHost = "localhost";
                targetPort = port;

            } else if (command.matches("^spi:\\d+,\\d+$")) {
                targetHost = "localhost";
                targetPort = port;

            } else if (command.matches("^spo:\\d+$")) {
                targetHost = secondaryHost;
                targetPort = secondaryPort;

            } else if (command.matches("^r:\\d+\\.\\d+\\.\\d+\\.\\d+,\\d+$")) {
                String[] params = command.substring(2).split(",");
                if (Integer.parseInt(params[1]) != port) {
                    log("ERROR: Wrong port");
                    return;
                }
                targetHost = remoteHost;
                targetPort = remotePort;

            } else if (command.matches("^o:\\d+\\.\\d+\\.\\d+\\.\\d+,\\d+$")) {
                String[] params = command.substring(2).split(",");
                if (Integer.parseInt(params[1]) != port) {
                    log("ERROR: Wrong port");
                    return;
                }
                targetHost = remoteHost;
                targetPort = remotePort;
            }

            if (targetHost != null && targetPort != -1) {
                String response = sendCommand(targetHost, targetPort, command);
                if (response!=null) {
                    log("Response: " + response);
                }
            } else {
                log("Error: Invalid command format.");
            }
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    private String sendCommand(String host, int port, String command) {
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(command);
            return in.readLine();
        } catch (IOException e) {
            log("Connection error: " + e.getMessage());
            return "";
        }
    }

    protected boolean isValidHost(String host) {
        Pattern pattern = Pattern.compile(HOST_REGEX);
        Matcher matcher = pattern.matcher(host);
        if (!matcher.matches()) return false;

        for (int i = 1; i <= 4; i++) {
            int value = Integer.parseInt(matcher.group(i));
            if (value < 0 || value > 255) return false;
        }
        return true;
    }

    protected static void log(String message) {
        logArea.append(message + "\n");
    }
}
