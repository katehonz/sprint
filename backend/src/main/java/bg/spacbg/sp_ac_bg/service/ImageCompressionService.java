package bg.spacbg.sp_ac_bg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    private static final Logger log = LoggerFactory.getLogger(ImageCompressionService.class);

    // Azure Document Intelligence limits
    private static final long MAX_IMAGE_SIZE = 4 * 1024 * 1024; // 4MB for images
    private static final long MAX_PDF_SIZE = 4 * 1024 * 1024; // 4MB - PDFs with scanned images also have this limit
    private static final int MAX_DIMENSION = 4096; // Max width/height
    private static final int PDF_RENDER_DPI = 150; // DPI for PDF rendering during compression

    @Value("${upload.temp-dir:/tmp/invoice-uploads}")
    private String tempDir;

    @Value("${upload.max-storage-bytes:3221225472}") // 3GB default
    private long maxStorageBytes;

    /**
     * Compress image or PDF if needed to fit Azure Document Intelligence limits.
     * Returns the compressed file path or null if no compression needed.
     */
    public Path compressIfNeeded(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        // Handle PDFs
        if ("application/pdf".equals(contentType)) {
            if (file.getSize() <= MAX_PDF_SIZE) {
                log.info("PDF {} is within size limit ({} bytes), no compression needed",
                        originalFilename, file.getSize());
                return null;
            }
            log.info("PDF {} exceeds size limit ({} bytes), compressing...",
                    originalFilename, file.getSize());
            return compressPdf(file);
        }

        // Only process images
        if (contentType == null || !contentType.startsWith("image/")) {
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException(
                    "Файлът е твърде голям (" + formatSize(file.getSize()) + "). " +
                    "Максимален размер: " + formatSize(MAX_IMAGE_SIZE) + ". " +
                    "Моля, компресирайте файла или го разделете на части."
                );
            }
            return null;
        }

        // If image is small enough, no compression needed
        if (file.getSize() <= MAX_IMAGE_SIZE) {
            log.info("Image {} is within size limit ({} bytes), no compression needed",
                    originalFilename, file.getSize());
            return null;
        }

        log.info("Image {} exceeds size limit ({} bytes), compressing...",
                originalFilename, file.getSize());

        // Ensure temp directory exists and has space
        ensureTempDirectorySpace();

        // Read the image
        BufferedImage originalImage;
        try (InputStream is = file.getInputStream()) {
            originalImage = ImageIO.read(is);
        }

        if (originalImage == null) {
            throw new IllegalArgumentException("Невалиден формат на изображението: " + originalFilename);
        }

        // Resize if dimensions are too large
        BufferedImage resizedImage = resizeIfNeeded(originalImage);

        // Try progressively lower quality until we fit the size limit
        Path tempFile = createTempFile(originalFilename);
        float quality = 0.85f;

        while (quality >= 0.1f) {
            compressToFile(resizedImage, tempFile, quality);
            long compressedSize = Files.size(tempFile);

            if (compressedSize <= MAX_IMAGE_SIZE) {
                log.info("Compressed {} from {} to {} (quality: {})",
                        originalFilename, formatSize(file.getSize()),
                        formatSize(compressedSize), quality);
                return tempFile;
            }

            quality -= 0.1f;
        }

        // If still too large after compression, try more aggressive resize
        BufferedImage smallerImage = resizeToFit(resizedImage, 2048);
        compressToFile(smallerImage, tempFile, 0.7f);

        long finalSize = Files.size(tempFile);
        if (finalSize > MAX_IMAGE_SIZE) {
            Files.deleteIfExists(tempFile);
            throw new IllegalArgumentException(
                "Изображението не може да бъде компресирано достатъчно. " +
                "Моля, използвайте изображение с по-малка резолюция или PDF формат."
            );
        }

        log.info("Aggressively compressed {} from {} to {}",
                originalFilename, formatSize(file.getSize()), formatSize(finalSize));
        return tempFile;
    }

    /**
     * Compress PDF by re-rendering pages as compressed JPEG images.
     */
    private Path compressPdf(MultipartFile file) throws IOException {
        ensureTempDirectorySpace();

        Path tempFile = createTempFile(file.getOriginalFilename(), ".pdf");
        float quality = 0.75f;
        int dpi = PDF_RENDER_DPI;

        // Save uploaded file temporarily for PDFBox to load
        Path inputFile = Files.createTempFile(Paths.get(tempDir), "input_", ".pdf");
        try {
            file.transferTo(inputFile.toFile());

            while (quality >= 0.3f && dpi >= 72) {
                try (PDDocument sourceDoc = Loader.loadPDF(inputFile.toFile());
                     PDDocument targetDoc = new PDDocument()) {

                    PDFRenderer renderer = new PDFRenderer(sourceDoc);

                    for (int page = 0; page < sourceDoc.getNumberOfPages(); page++) {
                        BufferedImage image = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);

                        // Create new page with same dimensions
                        PDRectangle originalSize = sourceDoc.getPage(page).getMediaBox();
                        PDPage newPage = new PDPage(originalSize);
                        targetDoc.addPage(newPage);

                        // Add compressed image to page
                        PDImageXObject pdImage = JPEGFactory.createFromImage(targetDoc, image, quality);
                        try (PDPageContentStream contentStream = new PDPageContentStream(targetDoc, newPage)) {
                            contentStream.drawImage(pdImage, 0, 0, originalSize.getWidth(), originalSize.getHeight());
                        }
                    }

                    targetDoc.save(tempFile.toFile());
                }

                long compressedSize = Files.size(tempFile);
                if (compressedSize <= MAX_PDF_SIZE) {
                    log.info("Compressed PDF from {} to {} (quality: {}, dpi: {})",
                            formatSize(file.getSize()), formatSize(compressedSize), quality, dpi);
                    return tempFile;
                }

                // Try lower quality/DPI
                if (quality > 0.4f) {
                    quality -= 0.15f;
                } else {
                    dpi -= 30;
                    quality = 0.6f;
                }
            }

            // Final attempt with minimum settings
            long finalSize = Files.size(tempFile);
            if (finalSize > MAX_PDF_SIZE) {
                Files.deleteIfExists(tempFile);
                throw new IllegalArgumentException(
                    "PDF файлът не може да бъде компресиран достатъчно (" + formatSize(finalSize) + "). " +
                    "Максимален размер: " + formatSize(MAX_PDF_SIZE) + ". " +
                    "Моля, използвайте PDF с по-малко страници или по-ниска резолюция."
                );
            }

            return tempFile;
        } finally {
            Files.deleteIfExists(inputFile);
        }
    }

    private BufferedImage resizeIfNeeded(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return image;
        }

        return resizeToFit(image, MAX_DIMENSION);
    }

    private BufferedImage resizeToFit(BufferedImage image, int maxDimension) {
        int width = image.getWidth();
        int height = image.getHeight();

        double scale = Math.min(
            (double) maxDimension / width,
            (double) maxDimension / height
        );

        if (scale >= 1.0) {
            return image;
        }

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        log.debug("Resized image from {}x{} to {}x{}", width, height, newWidth, newHeight);
        return resized;
    }

    private void compressToFile(BufferedImage image, Path outputPath, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (OutputStream os = Files.newOutputStream(outputPath);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private Path createTempFile(String originalFilename) throws IOException {
        return createTempFile(originalFilename, null);
    }

    private Path createTempFile(String originalFilename, String forceExtension) throws IOException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        String extension;
        if (forceExtension != null) {
            extension = forceExtension;
        } else {
            String baseName = originalFilename != null ?
                originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "upload";
            extension = baseName.contains(".") ?
                baseName.substring(baseName.lastIndexOf('.')) : ".jpg";
        }

        return Files.createTempFile(dir, "compressed_", extension);
    }

    /**
     * Ensure temp directory exists and has enough space.
     * Cleans up old files if storage limit is exceeded.
     */
    public void ensureTempDirectorySpace() throws IOException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        long currentSize = calculateDirectorySize(dir);

        if (currentSize > maxStorageBytes) {
            log.warn("Temp directory size ({}) exceeds limit ({}), cleaning up...",
                    formatSize(currentSize), formatSize(maxStorageBytes));
            cleanupOldFiles(dir, maxStorageBytes / 2); // Clean down to 50% of limit
        }
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

    private void cleanupOldFiles(Path dir, long targetSize) throws IOException {
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

            long currentSize = calculateDirectorySize(dir);

            for (Path file : files) {
                if (currentSize <= targetSize) {
                    break;
                }

                try {
                    long fileSize = Files.size(file);
                    Files.delete(file);
                    currentSize -= fileSize;
                    log.info("Deleted old temp file: {}", file.getFileName());
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", file, e);
                }
            }
        }
    }

    /**
     * Delete a temporary file if it exists.
     */
    public void deleteIfExists(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }

    /**
     * Get current temp directory size.
     */
    public long getTempDirectorySize() {
        try {
            return calculateDirectorySize(Paths.get(tempDir));
        } catch (IOException e) {
            return -1;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
