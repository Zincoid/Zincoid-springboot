package com.zincoid.me.configuration;

import com.zincoid.me.interceptor.JwtInterceptor;
import com.zincoid.me.interceptor.MaintenanceInterceptor;
import com.zincoid.me.interceptor.StatInterceptor;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final MaintenanceInterceptor maintenanceInterceptor;
    private final StatInterceptor statInterceptor;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, RelatedType.class,
                s -> RelatedType.fromValue(Integer.parseInt(s)));
        registry.addConverter(String.class, Role.class,
                s -> Role.fromValue(Integer.parseInt(s)));
        registry.addConverter(String.class, Status.class,
                s -> Status.fromValue(Integer.parseInt(s)));
        registry.addConverter(String.class, Visibility.class,
                s -> Visibility.fromValue(Integer.parseInt(s)));
        registry.addConverter(String.class, RepoType.class,
                s -> RepoType.fromValue(Integer.parseInt(s)));
        registry.addConverter(String.class, Access.class,
                s -> Access.fromValue(Integer.parseInt(s)));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(statInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
