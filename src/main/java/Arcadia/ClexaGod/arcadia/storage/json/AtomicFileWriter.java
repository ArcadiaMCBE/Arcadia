package Arcadia.ClexaGod.arcadia.storage.json;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;

@UtilityClass
public class AtomicFileWriter {

    private static final int STRIPES = 256;
    private static final ReentrantLock[] JVM_LOCKS = new ReentrantLock[STRIPES];

    static {
        for (int i = 0; i < STRIPES; i++) {
            JVM_LOCKS[i] = new ReentrantLock();
        }
    }

    public static void write(Path path, String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path lockPath = path.resolveSibling(path.getFileName() + ".lock");
        ReentrantLock jvmLock = lockFor(lockPath);
        jvmLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
            try {
                Files.writeString(tempPath, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                try {
                    Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempPath);
            }
        } finally {
            jvmLock.unlock();
        }
    }

    private static ReentrantLock lockFor(Path path) {
        int index = Math.floorMod(path.toAbsolutePath().normalize().hashCode(), STRIPES);
        return JVM_LOCKS[index];
    }
}
