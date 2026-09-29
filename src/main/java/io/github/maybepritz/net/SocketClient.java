package io.github.maybepritz.net;

import io.github.maybepritz.model.RawResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SockerClient {
    public SockerClient() {}

    public static RawResponse get(String host, int port, String path, int timeout) throws IOException {
        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            OutputStream out = socket.getOutputStream();
            String rawRequest = "GET " + path + "HTTP/1.0\r\n" +
                    "Host: " + host + "\r\n" +
                    "Accept: text/html\r\n" +
                    "Connection: close\r\n\r\n";

            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            InputStream in = new BufferedInputStream(socket.getInputStream());

            String statusLine = readLine(in);
            if(statusLine == null || statusLine.isEmpty()){
                throw new IOException("Пустой ответ от сервера");
            }

            String[] statusParts = statusLine.split(" ");
            int statusCode = statusParts.length >= 2 ? Integer.parseInt(statusParts[1]) : 0;

            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()){
                int colon = line.indexOf(':');

                if(colon != -1) {
                    headers.put(line.substring(0, colon).toLowerCase(),
                            line.substring(colon + 1).trim());
                }
            }

            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                bodyStream.write(buffer, 0, n);
            }

            return new RawResponse(statusCode, headers, bodyStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        int b;
        while((b = in.read()) != -1) {
            if(b == '\n') break;
            if(b != '\r') buff.write(b);
        }

        if (b == - 1 || buff.size() == 0) return null;
        return buff.toString();
    }
}
