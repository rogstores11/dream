package com.dream.torrent;

import com.dream.torrent.TrackerServer.PeerInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Beginner-friendly end-to-end demo:
 * File -> Split -> Hash -> Metadata -> Transfer -> Merge
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Path workingDir = Path.of("torrent-demo");
        Files.createDirectories(workingDir);

        Path inputFile;
        if (args.length > 0) {
            inputFile = Path.of(args[0]);
        } else {
            inputFile = workingDir.resolve("sample.txt");
            Files.writeString(inputFile,
                    "This is a beginner torrent demo file.\n".repeat(20000),
                    StandardCharsets.UTF_8);
        }

        String trackerUrl = "http://127.0.0.1:8085";
        int chunkSize = FileSplitter.DEFAULT_CHUNK_SIZE;

        // Step 1: Split
        FileSplitter splitter = new FileSplitter();
        List<Path> seederChunks = splitter.splitFile(inputFile, workingDir.resolve("seeder"), chunkSize);

        // Step 2: Hash
        ChunkHasher hasher = new ChunkHasher();
        List<String> chunkHashes = hasher.hashChunks(seederChunks);
        System.out.println("[Main] First chunk hash: " + chunkHashes.get(0));

        // Step 3: Metadata
        TorrentMetadata metadata = TorrentMetadata.from(inputFile, chunkSize, chunkHashes, trackerUrl);
        Path metadataPath = workingDir.resolve("metadata.json");
        metadata.saveToFile(metadataPath);
        System.out.println("[Main] Metadata saved: " + metadataPath);

        String fileHash = metadata.getFileHash();

        // Step 5: Start tracker server
        TrackerServer trackerServer = new TrackerServer(8085);
        trackerServer.start();

        // Step 6: Start seeder peer and register
        PeerClient seederPeer = new PeerClient(trackerUrl, "127.0.0.1", 9091);
        seederPeer.startChunkServer(fileHash, workingDir.resolve("seeder/chunks"));
        Thread.sleep(400);
        seederPeer.register(fileHash);

        // Leecher peer discovers seeder
        PeerClient leecherPeer = new PeerClient(trackerUrl, "127.0.0.1", 9092);
        List<PeerInfo> peers = leecherPeer.discoverPeers(fileHash);

        // Step 7: Download chunks
        ChunkDownloader downloader = new ChunkDownloader(leecherPeer);
        Path downloadedChunks = workingDir.resolve("leecher/chunks");
        downloader.downloadAllChunks(fileHash, metadata, peers, downloadedChunks, 3);

        // Step 4 (after download): Merge
        FileMerger merger = new FileMerger();
        Path mergedFile = workingDir.resolve("leecher/merged_" + metadata.getFileName());
        merger.mergeChunks(downloadedChunks, mergedFile, metadata);

        String originalFileHash = hasher.hashFile(inputFile);
        String mergedFileHash = hasher.hashFile(mergedFile);
        System.out.println("[Main] Original file hash: " + originalFileHash);
        System.out.println("[Main] Merged file hash:   " + mergedFileHash);
        System.out.println("[Main] Transfer success: " + originalFileHash.equals(mergedFileHash));

        leecherPeer.shutdown();
        seederPeer.shutdown();
        trackerServer.stop();
    }
}
