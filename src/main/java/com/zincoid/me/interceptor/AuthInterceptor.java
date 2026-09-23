package com.zincoid.me.interceptor;

import com.zincoid.me.controller.AuthController;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.exception.UnauthorizedException;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.JwtTool;
import com.zincoid.me.utils.AuthCtx;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;
    private final UserService userService;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/health",
            "/api/auth",
            "/api/users/public",
            "/api/moments/public",
            "/api/articles/public",
            "/api/repos/public",
            "/api/chats/public",
            "/api/comments/public",
            "/api/likes/public",
            "/api/configs/public",
            "/uploads",
            "/thumbnails"
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // Check public paths
        String path = request.getRequestURI();
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(
                p -> path.equals(p) || path.startsWith(p + "/"));

        // Resolve token from header or cookie
        String token = resolveToken(request);
        boolean hasToken = token != null && !token.isBlank();
        User user = null;
        if (hasToken) {
            boolean valid = jwtTool.validate(token);
            boolean revoked = valid && userService.isTokenRevoked(token);
            if (!isPublic && !valid)
                throw new UnauthorizedException("Token is invalid");
            if (!isPublic && revoked)
                throw new UnauthorizedException("Token is revoked");
            if (valid && !revoked)
                user = userService.getById(jwtTool.getUserId(token));
        }

        // Allow public paths without auth
        if (isPublic) return true;
        // Other paths require auth
        if (user == null)
            throw new BusinessException(404, "Account not found");
        if (user.getStatus() == Status.DISABLED)
            throw new BusinessException(403, "Account is disabled");
        if (user.getRole() != AuthCtx.getRole())
            throw new BusinessException(403, "Token role stale");

        // Update user last active time
        userService.updateActiveAt(user.getId());
        // AuthCtx initialize
        AuthCtx.init(user.getId(), user.getRole());

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer "))
            return authHeader.substring(7);
        if (request.getCookies() != null)
            for (Cookie cookie : request.getCookies())
                if (AuthController.COOKIE_NAME.equals(cookie.getName()))
                    return cookie.getValue();
        return null;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        // Clear auth context
        AuthCtx.clear();
    }
}
