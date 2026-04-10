package com.dream.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates SHA-256 hashes for chunk files.
 */
public class ChunkHasher {

    /**
     * Hash a list of chunk files and return hexadecimal hashes in order.
     */
    public List<String> hashChunks(List<Path> chunkPaths) throws IOException {
        List<String> hashes = new ArrayList<>();
        for (Path chunkPath : chunkPaths) {
            byte[] data = Files.readAllBytes(chunkPath);
            hashes.add(hashBytes(data));
        }
        return hashes;
    }

    /**
     * Hash any byte array using SHA-256.
     */
    public String hashBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Hash a file directly.
     */
    public String hashFile(Path filePath) throws IOException {
        return hashBytes(Files.readAllBytes(filePath));
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
