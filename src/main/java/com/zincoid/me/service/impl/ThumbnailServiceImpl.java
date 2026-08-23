package com.zincoid.me.service.impl;

import com.zincoid.me.service.ThumbnailService;
import com.zincoid.me.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailServiceImpl implements ThumbnailService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.thumb-max-size:512}")
    private int maxSize;

    @Override
    public String toThumbUrl(String urlOrPath) {
        return FileUtil.toThumbUrl(urlOrPath);
    }

    @Override
    public Resource getThumbnail(String filename) {
        if (!isSafeName(filename)) return null;
        Path original = Paths.get(uploadPath, filename);
        if (!Files.isRegularFile(original)) return null;
        try {
            Path cache = Paths.get(
                    uploadPath,
                    FileUtil.CACHE_FOLDER,
                    FileUtil.toThumbName(filename)
            );
            if (!Files.isRegularFile(cache)) {
                if (isAnimatedGif(original)) {
                    log.debug("Thumbnail of gif, use original: {}", filename);
                    return new FileSystemResource(original);
                }
                if (!generate(original, cache)) {
                    log.warn("Thumbnail generation failed: {}", filename);
                    return new FileSystemResource(original);
                }
                log.info("Thumbnail generated: {}", cache.getFileName());
            }
            return new FileSystemResource(cache);
        } catch (Exception e) {
            log.warn("Thumbnail request failed: {}", filename, e);
            return new FileSystemResource(original);
        }
    }

    // ──────── Private tool ────────────────────────────────

    private boolean isSafeName(String filename) {
        return filename != null
                && !filename.isBlank()
                && !filename.contains("/")
                && !filename.contains("\\")
                && !filename.contains("..");
    }

    private boolean generate(Path original, Path cache) {
        try {
            BufferedImage image = ImageIO.read(original.toFile());
            if (image == null) return false;
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) return false;

            boolean toJpeg = FileUtil.isJpeg(FileUtil.getExt(original.getFileName().toString()));
            int targetWidth = width;
            int targetHeight = height;
            if (Math.max(width, height) > maxSize) {
                double scale = (double) maxSize / Math.max(width, height);
                targetWidth = Math.max(1, (int) Math.round(width * scale));
                targetHeight = Math.max(1, (int) Math.round(height * scale));
            }

            BufferedImage thumb = resize(image, targetWidth, targetHeight, toJpeg);
            Files.createDirectories(cache.getParent());
            return ImageIO.write(thumb, toJpeg ? "jpg" : "png", cache.toFile());
        } catch (IOException e) {
            log.warn("Failed to generate thumbnail: {}", cache, e);
            return false;
        }
    }

    private boolean isAnimatedGif(Path file) {
        if (!"gif".equals(FileUtil.getExt(file.getFileName().toString()))) return false;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
            if (iis == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return reader.getNumImages(true) > 1;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private BufferedImage resize(BufferedImage image, int targetWidth, int targetHeight, boolean toJpeg) {
        BufferedImage result = new BufferedImage(
                targetWidth, targetHeight, toJpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        if (toJpeg) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return result;
    }
}
