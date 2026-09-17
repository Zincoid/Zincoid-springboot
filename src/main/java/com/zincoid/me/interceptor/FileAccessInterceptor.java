package com.zincoid.me.interceptor;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.po.File;
import com.zincoid.me.service.FileService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class FileAccessInterceptor implements HandlerInterceptor {

    private static final String CACHE_PUBLIC = "public, max-age=31536000, immutable";
    private static final String CACHE_PRIVATE = "private, no-store";

    private final FileService fileService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String path = extractPath(request.getRequestURI());
        if (path == null)
            throw new BusinessException(404, "Path is invalid");

        Long userId = AuthCtx.getUserId();
        Role role = AuthCtx.isAuthed() ? AuthCtx.getRole() : null;
        File file = fileService.get(path);
        if (file == null)
            throw new BusinessException(404, "File not found");
        if (!fileService.accessible(file, userId, role))
            throw new BusinessException(403, "Permission denied");

        boolean cacheable = fileService.cacheable(file);
        response.setHeader("Cache-Control",
                cacheable ? CACHE_PUBLIC : CACHE_PRIVATE);
        return true;
    }

    private String extractPath(String uri) {
        if (uri.startsWith("/uploads/"))
            return uri.substring("/uploads/".length());
        if (uri.startsWith("/thumbnails/"))
            return uri.substring("/thumbnails/".length());
        return null;
    }
}
