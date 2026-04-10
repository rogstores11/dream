package com.dream.torrent;

import com.dream.torrent.TrackerServer.PeerInfo;
import com.dream.torrent.TrackerServer.RegisterRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Peer client that talks to tracker and other peers.
 */
public class PeerClient {
    private final String trackerUrl;
    private final String localIp;
    private final int localPort;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService peerServerPool = Executors.newCachedThreadPool();

    public PeerClient(String trackerUrl, String localIp, int localPort) {
        this.trackerUrl = trackerUrl;
        this.localIp = localIp;
        this.localPort = localPort;
    }

    public void register(String fileHash) throws IOException {
        URL url = new URL(trackerUrl + "/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        RegisterRequest request = new RegisterRequest(fileHash, localIp, localPort);
        byte[] body = mapper.writeValueAsBytes(request);

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.write(body);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Tracker registration failed with code " + code);
        }
        System.out.println("[PeerClient] Registered with tracker: " + localIp + ":" + localPort);
    }

    public List<PeerInfo> discoverPeers(String fileHash) throws IOException {
        String encodedHash = URLEncoder.encode(fileHash, StandardCharsets.UTF_8);
        URL url = new URL(trackerUrl + "/peers?fileHash=" + encodedHash);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Peer lookup failed with code " + conn.getResponseCode());
        }

        List<PeerInfo> peers = mapper.readValue(conn.getInputStream(), new TypeReference<>() {
        });
        System.out.println("[PeerClient] Peers found: " + peers);
        return peers;
    }

    /**
     * Start a TCP server that can send chunk files when requested.
     */
    public void startChunkServer(String fileHash, Path chunksDir) {
        peerServerPool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(localPort)) {
                System.out.println("[PeerClient] Chunk server listening on " + localPort);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    peerServerPool.submit(() -> handlePeerRequest(clientSocket, fileHash, chunksDir));
                }
            } catch (IOException e) {
                System.err.println("[PeerClient] Chunk server stopped: " + e.getMessage());
            }
        });
    }

    private void handlePeerRequest(Socket socket, String expectedFileHash, Path chunksDir) {
        try (Socket ignored = socket;
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String command = dis.readUTF();
            String fileHash = dis.readUTF();
            int chunkIndex = dis.readInt();

            if (!"GET_CHUNK".equals(command) || !expectedFileHash.equals(fileHash)) {
                dos.writeBoolean(false);
                return;
            }

            Path chunkPath = chunksDir.resolve("chunk_" + chunkIndex + ".dat");
            if (!Files.exists(chunkPath)) {
                dos.writeBoolean(false);
                return;
            }

            byte[] chunkBytes = Files.readAllBytes(chunkPath);
            dos.writeBoolean(true);
            dos.writeInt(chunkBytes.length);
            dos.write(chunkBytes);
            dos.flush();
        } catch (IOException e) {
            System.err.println("[PeerClient] Error serving peer request: " + e.getMessage());
        }
    }

    /**
     * Request one chunk from a remote peer.
     */
    public boolean downloadChunkFromPeer(PeerInfo peer, String fileHash, int chunkIndex, Path outputChunkFile) {
        try (Socket socket = new Socket(peer.peerIP, peer.port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("GET_CHUNK");
            dos.writeUTF(fileHash);
            dos.writeInt(chunkIndex);
            dos.flush();

            boolean found = dis.readBoolean();
            if (!found) {
                return false;
            }

            int size = dis.readInt();
            byte[] data = dis.readNBytes(size);
            Files.createDirectories(outputChunkFile.getParent());
            Files.write(outputChunkFile, data);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void shutdown() {
        peerServerPool.shutdownNow();
    }
}
