package com.dream.torrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TorrentModulesTest {

    @TempDir
    Path tempDir;

    @Test
    void step1to4_splitHashMetadataAndMerge_shouldWork() throws Exception {
        Path input = tempDir.resolve("input.txt");
        Files.writeString(input, "hello world ".repeat(20000), StandardCharsets.UTF_8);

        FileSplitter splitter = new FileSplitter();
        List<Path> chunks = splitter.splitFile(input, tempDir, 16 * 1024);
        assertTrue(chunks.size() > 1);

        ChunkHasher hasher = new ChunkHasher();
        List<String> hashes = hasher.hashChunks(chunks);
        assertEquals(chunks.size(), hashes.size());

        TorrentMetadata metadata = TorrentMetadata.from(input, 16 * 1024, hashes, "http://127.0.0.1:18085");
        Path metadataPath = tempDir.resolve("metadata.json");
        metadata.saveToFile(metadataPath);

        TorrentMetadata loaded = TorrentMetadata.loadFromFile(metadataPath);
        loaded.validate();
        assertEquals(metadata, loaded);

        FileMerger merger = new FileMerger();
        Path merged = tempDir.resolve("merged.txt");
        merger.mergeChunks(tempDir.resolve("chunks"), merged, loaded);

        assertEquals(hasher.hashFile(input), hasher.hashFile(merged));
    }

    @Test
    void step5_trackerRegisterAndLookup_shouldReturnPeer() throws Exception {
        TrackerServer tracker = new TrackerServer(18085);
        tracker.start();

        try {
            PeerClient peerClient = new PeerClient("http://127.0.0.1:18085", "127.0.0.1", 19091);
            String fileHash = "demo-file-hash";

            peerClient.register(fileHash);
            List<TrackerServer.PeerInfo> peers = peerClient.discoverPeers(fileHash);

            assertFalse(peers.isEmpty());
            assertEquals("127.0.0.1", peers.get(0).peerIP);
            assertEquals(19091, peers.get(0).port);
        } finally {
            tracker.stop();
        }
    }

    @Test
    void step6and7_peerTransfer_shouldDownloadChunk() throws Exception {
        Path seederRoot = tempDir.resolve("seeder");
        Path leecherRoot = tempDir.resolve("leecher");
        Files.createDirectories(seederRoot);
        Files.createDirectories(leecherRoot);

        Path input = tempDir.resolve("seed.txt");
        Files.writeString(input, "peer transfer ".repeat(10000), StandardCharsets.UTF_8);

        FileSplitter splitter = new FileSplitter();
        List<Path> chunks = splitter.splitFile(input, seederRoot, 8 * 1024);

        ChunkHasher hasher = new ChunkHasher();
        List<String> hashes = hasher.hashChunks(chunks);
        TorrentMetadata metadata = TorrentMetadata.from(input, 8 * 1024, hashes, "http://127.0.0.1:18086");
        String fileHash = metadata.getFileHash();

        TrackerServer tracker = new TrackerServer(18086);
        tracker.start();

        PeerClient seeder = new PeerClient("http://127.0.0.1:18086", "127.0.0.1", 19092);
        PeerClient leecher = new PeerClient("http://127.0.0.1:18086", "127.0.0.1", 19093);

        try {
            seeder.startChunkServer(fileHash, seederRoot.resolve("chunks"));
            Thread.sleep(250);
            seeder.register(fileHash);

            List<TrackerServer.PeerInfo> peers = leecher.discoverPeers(fileHash);
            ChunkDownloader downloader = new ChunkDownloader(leecher);
            Path downloadedChunks = leecherRoot.resolve("chunks");
            downloader.downloadAllChunks(fileHash, metadata, peers, downloadedChunks, 2);

            FileMerger merger = new FileMerger();
            Path merged = leecherRoot.resolve("merged.txt");
            merger.mergeChunks(downloadedChunks, merged, metadata);

            assertEquals(hasher.hashFile(input), hasher.hashFile(merged));
        } finally {
            seeder.shutdown();
            leecher.shutdown();
            tracker.stop();
        }
    }
}
