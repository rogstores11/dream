package com.dream.torrent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores and serializes metadata for the torrent-like transfer flow.
 */
public class TorrentMetadata {
    private String fileName;
    private long fileSize;
    private int chunkSize;
    private int totalChunks;
    private List<String> chunkHashes = new ArrayList<>();
    private String trackerUrl;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public List<String> getChunkHashes() {
        return chunkHashes;
    }

    public void setChunkHashes(List<String> chunkHashes) {
        this.chunkHashes = chunkHashes;
    }

    public String getTrackerUrl() {
        return trackerUrl;
    }

    public void setTrackerUrl(String trackerUrl) {
        this.trackerUrl = trackerUrl;
    }

    /**
     * Save metadata as JSON.
     */
    public void saveToFile(Path metadataPath) throws IOException {
        Files.createDirectories(metadataPath.getParent());
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), this);
    }

    /**
     * Load metadata from JSON file.
     */
    public static TorrentMetadata loadFromFile(Path metadataPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(metadataPath.toFile(), TorrentMetadata.class);
    }

    /**
     * Basic validation check.
     */
    public void validate() {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is missing");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize cannot be negative");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks must be positive");
        }
        if (chunkHashes == null || chunkHashes.size() != totalChunks) {
            throw new IllegalArgumentException("chunkHashes size must match totalChunks");
        }
        if (trackerUrl == null || trackerUrl.isBlank()) {
            throw new IllegalArgumentException("trackerUrl is missing");
        }
    }

    /**
     * Simple deterministic overall file hash from metadata core fields.
     */
    @JsonIgnore
    public String getFileHash() {
        String base = fileName + "|" + fileSize + "|" + chunkSize + "|" + String.join("", chunkHashes);
        return new ChunkHasher().hashBytes(base.getBytes());
    }

    public static TorrentMetadata from(Path inputFile,
                                       int chunkSize,
                                       List<String> chunkHashes,
                                       String trackerUrl) throws IOException {
        TorrentMetadata metadata = new TorrentMetadata();
        metadata.setFileName(inputFile.getFileName().toString());
        metadata.setFileSize(Files.size(inputFile));
        metadata.setChunkSize(chunkSize);
        metadata.setTotalChunks(chunkHashes.size());
        metadata.setChunkHashes(new ArrayList<>(chunkHashes));
        metadata.setTrackerUrl(trackerUrl);
        metadata.validate();
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TorrentMetadata that)) return false;
        return fileSize == that.fileSize && chunkSize == that.chunkSize && totalChunks == that.totalChunks && Objects.equals(fileName, that.fileName) && Objects.equals(chunkHashes, that.chunkHashes) && Objects.equals(trackerUrl, that.trackerUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, fileSize, chunkSize, totalChunks, chunkHashes, trackerUrl);
    }
}
