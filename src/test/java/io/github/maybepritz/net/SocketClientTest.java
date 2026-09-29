package io.github.maybepritz.net;

import io.github.maybepritz.model.RawResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class SocketClientTest {
    private ServerSocket mockServer;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new ServerSocket(0);
        port = mockServer.getLocalPort();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null && !mockServer.isClosed()) {
            mockServer.close();
        }
    }
}
