package com.dream.torrent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Merges chunk files back into original file after hash verification.
 */
public class FileMerger {

    /**
     * Merge chunks in numeric order and verify each chunk hash first.
     */
    public void mergeChunks(Path chunksDir, Path outputFile, TorrentMetadata metadata) throws IOException {
        Files.createDirectories(outputFile.getParent());
        ChunkHasher hasher = new ChunkHasher();

        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            for (int i = 0; i < metadata.getTotalChunks(); i++) {
                Path chunkPath = chunksDir.resolve("chunk_" + i + ".dat");
                if (!Files.exists(chunkPath)) {
                    throw new IOException("Missing chunk file: " + chunkPath);
                }

                byte[] chunkData = Files.readAllBytes(chunkPath);
                String actualHash = hasher.hashBytes(chunkData);
                String expectedHash = metadata.getChunkHashes().get(i);

                if (!actualHash.equals(expectedHash)) {
                    throw new IOException("Chunk hash mismatch for chunk_" + i + ".dat. Merge stopped.");
                }

                fos.write(chunkData);
            }
        }

        System.out.println("[FileMerger] File merged successfully: " + outputFile);
    }
}
