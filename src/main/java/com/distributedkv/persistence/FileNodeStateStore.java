package com.distributedkv.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * A {@link NodeStateStore} that persists each node's durable state to a
 * dedicated JSON file on the local filesystem.
 *
 * <p>Writes are atomic: the state is first written to a temporary file in
 * the same directory and then moved into place. {@code ATOMIC_MOVE} is used
 * when the filesystem supports it, so a partially written file is never
 * exposed to a concurrent or subsequent {@link #load()}.</p>
 *
 * <p>If the stored file cannot be parsed, the corruption is never treated
 * as valid state: the offending file is moved aside and an empty result is
 * returned so the node starts from a clean slate.</p>
 */
public class FileNodeStateStore implements NodeStateStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;

    /**
     * Creates a store for the given node inside {@code directory}. The
     * directory is created lazily on the first save.
     *
     * @param directory the directory that holds the node state files
     * @param nodeId    the node identifier used to derive the file name
     */
    public FileNodeStateStore(Path directory, String nodeId) {
        this.file = directory.resolve(sanitize(nodeId) + ".json");
    }

    @Override
    public synchronized void save(RaftPersistentState state) {

        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        Path directory = file.getParent();

        try {
            if (directory != null) {
                Files.createDirectories(directory);
            }

            Path temporary = Files.createTempFile(
                    directory,
                    file.getFileName().toString(),
                    ".tmp"
            );

            try {
                MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValue(temporary.toFile(), state);

                moveIntoPlace(temporary);
            } catch (IOException e) {
                Files.deleteIfExists(temporary);
                throw e;
            }

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to persist Raft state for " + file, e);
        }
    }

    @Override
    public synchronized Optional<RaftPersistentState> load() {

        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            byte[] bytes = Files.readAllBytes(file);

            if (bytes.length == 0) {
                // An empty file carries no valid state.
                return Optional.empty();
            }

            RaftPersistentState state =
                    MAPPER.readValue(bytes, RaftPersistentState.class);

            return Optional.ofNullable(state);

        } catch (IOException | RuntimeException e) {

            // Corrupt or unreadable state must never be treated as valid.
            System.err.println(
                    "WARNING: corrupt Raft state file " + file
                            + " (" + e.getMessage() + "). Starting fresh."
            );

            quarantineCorruptFile();

            return Optional.empty();
        }
    }

    /** The file backing this store; exposed for diagnostics and tests. */
    public Path getFilePath() {
        return file;
    }

    private void moveIntoPlace(Path temporary) throws IOException {

        try {
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException e) {
            // Fall back to a plain move when atomic moves are unavailable.
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void quarantineCorruptFile() {

        Path quarantined = file.resolveSibling(file.getFileName() + ".corrupt");

        try {
            Files.move(
                    file,
                    quarantined,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException ignored) {
            // Best effort only; the node still starts from a clean slate.
        }
    }

    private static String sanitize(String nodeId) {
        return nodeId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
