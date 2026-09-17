package com.zincoid.me.controller;

import com.zincoid.me.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/thumbnails")
@RequiredArgsConstructor
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    // ──── Open endpoints ──────────────────

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        Resource resource = thumbnailService.getThumbnail(filename);
        if (resource == null || !resource.exists())
            return ResponseEntity.notFound().build();
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
