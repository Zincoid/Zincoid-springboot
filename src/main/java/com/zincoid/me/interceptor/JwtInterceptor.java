package com.zincoid.me.interceptor;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.exception.UnauthorizedException;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.JwtTool;
import com.zincoid.me.utils.AuthCtx;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtTool jwtUtils;
    private final UserService userService;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/health",
            "/api/auth",
            "/api/users/public",
            "/api/moments/public",
            "/api/articles/public",
            "/api/chats/public",
            "/api/comments/public",
            "/api/likes/public",
            "/api/configs/public"
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // Populate from token if present
        String authHeader = request.getHeader("Authorization");
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");

        if (hasToken) {
            String token = authHeader.substring(7);
            if (!jwtUtils.validate(token))
                throw new UnauthorizedException("Token is invalid");
            if (userService.isTokenRevoked(token))
                throw new UnauthorizedException("Token is revoked");
            AuthCtx.set(
                    jwtUtils.getUserId(token),
                    jwtUtils.getUsername(token),
                    jwtUtils.getRole(token)
            );
            User user = userService.getById(jwtUtils.getUserId(token));
            if (user != null && user.getStatus() == Status.DISABLED)
                throw new BusinessException(403, "Account is disabled");
        }

        // Allow public paths without auth
        String path = request.getRequestURI();
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) return true;

        // Other paths require auth
        if (!AuthCtx.isAuthed())
            throw new UnauthorizedException("Authentication required");

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        AuthCtx.clear();
    }
}
