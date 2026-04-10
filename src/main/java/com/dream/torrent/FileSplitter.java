package com.dream.torrent;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a file into fixed-size chunk files.
 */
public class FileSplitter {
    public static final int DEFAULT_CHUNK_SIZE = 512 * 1024;

    /**
     * Split an input file into chunks and store them in outputDir/chunks.
     */
    public List<Path> splitFile(Path inputFile, Path outputDir, int chunkSize) throws IOException {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than zero");
        }

        Path chunksDir = outputDir.resolve("chunks");
        Files.createDirectories(chunksDir);

        List<Path> chunkPaths = new ArrayList<>();
        byte[] buffer = new byte[chunkSize];

        try (FileInputStream fis = new FileInputStream(inputFile.toFile())) {
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                Path chunkPath = chunksDir.resolve("chunk_" + chunkIndex + ".dat");
                try (FileOutputStream fos = new FileOutputStream(chunkPath.toFile())) {
                    fos.write(buffer, 0, bytesRead);
                }
                chunkPaths.add(chunkPath);
                chunkIndex++;
            }
        }

        System.out.println("[FileSplitter] Total chunks created: " + chunkPaths.size());
        return chunkPaths;
    }

    /**
     * Overload with default 512 KB chunk size.
     */
    public List<Path> splitFile(Path inputFile, Path outputDir) throws IOException {
        return splitFile(inputFile, outputDir, DEFAULT_CHUNK_SIZE);
    }
}
