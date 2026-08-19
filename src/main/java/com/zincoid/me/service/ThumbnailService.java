package com.zincoid.me.service;

import org.springframework.core.io.Resource;

public interface ThumbnailService {

    /**
     * Convert an original file URL/path to its thumbnail URL.
     * Only local uploads ({@code /uploads/...}) are converted; other URLs pass through unchanged.
     */
    String toThumbUrl(String urlOrPath);

    /**
     * Lazy-generate and cache the thumbnail of an uploaded file.
     * Falls back to the original file when the format cannot be processed.
     */
    Resource getThumbnail(String filename);
}
