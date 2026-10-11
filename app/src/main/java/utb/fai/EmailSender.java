package utb.fai;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EmailSender {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    /*
     * Constructor opens Socket to host/port. If the Socket throws an exception
     * during opening,
     * the exception is not handled in the constructor.
     */
    public EmailSender(String host, int port) throws UnknownHostException, IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        // read server welcome banner
        String greeting = readResponse();
        if (!greeting.startsWith("220")) {
            throw new IOException("SMTP server rejected connection: " + greeting);
        }

        // say hello
        sendHelo();
    }

    // Sends an email
    public void send(String from, String to, String subject, String text) throws IOException {
        // sender
        writer.write("MAIL FROM:<" + from + ">\r\n");
        writer.flush();
        String mailFromResp = readResponse();
        if (!mailFromResp.startsWith("250")) {
            throw new IOException("MAIL FROM command failed: " + mailFromResp);
        }

        // recipient
        writer.write("RCPT TO:<" + to + ">\r\n");
        writer.flush();
        String rcptToResp = readResponse();
        if (!rcptToResp.startsWith("250")) {
            throw new IOException("RCPT TO command failed: " + rcptToResp);
        }

        // data command
        writer.write("DATA\r\n");
        writer.flush();
        String dataResp = readResponse();
        if (!dataResp.startsWith("354")) {
            throw new IOException("DATA command failed: " + dataResp);
        }

        // headers and empty line before body
        writer.write("From: " + from + "\r\n");
        writer.write("To: " + to + "\r\n");
        writer.write("Subject: " + (subject != null ? subject : "") + "\r\n");
        writer.write("\r\n");

        // body lines, escape leading dot if needed
        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\r?\n", -1);
            for (String line : lines) {
                if (line.startsWith(".")) {
                    line = "." + line;
                }
                writer.write(line + "\r\n");
            }
        }

        // single dot on newline to finish message
        writer.write(".\r\n");
        writer.flush();

        String dataEndResp = readResponse();
        if (!dataEndResp.startsWith("250")) {
            throw new IOException("Data transmission failed: " + dataEndResp);
        }
    }

    // Sends QUIT and closes the socket
    public void close() {
        try {
            if (writer != null) {
                writer.write("QUIT\r\n");
                writer.flush();
                if (reader != null) {
                    readResponse();
                }
            }
        } catch (Exception ignored) {
            // ignore quit errors
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // helper to read server response (handles multiline with '-')
    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.length() >= 3) {
                // '-' after code means more lines coming
                if (line.length() == 3 || line.charAt(3) != '-') {
                    break;
                }
            }
        }
        return response.toString();
    }

    // try ehlo, if not supported fallback to helo
    private void sendHelo() throws IOException {
        String hostname = "localhost";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null && localHost.getHostName() != null && !localHost.getHostName().isEmpty()) {
                hostname = localHost.getHostName();
            }
        } catch (Exception ignored) {
        }

        writer.write("EHLO " + hostname + "\r\n");
        writer.flush();
        String response = readResponse();
        if (!response.startsWith("250")) {
            writer.write("HELO " + hostname + "\r\n");
            writer.flush();
            response = readResponse();
            if (!response.startsWith("250")) {
                throw new IOException("HELO/EHLO handshake failed: " + response);
            }
        }
    }
}
