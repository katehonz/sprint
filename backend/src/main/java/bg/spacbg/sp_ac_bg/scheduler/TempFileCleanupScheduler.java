package bg.spacbg.sp_ac_bg.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

@Component
public class TempFileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanupScheduler.class);

    @Value("${upload.temp-dir:/tmp/invoice-uploads}")
    private String tempDir;

    @Value("${upload.max-storage-bytes:3221225472}") // 3GB default
    private long maxStorageBytes;

    @Value("${upload.file-max-age-hours:24}") // Delete files older than 24 hours
    private int maxAgeHours;

    /**
     * Run cleanup every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupTempFiles() {
        log.info("Starting scheduled temp file cleanup");

        Path dir = Paths.get(tempDir);
        if (!Files.exists(dir)) {
            log.debug("Temp directory does not exist, skipping cleanup");
            return;
        }

        try {
            CleanupStats stats = performCleanup(dir);
            if (stats.deletedCount > 0) {
                log.info("Cleanup complete: deleted {} files, freed {} bytes",
                        stats.deletedCount, formatSize(stats.freedBytes));
            } else {
                log.debug("Cleanup complete: no files to delete");
            }
        } catch (IOException e) {
            log.error("Error during temp file cleanup", e);
        }
    }

    private CleanupStats performCleanup(Path dir) throws IOException {
        CleanupStats stats = new CleanupStats();
        Instant cutoff = Instant.now().minus(Duration.ofHours(maxAgeHours));

        // First pass: delete old files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    Instant lastModified = attrs.lastModifiedTime().toInstant();

                    if (lastModified.isBefore(cutoff)) {
                        long size = attrs.size();
                        Files.delete(file);
                        stats.deletedCount++;
                        stats.freedBytes += size;
                        log.debug("Deleted old temp file: {} (age: {} hours)",
                                file.getFileName(),
                                Duration.between(lastModified, Instant.now()).toHours());
                    }
                } catch (IOException e) {
                    log.warn("Failed to process file: {}", file, e);
                }
            }
        }

        // Second pass: if still over limit, delete oldest files
        long currentSize = calculateDirectorySize(dir);
        if (currentSize > maxStorageBytes) {
            log.warn("Directory still over size limit ({} > {}), deleting oldest files",
                    formatSize(currentSize), formatSize(maxStorageBytes));

            long targetSize = (long) (maxStorageBytes * 0.8); // Target 80% of limit

            try (var stream = Files.list(dir)) {
                var files = stream
                    .filter(Files::isRegularFile)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

                for (Path file : files) {
                    if (currentSize <= targetSize) {
                        break;
                    }

                    try {
                        long size = Files.size(file);
                        Files.delete(file);
                        currentSize -= size;
                        stats.deletedCount++;
                        stats.freedBytes += size;
                        log.info("Deleted file to free space: {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("Failed to delete file: {}", file, e);
                    }
                }
            }
        }

        return stats;
    }

    private long calculateDirectorySize(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }

        try (var stream = Files.walk(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static class CleanupStats {
        int deletedCount = 0;
        long freedBytes = 0;
    }
}
