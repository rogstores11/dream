package com.dream.torrent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * In-memory tracker server that stores peers by file hash.
 */
public class TrackerServer {
    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, List<PeerInfo>> peerMap = new ConcurrentHashMap<>();

    public TrackerServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.createContext("/register", this::handleRegister);
        this.server.createContext("/peers", this::handlePeers);
    }

    public void start() {
        server.start();
        System.out.println("[TrackerServer] Started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("[TrackerServer] Stopped");
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }

        RegisterRequest req = mapper.readValue(exchange.getRequestBody(), RegisterRequest.class);
        if (req.fileHash == null || req.fileHash.isBlank() || req.peerIP == null || req.peerIP.isBlank() || req.port <= 0) {
            sendJson(exchange, 400, Map.of("error", "Invalid request"));
            return;
        }

        peerMap.putIfAbsent(req.fileHash, new ArrayList<>());
        synchronized (peerMap.get(req.fileHash)) {
            List<PeerInfo> peers = peerMap.get(req.fileHash);
            boolean exists = peers.stream().anyMatch(p -> p.peerIP.equals(req.peerIP) && p.port == req.port);
            if (!exists) {
                peers.add(new PeerInfo(req.peerIP, req.port));
            }
        }

        sendJson(exchange, 200, Map.of("status", "registered"));
    }

    private void handlePeers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        String fileHash = parseQueryParam(query, "fileHash");
        if (fileHash == null || fileHash.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "fileHash query param is required"));
            return;
        }

        List<PeerInfo> peers = peerMap.getOrDefault(fileHash, List.of());
        sendJson(exchange, 200, peers);
    }

    private String parseQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Request body for /register.
     */
    public static class RegisterRequest {
        public String fileHash;
        public String peerIP;
        public int port;

        public RegisterRequest() {
        }

        public RegisterRequest(String fileHash, String peerIP, int port) {
            this.fileHash = fileHash;
            this.peerIP = peerIP;
            this.port = port;
        }
    }

    /**
     * Peer information returned by /peers.
     */
    public static class PeerInfo {
        public String peerIP;
        public int port;

        public PeerInfo() {
        }

        public PeerInfo(String peerIP, int port) {
            this.peerIP = peerIP;
            this.port = port;
        }

        @Override
        public String toString() {
            return peerIP + ":" + port;
        }
    }
}
