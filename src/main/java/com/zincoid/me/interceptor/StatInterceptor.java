package com.zincoid.me.interceptor;

import com.zincoid.me.service.StatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class StatInterceptor implements HandlerInterceptor {

    private final StatService statService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        statService.record(
                request.getMethod(),
                request.getRequestURI().replaceAll("/\\d+(?=/|$)", "/{param}")
        );
        return true;
    }
}
