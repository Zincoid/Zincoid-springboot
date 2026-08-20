package com.zincoid.me.interceptor;

import com.zincoid.me.configuration.MaintenanceManager;
import com.zincoid.me.exception.MaintenanceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final MaintenanceManager maintenanceManager;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (maintenanceManager.isActive())
            throw new MaintenanceException();
        return true;
    }
}
