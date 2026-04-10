package com.dream.torrent;

import com.dream.torrent.TrackerServer.PeerInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Downloads chunks in parallel with retries and hash validation.
 */
public class ChunkDownloader {
    private final PeerClient peerClient;

    public ChunkDownloader(PeerClient peerClient) {
        this.peerClient = peerClient;
    }

    public void downloadAllChunks(String fileHash,
                                  TorrentMetadata metadata,
                                  List<PeerInfo> peers,
                                  Path outputChunksDir,
                                  int maxRetries) throws Exception {

        Files.createDirectories(outputChunksDir);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, peers.size()));
        ChunkHasher hasher = new ChunkHasher();

        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < metadata.getTotalChunks(); i++) {
                int chunkIndex = i;
                tasks.add(() -> {
                    String expectedHash = metadata.getChunkHashes().get(chunkIndex);
                    Path output = outputChunksDir.resolve("chunk_" + chunkIndex + ".dat");

                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        for (PeerInfo peer : peers) {
                            boolean ok = peerClient.downloadChunkFromPeer(peer, fileHash, chunkIndex, output);
                            if (ok) {
                                byte[] data = Files.readAllBytes(output);
                                String actualHash = hasher.hashBytes(data);
                                if (actualHash.equals(expectedHash)) {
                                    System.out.println("[ChunkDownloader] Downloaded and verified chunk " + chunkIndex);
                                    return true;
                                }
                                Files.deleteIfExists(output);
                                System.out.println("[ChunkDownloader] Hash mismatch for chunk " + chunkIndex + " from " + peer);
                            }
                        }
                        System.out.println("[ChunkDownloader] Retry " + attempt + " for chunk " + chunkIndex);
                    }
                    return false;
                });
            }

            List<Future<Boolean>> results = pool.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).get()) {
                    throw new IOException("Failed to download chunk_" + i + ".dat after retries");
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
