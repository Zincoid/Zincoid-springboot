package com.zincoid.me.ai.model.openai;

import com.zincoid.me.exception.BusinessException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ai.openai")
public class OpenAiProperties {

    private String active;
    private List<Provider> providers;

    @Data
    public static class Provider {
        private String name;
        private String apiUrl;
        private String apiKey;
        private String model;
    }

    public Provider current() {
        Provider p = null;
        if (providers != null && active != null)
            p = providers.stream()
                    .filter(x -> active.equalsIgnoreCase(x.getName()))
                    .findFirst().orElse(null);
        if (p == null && providers != null && !providers.isEmpty()) p = providers.getFirst();
        if (p == null || p.getApiUrl() == null || p.getApiUrl().isBlank()
                || p.getApiKey() == null || p.getApiKey().isBlank()
                || p.getModel() == null || p.getModel().isBlank())
            throw new BusinessException("AI provider not configured (ai.openai.providers)");
        return p;
    }
}
